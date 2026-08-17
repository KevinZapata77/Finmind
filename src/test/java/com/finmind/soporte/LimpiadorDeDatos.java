package com.finmind.soporte;

import com.finmind.cuentas.repository.CuentaRepository;
import com.finmind.obligaciones.repository.ObligacionRepository;
import com.finmind.obligaciones.repository.PagoObligacionRepository;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.repository.RolRepository;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final CuentaRepository cuentas;
    private final ObligacionRepository obligaciones;
    private final PagoObligacionRepository pagos;
    private final CodigoVerificacionRepository codigos;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;

    public LimpiadorDeDatos(CuentaRepository cuentas,
                            ObligacionRepository obligaciones,
                            PagoObligacionRepository pagos,
                            CodigoVerificacionRepository codigos,
                            UsuarioRepository usuarios,
                            RolRepository roles) {
        this.cuentas = cuentas;
        this.obligaciones = obligaciones;
        this.pagos = pagos;
        this.codigos = codigos;
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Transactional
    public void limpiar() {
        // --- hijos primero -------------------------------------------------
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
