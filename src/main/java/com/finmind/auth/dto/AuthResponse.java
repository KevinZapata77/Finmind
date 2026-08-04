package com.finmind.auth.dto;

public record AuthResponse(
        String token,
        String tipo,
        long expiraEnMs,
        UsuarioResponse usuario
) {
    public static AuthResponse de(String token, long expiraEnMs, UsuarioResponse usuario) {
        return new AuthResponse(token, "Bearer", expiraEnMs, usuario);
    }
}
