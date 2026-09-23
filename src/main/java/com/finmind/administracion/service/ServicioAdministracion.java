package com.finmind.administracion.service;

import com.finmind.administracion.dto.*;

import java.time.LocalDateTime;
import com.finmind.administracion.entity.AuditoriaAdmin;
import com.finmind.administracion.repository.AuditoriaAdminRepository;
import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion de usuarios por parte del administrador (RF-023, RF-024).
 *
 * LIMITE DEL PODER DE ADMINISTRAR:
 * este servicio solo toca la columna 'activo' de usuarios y escribe auditoria.
 * No lee cuentas, movimientos, presupuestos, obligaciones ni metas de nadie.
 * RN-005 no tiene excepcion para el administrador: administra el acceso a la
 * plataforma, no el dinero de las personas.
 */
@Service
public class ServicioAdministracion {

    private final UsuarioRepository usuarios;
    private final AuditoriaAdminRepository auditoria;

    public ServicioAdministracion(UsuarioRepository usuarios, AuditoriaAdminRepository auditoria) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listarUsuarios() {
        return usuarios.findAllByOrderByFechaCreacionDesc().stream()
                .map(UsuarioAdminResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ResumenAdminResponse resumen() {
        /*
          El "ahora" se calcula una sola vez y se reparte.

          Si cada conteo llamara a LocalDateTime.now() por su cuenta, los ocho
          numeros quedarian medidos en instantes distintos. Con milisegundos de
          diferencia da igual casi siempre, pero no cuando alguien se registra
          justo en el medio: ahi el resumen se contradice a si mismo y nadie
          entiende por que. Una sola foto del reloj, ocho numeros coherentes.
        */
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime hace7 = ahora.minusDays(7);
        LocalDateTime hace30 = ahora.minusDays(30);

        return new ResumenAdminResponse(
                usuarios.count(),
                usuarios.countByActivoTrueAndCorreoVerificadoTrue(),
                usuarios.countByCorreoVerificadoFalse(),
                usuarios.countByActivoFalse(),
                usuarios.countByFechaCreacionAfter(hace7),
                usuarios.countByFechaCreacionAfter(hace30),
                usuarios.countByUltimoAccesoAfter(hace7),
                usuarios.countByUltimoAccesoIsNull());
    }

    @Transactional
    public UsuarioAdminResponse desactivar(Long adminId, Long usuarioId) {
        if (adminId.equals(usuarioId)) {
            // Si el ultimo administrador se desactiva a si mismo, nadie puede reactivarlo:
            // la plataforma se queda sin quien administre.
            throw new AdminNoSePuedeDesactivarException(
                    "Un administrador no puede desactivar su propia cuenta");
        }
        Usuario objetivo = exigirUsuario(usuarioId);
        objetivo.setActivo(Boolean.FALSE);
        registrar(adminId, AuditoriaAdmin.DESACTIVAR_USUARIO, objetivo);
        return UsuarioAdminResponse.de(objetivo);
    }

    @Transactional
    public UsuarioAdminResponse activar(Long adminId, Long usuarioId) {
        Usuario objetivo = exigirUsuario(usuarioId);
        objetivo.setActivo(Boolean.TRUE);
        registrar(adminId, AuditoriaAdmin.ACTIVAR_USUARIO, objetivo);
        return UsuarioAdminResponse.de(objetivo);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> historial(int pagina, int tamano) {
        return auditoria.findAllByOrderByFechaDescIdDesc(
                        PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100)))
                .getContent().stream().map(AuditoriaResponse::de).toList();
    }

    /** Toda accion queda registrada. Es la contrapartida de tener el poder. */
    private void registrar(Long adminId, String accion, Usuario objetivo) {
        Usuario admin = usuarios.findById(adminId)
                .orElseThrow(() -> new IllegalStateException("El token es valido pero el admin ya no existe"));
        auditoria.save(new AuditoriaAdmin(admin, accion, "usuarios", objetivo.getId(),
                "Cuenta de " + objetivo.getCorreo()));
    }

    private Usuario exigirUsuario(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    }

    /** 409 */
    public static class AdminNoSePuedeDesactivarException extends RuntimeException {
        public AdminNoSePuedeDesactivarException(String m) { super(m); }
    }
}
