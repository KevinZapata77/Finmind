package com.finmind.reportes.dto;

import com.finmind.obligaciones.dto.PatrimonioResponse;
import com.finmind.presupuestos.dto.PresupuestoResponse;

import java.util.List;

/**
 * Todo lo que necesita la pantalla del panel, en una sola llamada.
 *
 * Podrian ser cuatro peticiones separadas, pero el panel es la primera pantalla
 * que ve el usuario al entrar: cuatro viajes al servidor se notan, y ademas
 * podrian llegar desincronizados entre si.
 */
public record PanelResponse(
        BalanceResponse balance,
        ComposicionResponse gastoPorCategoria,
        PatrimonioResponse patrimonio,
        List<PresupuestoResponse> presupuestosEnAlerta
) {
}
