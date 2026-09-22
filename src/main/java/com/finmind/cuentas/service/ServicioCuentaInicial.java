package com.finmind.cuentas.service;

import com.finmind.cuentas.entity.Cuenta;
import com.finmind.cuentas.repository.CuentaRepository;
import com.finmind.usuarios.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * La cuenta de efectivo con la que arranca todo usuario nuevo.
 *
 * POR QUE EXISTE LA CUENTA
 * Un movimiento exige una cuenta por llave foranea. Sin esto, el primer gesto
 * de alguien recien registrado no seria anotar su plata sino llenar un
 * formulario de cuentas que no pidio. El tramite iria antes del valor, y eso
 * es lo que hace que una aplicacion se abandone a los tres dias.
 *
 * POR QUE ESTA EN SU PROPIA CLASE Y NO SUELTA EN EL REGISTRO (DEF-030)
 * Porque ya se desincronizo una vez. Estaba escrita dentro del registro con
 * correo y contrasena, y cuando se agrego el acceso con Google nadie se acordo
 * de repetirla alli. Resultado: quien entraba con Google llegaba a una
 * aplicacion sin ninguna cuenta, y al intentar anotar su primer movimiento se
 * encontraba con un desplegable vacio y sin explicacion.
 *
 * Ese defecto no se arregla agregando la linea en el segundo lugar: se arregla
 * dejando UN solo lugar. Si manana aparece un tercer camino de registro
 * —Microsoft, Apple, lo que sea—, quien lo escriba va a encontrar este servicio
 * y no va a tener que adivinar que hacia falta.
 */
@Service
public class ServicioCuentaInicial {

    private static final Logger log = LoggerFactory.getLogger(ServicioCuentaInicial.class);

    private final CuentaRepository cuentas;

    public ServicioCuentaInicial(CuentaRepository cuentas) {
        this.cuentas = cuentas;
    }

    /**
     * Crea la cuenta de efectivo del usuario, si todavia no tiene ninguna.
     *
     * La comprobacion no es adorno: este metodo tiene que poder llamarse dos
     * veces sin dejar dos cuentas "Efectivo" repetidas. Pasa de verdad cuando
     * una cuenta local sin verificar la toma Google (RN-034), porque el registro
     * original ya habia creado la suya.
     */
    public void crearSiNoTiene(Usuario usuario) {
        if (cuentas.existsByUsuarioId(usuario.getId())) {
            return;
        }
        cuentas.save(new Cuenta(usuario, "Efectivo", Cuenta.EFECTIVO, BigDecimal.ZERO, "COP"));
        log.info("Cuenta de efectivo creada para {}", usuario.getCorreo());
    }
}
