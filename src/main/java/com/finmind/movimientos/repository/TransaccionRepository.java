package com.finmind.movimientos.repository;

import com.finmind.movimientos.entity.Transaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    /** RN-005. */
    Optional<Transaccion> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Listado con filtros opcionales (RF-014). Los nulos se ignoran, asi que una
     * sola consulta cubre todas las combinaciones sin construir SQL a mano.
     *
     * DEF-18. El filtro por cuenta mira TAMBIEN la cuenta de destino.
     *
     * Una transferencia se guarda en una sola fila, y esa fila pertenece a la
     * cuenta de origen. Con el filtro anterior, pedir los movimientos de una
     * tarjeta no devolvia sus abonos: el pago existia, bajaba la deuda y se veia
     * en el saldo, pero no aparecia en ninguna lista. El usuario habria
     * concluido que no se guardo.
     *
     * "Los movimientos de esta cuenta" incluye lo que salio y lo que entro.
     * Filtrar solo por origen respondia a como esta guardado el dato, no a lo
     * que la pregunta significa.
     */
    @Query("""
           SELECT t FROM Transaccion t
           WHERE t.usuario.id = :usuarioId
             AND (:desde       IS NULL OR t.fecha >= :desde)
             AND (:hasta       IS NULL OR t.fecha <= :hasta)
             AND (:cuentaId    IS NULL OR t.cuenta.id = :cuentaId
                                       OR t.cuentaDestino.id = :cuentaId)
             AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId)
             AND (:tipo        IS NULL OR t.tipo = :tipo)
           ORDER BY t.fecha DESC, t.id DESC
           """)
    Page<Transaccion> buscar(@Param("usuarioId") Long usuarioId,
                             @Param("desde") LocalDate desde,
                             @Param("hasta") LocalDate hasta,
                             @Param("cuentaId") Long cuentaId,
                             @Param("categoriaId") Long categoriaId,
                             @Param("tipo") String tipo,
                             Pageable pagina);

    /**
     * DT-09 resuelta: movimiento neto de una cuenta.
     *
     * El ELSE cubre TRANSFERENCIA y resta, porque una fila de transferencia
     * pertenece a la cuenta de ORIGEN: desde aqui el dinero sale. Lo que la
     * cuenta de destino recibe se suma aparte, en recibidoPorTransferencia.
     *
     * El motivo va en este javadoc y no dentro de la consulta: JPQL no admite
     * comentarios con --, y ponerlo ahi rompia la creacion del repositorio y con
     * ella el arranque de todo el contexto de Spring.
     * Se suma en la base porque traer miles de movimientos para sumarlos en
     * memoria funciona con datos de prueba y se cae con un usuario real.
     */
    @Query("""
           SELECT COALESCE(SUM(CASE
                    WHEN t.tipo = 'INGRESO' THEN t.monto
                    WHEN t.tipo = 'GASTO'   THEN -t.monto
                    ELSE -t.monto
                  END), 0)
           FROM Transaccion t WHERE t.cuenta.id = :cuentaId
           """)
    BigDecimal salidasYEntradasPropias(@Param("cuentaId") Long cuentaId);

    /**
     * RN-022. Lo que ESTA cuenta recibio por transferencias de otras.
     *
     * Va aparte porque una transferencia se guarda en una sola fila, y esa fila
     * pertenece a la cuenta de origen. Para la cuenta de destino la unica forma
     * de verla es buscando por cuenta_destino_id.
     */
    @Query("""
           SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t
           WHERE t.cuentaDestino.id = :cuentaId AND t.tipo = 'TRANSFERENCIA'
           """)
    BigDecimal recibidoPorTransferencia(@Param("cuentaId") Long cuentaId);

    /**
     * RF-044. Cuanto se ha abonado a una cuenta, sumando solo los ingresos.
     *
     * Suma las transferencias que llegaron a esta cuenta. En una tarjeta de
     * credito eso es exactamente lo que le has pagado.
     *
     * Antes se sumaban los INGRESO registrados sobre la tarjeta, que era como
     * se abonaba a falta de transferencias. Eso inflaba los ingresos del mes y
     * no descontaba el dinero de la cuenta de origen (DEF-16).
     */
    @Query("""
           SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t
           WHERE t.cuentaDestino.id = :cuentaId AND t.tipo = 'TRANSFERENCIA'
           """)
    BigDecimal totalAbonadoALaCuenta(@Param("cuentaId") Long cuentaId);

    /** Para el balance del periodo (RF-021). */
    @Query("""
           SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t
           WHERE t.usuario.id = :usuarioId AND t.tipo = :tipo
             AND t.fecha BETWEEN :desde AND :hasta
           """)
    BigDecimal totalPorTipo(@Param("usuarioId") Long usuarioId, @Param("tipo") String tipo,
                            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * RN-009: consumo de un presupuesto. Solo GASTO, solo esa categoria, solo ese mes.
     * Los ingresos no consumen presupuesto: un reintegro no "libera" gasto.
     */
    @Query("""
           SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t
           WHERE t.usuario.id = :usuarioId AND t.categoria.id = :categoriaId
             AND t.tipo = 'GASTO' AND t.fecha BETWEEN :desde AND :hasta
           """)
    BigDecimal consumoDeCategoria(@Param("usuarioId") Long usuarioId,
                                  @Param("categoriaId") Long categoriaId,
                                  @Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta);

    /**
     * RF-022: composicion del gasto por categoria.
     * Devuelve filas [categoriaId, nombre, color, total] ya agrupadas y ordenadas
     * por la base. Traer los movimientos y agrupar en Java daria el mismo numero
     * y mucho mas trabajo al servidor.
     */
    @Query("""
           SELECT t.categoria.id, t.categoria.nombre, t.categoria.colorHex, SUM(t.monto)
           FROM Transaccion t
           WHERE t.usuario.id = :usuarioId AND t.tipo = :tipo
             AND t.fecha BETWEEN :desde AND :hasta
           GROUP BY t.categoria.id, t.categoria.nombre, t.categoria.colorHex
           ORDER BY SUM(t.monto) DESC
           """)
    List<Object[]> agruparPorCategoria(@Param("usuarioId") Long usuarioId,
                                       @Param("tipo") String tipo,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /**
     * RF-048. Suma por dia y por tipo dentro de un rango.
     *
     * Devuelve solo los dias CON movimientos. Rellenar los huecos es tarea del
     * servicio: hacerlo en SQL exigiria generar una serie de fechas, que en
     * H2 y en PostgreSQL se escribe distinto.
     *
     * Columnas: [0] fecha, [1] tipo, [2] suma.
     */
    @Query("""
           SELECT t.fecha, t.tipo, SUM(t.monto) FROM Transaccion t
           WHERE t.usuario.id = :usuarioId
             AND t.tipo IN ('INGRESO', 'GASTO')
             AND t.fecha BETWEEN :desde AND :hasta
           GROUP BY t.fecha, t.tipo
           ORDER BY t.fecha
           """)
    List<Object[]> agruparPorDia(@Param("usuarioId") Long usuarioId,
                                @Param("desde") LocalDate desde,
                                @Param("hasta") LocalDate hasta);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCuentaId(Long cuentaId);
}
