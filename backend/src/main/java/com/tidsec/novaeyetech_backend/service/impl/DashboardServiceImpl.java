package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.repo.IClientRepo;
import com.tidsec.novaeyetech_backend.repo.IProductRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceItemRepo;
import com.tidsec.novaeyetech_backend.repo.ISupplierRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IDashboardService;
import com.tidsec.novaeyetech_backend.service.IMaintenanceService;
import com.tidsec.novaeyetech_backend.service.IQuotationService;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El alcance por rol no se reimplementa aqui: se delega en los servicios de cotizaciones y
 * mantenimientos, que ya lo resuelven, para que exista una sola regla.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final int RECENT_LIMIT = 5;

    private final IClientRepo clientRepo;
    private final ISupplierRepo supplierRepo;
    private final IProductRepo productRepo;
    private final IServiceItemRepo serviceRepo;
    private final IQuotationService quotationService;
    private final IMaintenanceService maintenanceService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary summary(AuthenticatedUser actor) {
        Pageable recent = PageRequest.of(0, RECENT_LIMIT, PaginationSupport.newestFirst());
        Page<Maintenance> pending = maintenanceService.findPending(actor, recent);

        return new DashboardSummary(
                clientRepo.count(),
                supplierRepo.count(),
                productRepo.count(),
                serviceRepo.count(),
                pending.getTotalElements(),
                quotationService.findAll(actor, recent).getContent(),
                pending.getContent());
    }
}
