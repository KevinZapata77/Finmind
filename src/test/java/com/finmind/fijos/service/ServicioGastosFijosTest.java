package com.finmind.fijos.service;

import com.finmind.categorias.entity.Categoria;
import com.finmind.categorias.service.ServicioCategorias;
import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.fijos.dto.GastoFijoRequest;
import com.finmind.fijos.dto.GastoFijoResponse;
import com.finmind.fijos.entity.GastoFijo;
import com.finmind.fijos.repository.GastoFijoRepository;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PRUEBA UNITARIA DEL SERVICIO CON MOCKITO.
 *
 * QUE ES UN DOBLE Y POR QUE SE USA AQUI
 * ServicioGastosFijos depende de cuatro colaboradores: dos repositorios, el
 * servicio de categorias y el repositorio de movimientos. Para probar la LOGICA
 * del servicio no hace falta que esos cuatro sean de verdad; hace falta que
 * respondan lo que la prueba necesite. Mockito construye un reemplazo falso de
 * cada uno (un "mock"), la prueba le dice que contestar, y despues comprueba con
 * que argumentos lo llamo el servicio.
 *
 * EN QUE SE DIFERENCIA DE LAS OTRAS 189 PRUEBAS
 * Las otras son de integracion: levantan Spring, entran por HTTP y escriben en
 * H2. Prueban que todo junto funciona. Estas prueban una sola clase aislada:
 * no hay contexto, no hay base de datos, no hay red. Corren en milisegundos y
 * cuando fallan señalan exactamente una clase.
 *
 * Las dos hacen falta. Una prueba de integracion no puede demostrar que el
 * servicio NO llamo a save(), y eso es justo lo que hay que demostrar cuando se
 * rechaza un dato: que no llego a la base. Mockito lo verifica directamente.
 *
 * @ExtendWith(MockitoExtension.class) crea los mocks antes de cada prueba y, de
 * paso, falla si una prueba prepara una respuesta que despues no usa. Eso evita
 * que queden montajes muertos que aparentan cubrir algo que ya nadie ejecuta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioGastosFijos: reglas de negocio aisladas")
class ServicioGastosFijosTest {

    private static final Long USUARIO = 1L;
    private static final Long OTRO_USUARIO = 2L;
    private static final Long CAT_VIVIENDA = 7L;
    private static final Long CAT_MERCADO = 8L;
    private static final YearMonth MARZO = YearMonth.of(2026, 3);

    @Mock private GastoFijoRepository fijos;
    @Mock private UsuarioRepository usuarios;
    @Mock private ServicioCategorias categorias;
    @Mock private TransaccionRepository movimientos;

    @InjectMocks private ServicioGastosFijos servicio;

    private Usuario dueno;
    private Categoria vivienda;
    private Categoria mercado;

    @BeforeEach
    void prepararDatos() {
        dueno = new Usuario("Kevin", "Zapata", "kevin@finmind.test", "hash", null);
        ReflectionTestUtils.setField(dueno, "id", USUARIO);

        vivienda = categoriaDeGasto("Vivienda", CAT_VIVIENDA);
        mercado = categoriaDeGasto("Mercado", CAT_MERCADO);
    }

