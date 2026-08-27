package com.finmind.soporte;

import com.finmind.categorias.entity.Categoria;
import com.finmind.categorias.repository.CategoriaRepository;
import com.finmind.cuentas.repository.CuentaRepository;
import com.finmind.fijos.repository.GastoFijoRepository;
import com.finmind.administracion.repository.AuditoriaAdminRepository;
import com.finmind.metas.repository.MetaAhorroRepository;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.presupuestos.repository.PresupuestoRepository;
import com.finmind.obligaciones.repository.ObligacionRepository;
import com.finmind.obligaciones.repository.PagoObligacionRepository;
import com.finmind.common.security.LimitadorDeIntentos;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.repository.RolRepository;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Deja la base en blanco entre pruebas.
 *
 * POR QUE EXISTE ESTA CLASE
 * Antes cada prueba borraba su propia lista de tablas. Al aparecer "cuentas",
 * las pruebas de identidad siguieron borrando solo usuarios y codigos, y al
 * borrar un usuario que ya tenia cuentas la base respondio:
 *
 *   Referential integrity constraint violation: fk_cuentas_usuario
 *
 * No fue un fallo de identidad: fue que nadie le aviso. Con un solo punto de
 * limpieza, agregar un modulo es agregar UNA linea aqui, y ninguna prueba
 * ajena se entera.
 *
 * EL ORDEN IMPORTA: primero lo que apunta, despues lo apuntado. Los roles no
 * se borran: son datos de catalogo que la migracion V1 siembra una vez.
 */
@Component
public class LimpiadorDeDatos {

    private final TransaccionRepository movimientos;
    private final MetaAhorroRepository metas;
    private final AuditoriaAdminRepository auditoria;
    private final PresupuestoRepository presupuestos;
    private final CategoriaRepository categorias;
    private final CuentaRepository cuentas;
    private final ObligacionRepository obligaciones;
    private final PagoObligacionRepository pagos;
    private final GastoFijoRepository fijos;
    private final CodigoVerificacionRepository codigos;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final LimitadorDeIntentos limitador;

    public LimpiadorDeDatos(TransaccionRepository movimientos,
                            MetaAhorroRepository metas,
                            AuditoriaAdminRepository auditoria,
                            PresupuestoRepository presupuestos,
                            CategoriaRepository categorias,
                            CuentaRepository cuentas,
                            ObligacionRepository obligaciones,
                            PagoObligacionRepository pagos,
                            GastoFijoRepository fijos,
                            CodigoVerificacionRepository codigos,
                            UsuarioRepository usuarios,
                            RolRepository roles,
                            LimitadorDeIntentos limitador) {
        this.movimientos = movimientos;
        this.metas = metas;
        this.auditoria = auditoria;
        this.presupuestos = presupuestos;
        this.categorias = categorias;
        this.cuentas = cuentas;
        this.obligaciones = obligaciones;
        this.pagos = pagos;
        this.fijos = fijos;
        this.codigos = codigos;
        this.usuarios = usuarios;
        this.roles = roles;
        this.limitador = limitador;
    }

    @Transactional
    public void limpiar() {
        // El limitador de intentos guarda su cuenta en memoria, no en la base.
        // Sin reiniciarlo, una clase que inicia sesion mas de cinco veces
        // empezaria a recibir 429 y fallaria por el limite en lugar de por lo
        // que estaba probando. Es el mismo error de antes con otra cara: un
        // estado compartido que nadie reinicia.
        limitador.reiniciar();

        // --- hijos primero -------------------------------------------------
        // Los movimientos apuntan a cuentas y categorias: van de primeros.
        movimientos.deleteAllInBatch();
        metas.deleteAllInBatch();
        // La auditoria apunta al administrador que hizo la accion.
        auditoria.deleteAllInBatch();
        // Los presupuestos apuntan a categorias: tambien van antes que ellas.
        presupuestos.deleteAllInBatch();
        // Los gastos fijos apuntan a categorias: van antes que ellas.
        fijos.deleteAllInBatch();
        // Solo las de usuarios: las del sistema son catalogo, igual que los roles.
        categorias.borrarLasDeUsuarios();
        // Los pagos apuntan a obligaciones: van antes que ellas.
        pagos.deleteAllInBatch();
        obligaciones.deleteAllInBatch();
        cuentas.deleteAllInBatch();
        codigos.deleteAllInBatch();
        // AL AGREGAR UN MODULO NUEVO, su tabla va AQUI ARRIBA si apunta a usuarios
        // (transacciones, presupuestos, metas_ahorro...).

        // --- despues los padres --------------------------------------------
        usuarios.deleteAllInBatch();

        sembrarRoles();
        sembrarCategoriasDelSistema();
    }

    /**
     * Las categorias del sistema las siembra la migracion V1, pero en pruebas
     * Flyway esta apagado y el esquema lo genera Hibernate desde las entidades.
     * Sin esto, en pruebas no existiria ninguna categoria y no se podria
     * registrar un solo movimiento.
     *
     * La lista es la misma de V1__esquema_inicial.sql. Si alla cambia, aqui tambien.
     */
    private void sembrarCategoriasDelSistema() {
        if (!categorias.findAll().isEmpty()) return;
        List.of(
                new Categoria(null, "Salario",         Categoria.INGRESO, "wallet",   "#15803D"),
                new Categoria(null, "Otros ingresos",  Categoria.INGRESO, "plus",     "#0E8368"),
                new Categoria(null, "Alimentacion",    Categoria.GASTO,   "utensils", "#B45309"),
                new Categoria(null, "Transporte",      Categoria.GASTO,   "bus",      "#0B6B57"),
                new Categoria(null, "Vivienda",        Categoria.GASTO,   "home",     "#0A5647"),
                new Categoria(null, "Servicios",       Categoria.GASTO,   "bolt",     "#374151"),
                new Categoria(null, "Salud",           Categoria.GASTO,   "heart",    "#B91C1C"),
                new Categoria(null, "Educacion",       Categoria.GASTO,   "book",     "#6B7280"),
                new Categoria(null, "Entretenimiento", Categoria.GASTO,   "film",     "#0E8368"),
                new Categoria(null, "Otros gastos",    Categoria.GASTO,   "dots",     "#D1D5DB"),
                // Agregadas por la migracion V5
                new Categoria(null, "Ventas",              Categoria.INGRESO, "tag",       "#0E8368"),
                new Categoria(null, "Trabajo por horas",   Categoria.INGRESO, "clock",     "#0B6B57"),
                new Categoria(null, "Servicios prestados", Categoria.INGRESO, "briefcase", "#15803D"),
                new Categoria(null, "Arriendos",           Categoria.INGRESO, "building",  "#0A5647"),
                new Categoria(null, "Prestamo recibido",   Categoria.INGRESO, "handshake", "#374151"),
                new Categoria(null, "Deudas y cuotas",     Categoria.GASTO,   "receipt",   "#B91C1C"),
                new Categoria(null, "Ahorro",              Categoria.GASTO,   "piggy",     "#0E8368")
        ).forEach(categorias::save);
    }

    /** Los roles son catalogo: si no estan, ningun registro puede completarse. */
    private void sembrarRoles() {
        if (roles.findByNombre(Rol.USUARIO).isEmpty()) {
            roles.save(new Rol(Rol.USUARIO, "Usuario final"));
        }
        if (roles.findByNombre(Rol.ADMIN).isEmpty()) {
            roles.save(new Rol(Rol.ADMIN, "Administrador de plataforma"));
        }
    }
}
