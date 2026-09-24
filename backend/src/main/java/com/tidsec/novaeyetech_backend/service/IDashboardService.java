package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.List;

/**
 * Indicadores del dashboard ejecutivo.
 *
 * <p>Sustituye a seis listados completos que el frontend descargaba solo para contarlos. Los totales
 * son globales; las cotizaciones y los mantenimientos respetan el alcance por rol de siempre: un
 * TECNICO solo ve los suyos.
 */
public interface IDashboardService {

    DashboardSummary summary(AuthenticatedUser actor);

    /**
     * @param pendingMaintenance      total de mantenimientos que no estan COMPLETADO
     * @param recentQuotations        las cotizaciones mas recientes del alcance del usuario
     * @param pendingMaintenanceItems los mantenimientos pendientes mas recientes del alcance del usuario
     */
    record DashboardSummary(
            long clients,
            long suppliers,
            long products,
            long services,
            long pendingMaintenance,
            List<Quotation> recentQuotations,
            List<Maintenance> pendingMaintenanceItems
    ) {
    }
}