    private Categoria categoriaDeGasto(String nombre, Long id) {
        Categoria c = new Categoria(dueno, nombre, Categoria.GASTO, "home", "#2DD4BF");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private GastoFijo compromiso(Categoria categoria, String nombre,
                                 String monto, String periodicidad, int diaPago) {
        return new GastoFijo(dueno, categoria, nombre,
                new BigDecimal(monto), periodicidad, (short) diaPago);
    }

    /** El montaje minimo para que crear() llegue hasta el final. */
    private void dejarPasarLaCreacion() {
        when(fijos.existsByUsuarioIdAndNombreIgnoreCase(anyLong(), any())).thenReturn(false);
        when(categorias.exigirUsable(USUARIO, CAT_VIVIENDA)).thenReturn(vivienda);
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(dueno));
        // save() en la base devuelve la entidad guardada; el doble devuelve la
        // misma que recibio, que es lo unico que el servicio necesita de el.
        when(fijos.save(any(GastoFijo.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientos.consumoDeCategoria(anyLong(), anyLong(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    // ================================================================ RF-046: alta

    @Test
    @DisplayName("Guarda el compromiso con el nombre recortado y la periodicidad en mayusculas")
    void guardaElCompromisoNormalizado() {
        dejarPasarLaCreacion();

        GastoFijoResponse r = servicio.crear(USUARIO,
                new GastoFijoRequest("  Arriendo  ", CAT_VIVIENDA,
                        new BigDecimal("1100000"), "mensual", (short) 2));

        ArgumentCaptor<GastoFijo> guardado = ArgumentCaptor.forClass(GastoFijo.class);
        verify(fijos).save(guardado.capture());

        assertThat(guardado.getValue().getNombre()).isEqualTo("Arriendo");
        assertThat(guardado.getValue().getPeriodicidad()).isEqualTo("MENSUAL");
        assertThat(guardado.getValue().getDiaPago()).isEqualTo((short) 2);
        assertThat(guardado.getValue().getMonto()).isEqualByComparingTo("1100000");
        assertThat(guardado.getValue().estaActivo()).isTrue();

        assertThat(r.nombre()).isEqualTo("Arriendo");
        assertThat(r.montoMensual()).isEqualByComparingTo("1100000.00");
        assertThat(r.cubiertoEsteMes()).isFalse();
    }

    /**
     * RN-005. El dueño del compromiso sale del usuarioId del token, que el
     * controlador pasa como primer argumento. El cuerpo de la peticion no tiene
     * campo de usuario y no puede tenerlo: si lo tuviera, cualquiera podria
     * crear gastos fijos a nombre de otra persona cambiando un numero en el JSON.
     */
    @Test
    @DisplayName("RN-005: el compromiso se crea a nombre del usuario del token, no del cuerpo")
    void elDuenoSaleDelToken() {
        dejarPasarLaCreacion();

        servicio.crear(USUARIO, new GastoFijoRequest("Arriendo", CAT_VIVIENDA,
                new BigDecimal("1100000"), "MENSUAL", (short) 2));

        verify(usuarios).findById(USUARIO);
        verify(categorias).exigirUsable(USUARIO, CAT_VIVIENDA);

        ArgumentCaptor<GastoFijo> guardado = ArgumentCaptor.forClass(GastoFijo.class);
        verify(fijos).save(guardado.capture());
        assertThat(guardado.getValue().perteneceA(USUARIO)).isTrue();
    }

    @Test
    @DisplayName("Un nombre repetido no llega a guardarse")
    void elNombreRepetidoNoLlegaALaBase() {
        when(fijos.existsByUsuarioIdAndNombreIgnoreCase(USUARIO, "Arriendo")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(USUARIO,
                new GastoFijoRequest("Arriendo", CAT_VIVIENDA,
                        new BigDecimal("1100000"), "MENSUAL", (short) 2)))
                .isInstanceOf(ServicioGastosFijos.GastoFijoRepetidoException.class)
                .hasMessageContaining("Ya tienes un gasto fijo con ese nombre");

        verify(fijos, never()).save(any(GastoFijo.class));
    }

    /**
     * Un compromiso recurrente de tipo ingreso no significa nada: un sueldo que
     * entra cada mes no es algo para lo que haya que reservar dinero. Ademas
     * romperia la alerta, que suma compromisos como dinero que va a salir.
     */
    @Test
    @DisplayName("Una categoria de ingreso no puede sostener un gasto fijo")
    void laCategoriaTieneQueSerDeGasto() {
        Categoria sueldo = new Categoria(dueno, "Sueldo", Categoria.INGRESO, "wallet", "#34D399");
        ReflectionTestUtils.setField(sueldo, "id", 9L);

        when(fijos.existsByUsuarioIdAndNombreIgnoreCase(anyLong(), any())).thenReturn(false);
        when(categorias.exigirUsable(USUARIO, 9L)).thenReturn(sueldo);

        assertThatThrownBy(() -> servicio.crear(USUARIO,
                new GastoFijoRequest("Sueldo", 9L,
                        new BigDecimal("2000000"), "MENSUAL", (short) 2)))
                .isInstanceOf(ServicioGastosFijos.CategoriaDeGastoRequeridaException.class);

        verify(fijos, never()).save(any(GastoFijo.class));
    }

    @Test
    @DisplayName("Una periodicidad desconocida se rechaza antes de guardar")
    void laPeriodicidadDesconocidaSeRechaza() {
        when(fijos.existsByUsuarioIdAndNombreIgnoreCase(anyLong(), any())).thenReturn(false);
        when(categorias.exigirUsable(USUARIO, CAT_VIVIENDA)).thenReturn(vivienda);
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(dueno));

        assertThatThrownBy(() -> servicio.crear(USUARIO,
                new GastoFijoRequest("Netflix", CAT_VIVIENDA,
                        new BigDecimal("25000"), "ANUAL", (short) 2)))
                .isInstanceOf(ServicioGastosFijos.PeriodicidadInvalidaException.class);

        verify(fijos, never()).save(any(GastoFijo.class));
    }

    /**
     * DEF-19. La anotacion del DTO solo puede exigir un rango fijo (1 a 28), asi
     * que un 20 pasa la validacion de forma. El rango real depende de la
     * periodicidad y esa comprobacion vive en el servicio, que ya sabe cual es.
     * En un semanal el dia es el dia de la SEMANA: 1 lunes, 7 domingo.
     */
    @Test
    @DisplayName("DEF-19: en un compromiso semanal el dia va de 1 a 7")
    void elSemanalNoAceptaElDia20() {
        when(fijos.existsByUsuarioIdAndNombreIgnoreCase(anyLong(), any())).thenReturn(false);
        when(categorias.exigirUsable(USUARIO, CAT_MERCADO)).thenReturn(mercado);
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(dueno));

        assertThatThrownBy(() -> servicio.crear(USUARIO,
                new GastoFijoRequest("Mercado", CAT_MERCADO,
                        new BigDecimal("50000"), "SEMANAL", (short) 20)))
                .isInstanceOf(ServicioGastosFijos.DiaDePagoInvalidoException.class)
                .hasMessageContaining("1 (lunes) a 7 (domingo)");

        verify(fijos, never()).save(any(GastoFijo.class));
    }

    /**
     * DEF-21. El segundo pago cae quince dias despues del primero. Con 13 el
     * segundo es el 28, que existe en todos los meses; con 14 seria el 29 y en
     * febrero no existe.
     */
    @Test
    @DisplayName("DEF-21: el primer pago quincenal no pasa del 13")
    void elQuincenalNoAceptaElDia14() {
        when(fijos.existsByUsuarioIdAndNombreIgnoreCase(anyLong(), any())).thenReturn(false);
        when(categorias.exigirUsable(USUARIO, CAT_VIVIENDA)).thenReturn(vivienda);
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(dueno));

        assertThatThrownBy(() -> servicio.crear(USUARIO,
                new GastoFijoRequest("Servicios", CAT_VIVIENDA,
                        new BigDecimal("300000"), "QUINCENAL", (short) 14)))
                .isInstanceOf(ServicioGastosFijos.DiaDePagoInvalidoException.class);

        verify(fijos, never()).save(any(GastoFijo.class));
    }

    /** El limite es inclusivo: el 13 si entra. Un limite sin su borde probado no esta probado. */
    @Test
    @DisplayName("DEF-21: el dia 13 si se acepta en un quincenal")
    void elQuincenalAceptaElDia13() {
        dejarPasarLaCreacion();

        GastoFijoResponse r = servicio.crear(USUARIO,
                new GastoFijoRequest("Servicios", CAT_VIVIENDA,
                        new BigDecimal("300000"), "QUINCENAL",
                        (short) GastoFijo.MAXIMO_DIA_QUINCENAL));

        verify(fijos).save(any(GastoFijo.class));
        assertThat(r.montoMensual()).isEqualByComparingTo("600000.00");
    }

    // ============================================== RN-026: lo que falta por pagar

    /**
     * RN-026. La alerta "no te alcanza" se sostiene en este numero. Si sumara
     * tambien lo que ya se pago, le diria al usuario que le falta dinero que en
     * realidad ya gasto, y el aviso perderia sentido.
     *
     * Los dobles responden con valores exactos y no con any(): eso obliga a que
     * el servicio consulte el gasto de la categoria correcta y con la ventana
     * del mes completo, del dia 1 al ultimo. Si preguntara por otro rango, el
     * doble no reconoceria la llamada.
     */
    @Test
    @DisplayName("RN-026: solo suma los compromisos que todavia no se han pagado")
    void soloSumaLoQueFaltaPorPagar() {
        when(fijos.findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(USUARIO)).thenReturn(List.of(
                compromiso(vivienda, "Arriendo", "1100000", "MENSUAL", 2),
                compromiso(mercado, "Mercado", "50000", "SEMANAL", 5)));

        // Vivienda: nada gastado todavia -> el arriendo sigue pendiente.
        when(movimientos.consumoDeCategoria(
                USUARIO, CAT_VIVIENDA, MARZO.atDay(1), MARZO.atEndOfMonth()))
                .thenReturn(BigDecimal.ZERO);

        // Mercado: 300.000 gastados, por encima de los 217.250 mensuales -> cubierto.
        when(movimientos.consumoDeCategoria(
                USUARIO, CAT_MERCADO, MARZO.atDay(1), MARZO.atEndOfMonth()))
                .thenReturn(new BigDecimal("300000"));

        assertThat(servicio.pendienteDelMes(USUARIO, MARZO))
                .isEqualByComparingTo("1100000.00");
    }

    /**
     * RN-025 vista desde el servicio: lo que se suma es el equivalente mensual,
     * no el monto escrito. 50.000 semanales pendientes son 217.250 de compromiso
     * en el mes. Sumar 50.000 subestimaria el gasto por cuatro.
     */
    @Test
    @DisplayName("RN-025: lo pendiente se suma en equivalente mensual, no en monto escrito")
    void sumaElEquivalenteMensualYNoElMonto() {
        when(fijos.findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(USUARIO)).thenReturn(
                List.of(compromiso(mercado, "Mercado", "50000", "SEMANAL", 5)));
        when(movimientos.consumoDeCategoria(
                USUARIO, CAT_MERCADO, MARZO.atDay(1), MARZO.atEndOfMonth()))
                .thenReturn(BigDecimal.ZERO);

        assertThat(servicio.pendienteDelMes(USUARIO, MARZO))
                .isEqualByComparingTo("217250.00");
    }

    @Test
    @DisplayName("RN-026: si ya se pago todo, no queda nada pendiente")
    void siTodoEstaPagadoNoQuedaNada() {
        when(fijos.findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(USUARIO)).thenReturn(
                List.of(compromiso(vivienda, "Arriendo", "1100000", "MENSUAL", 2)));
        when(movimientos.consumoDeCategoria(
                USUARIO, CAT_VIVIENDA, MARZO.atDay(1), MARZO.atEndOfMonth()))
                .thenReturn(new BigDecimal("1100000"));

        assertThat(servicio.pendienteDelMes(USUARIO, MARZO))
                .isEqualByComparingTo("0");
    }

    /**
     * Los desactivados no entran. El servicio pide la lista de activos y nunca
     * la lista completa: se verifica que no haya llamado al metodo que trae
     * todo, porque incluir un compromiso que ya no aplica inflaria la alerta.
     */
    @Test
    @DisplayName("RN-026: los compromisos desactivados no cuentan")
    void losDesactivadosNoCuentan() {
        when(fijos.findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(USUARIO)).thenReturn(List.of());

        assertThat(servicio.pendienteDelMes(USUARIO, MARZO)).isEqualByComparingTo("0");

        verify(fijos, never()).findByUsuarioIdOrderByDiaPagoAsc(anyLong());
        verify(movimientos, never()).consumoDeCategoria(anyLong(), anyLong(), any(), any());
    }

    // ====================================================== RN-005: datos ajenos

    /**
     * RN-005. La consulta lleva el usuarioId dentro, no despues. Buscar por id y
     * comprobar el dueño luego funciona igual, pero deja abierta la puerta a que
     * alguien olvide la segunda mitad; asi no hay segunda mitad que olvidar.
     *
     * Devuelve 404 y no 403 a proposito: un 403 confirmaria que ese id existe.
     */
    @Test
    @DisplayName("RN-005: no se puede editar el gasto fijo de otro usuario")
    void noSePuedeEditarLoAjeno() {
        when(fijos.findByIdAndUsuarioId(99L, OTRO_USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(OTRO_USUARIO, 99L,
                new GastoFijoRequest("Arriendo", CAT_VIVIENDA,
                        new BigDecimal("1100000"), "MENSUAL", (short) 2)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(fijos).findByIdAndUsuarioId(99L, OTRO_USUARIO);
        verify(fijos, never()).save(any(GastoFijo.class));
    }

    /**
     * Un compromiso que ya no aplica sigue explicando las alertas de los meses en
     * que si aplicaba. Por eso se apaga y no se borra: borrarlo reescribiria el
     * pasado.
     */
    @Test
    @DisplayName("Desactivar apaga el compromiso pero no lo borra")
    void desactivarNoBorra() {
        GastoFijo arriendo = compromiso(vivienda, "Arriendo", "1100000", "MENSUAL", 2);
        when(fijos.findByIdAndUsuarioId(5L, USUARIO)).thenReturn(Optional.of(arriendo));
        when(movimientos.consumoDeCategoria(anyLong(), anyLong(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        GastoFijoResponse r = servicio.desactivar(USUARIO, 5L);

        assertThat(r.activo()).isFalse();
        assertThat(arriendo.estaActivo()).isFalse();
        verify(fijos, never()).delete(any(GastoFijo.class));
        verify(fijos, never()).deleteById(anyLong());
    }
}
