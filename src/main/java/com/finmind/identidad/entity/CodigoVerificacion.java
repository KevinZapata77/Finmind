package com.finmind.identidad.entity;

import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Codigo de un solo uso para verificar el correo o recuperar la contrasena.
 *
 * Una sola entidad para los dos flujos: tienen la misma estructura y las
 * mismas reglas de vigencia (ADR-010). Los distingue el campo tipo.
 */
@Entity
@Table(name = "codigos_verificacion")
public class CodigoVerificacion {

    public static final String VERIFICACION = "VERIFICACION";
    public static final String RECUPERACION = "RECUPERACION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo", nullable = false, length = 6)
    private String codigo;

    @Column(name = "tipo", nullable = false, length = 12)
    private String tipo;

    @Column(name = "intentos", nullable = false)
    private Short intentos = 0;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado_en")
    private LocalDateTime usadoEn;

    protected CodigoVerificacion() {
        // Requerido por JPA.
    }

    public CodigoVerificacion(Usuario usuario, String codigo, String tipo, int vigenciaMinutos) {
        this.usuario = usuario;
        this.codigo = codigo;
        this.tipo = tipo;
        this.creadoEn = LocalDateTime.now();
        this.expiraEn = this.creadoEn.plusMinutes(vigenciaMinutos);
        this.intentos = 0;
    }

    /** RNF-013: un solo uso y vigencia limitada. Ambas condiciones deben cumplirse. */
    public boolean estaVigente() {
        return usadoEn == null && LocalDateTime.now().isBefore(expiraEn);
    }

    public boolean coincide(String otro) {
        return codigo.equals(otro);
    }

    public void registrarIntentoFallido() {
        this.intentos = (short) (this.intentos + 1);
    }

    /** DT-06: al superar el maximo, el codigo queda inutilizable aunque no haya expirado. */
    public boolean superoIntentos(int maximo) {
        return this.intentos >= maximo;
    }

    public void consumir() {
        this.usadoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getCodigo() { return codigo; }
    public String getTipo() { return tipo; }
    public Short getIntentos() { return intentos; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public LocalDateTime getUsadoEn() { return usadoEn; }
}
