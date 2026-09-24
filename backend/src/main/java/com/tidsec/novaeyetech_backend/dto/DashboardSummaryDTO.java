package com.tidsec.novaeyetech_backend.dto;

import java.util.List;
import lombok.Data;

/** Indicadores del dashboard. Los nombres coinciden con el estado que ya consume el frontend. */
@Data
public class DashboardSummaryDTO {

    private long clients;
    private long suppliers;
    private long products;
    private long services;
    private long pendingMaintenance;
    private List<QuotationDTO> quotations;
    private List<MaintenanceDTO> maintenance;
}
