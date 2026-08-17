package com.finmind.categorias.dto;

import com.finmind.categorias.entity.Categoria;

/** `delSistema` le dice al frontend que no ofrezca editar ni borrar. */
public record CategoriaResponse(
        Long id, String nombre, String tipo, String icono,
        String colorHex, Boolean activa, boolean delSistema
) {
    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getTipo(), c.getIcono(),
                c.getColorHex(), c.getActiva(), c.esDelSistema());
    }
}
