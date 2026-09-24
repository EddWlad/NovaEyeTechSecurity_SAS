package com.tidsec.novaeyetech_backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.model.enums.Role;
import com.tidsec.novaeyetech_backend.repo.IClientRepo;
import com.tidsec.novaeyetech_backend.repo.IProductRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceItemRepo;
import com.tidsec.novaeyetech_backend.repo.ISupplierRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IDashboardService.DashboardSummary;
import com.tidsec.novaeyetech_backend.service.IMaintenanceService;
import com.tidsec.novaeyetech_backend.service.IQuotationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * El dashboard no reimplementa el alcance por rol: lo delega en los servicios que ya lo resuelven,
 * pasando siempre el usuario autenticado. Si dejara de hacerlo, un tecnico veria datos ajenos.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private IClientRepo clientRepo;
    @Mock
    private ISupplierRepo supplierRepo;
    @Mock
    private IProductRepo productRepo;
    @Mock
    private IServiceItemRepo serviceRepo;
    @Mock
    private IQuotationService quotationService;
    @Mock
    private IMaintenanceService maintenanceService;

    @InjectMocks
    private DashboardServiceImpl service;

    @Test
    @DisplayName("Totales globales, y cotizaciones y mantenimientos delegados con el usuario autenticado")
    void summaryDelegatesRoleScopingToServices() {
        AuthenticatedUser technician = new AuthenticatedUser(UUID.randomUUID(), "t@test.com", Role.TECNICO);
        Quotation quotation = Quotation.builder().quotationNumber("COT-2026-000001").build();
        Maintenance maintenance = Maintenance.builder().intervenedSystem("CCTV").build();

        when(clientRepo.count()).thenReturn(10L);
        when(supplierRepo.count()).thenReturn(2L);
        when(productRepo.count()).thenReturn(684L);
        when(serviceRepo.count()).thenReturn(37L);
        when(quotationService.findAll(eq(technician), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(quotation)));
        // El total real (7) supera lo que cabe en la pagina (1): el indicador no debe toparse en 5.
        Page<Maintenance> pending = new PageImpl<>(List.of(maintenance), PageRequest.of(0, 1), 7);
        when(maintenanceService.findPending(eq(technician), any(Pageable.class))).thenReturn(pending);

        DashboardSummary summary = service.summary(technician);

        assertThat(summary.clients()).isEqualTo(10);
        assertThat(summary.suppliers()).isEqualTo(2);
        assertThat(summary.products()).isEqualTo(684);
        assertThat(summary.services()).isEqualTo(37);
        assertThat(summary.pendingMaintenance()).isEqualTo(7);
        assertThat(summary.recentQuotations()).containsExactly(quotation);
        assertThat(summary.pendingMaintenanceItems()).containsExactly(maintenance);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(quotationService).findAll(eq(technician), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }
}
