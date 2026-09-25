package com.tidsec.novaeyetech_backend.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.model.enums.QuotationStatus;
import com.tidsec.novaeyetech_backend.model.enums.Role;
import com.tidsec.novaeyetech_backend.repo.IQuotationRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * La busqueda y el filtro de estado nunca amplian lo que ve un tecnico: el alcance por creador va
 * siempre en la consulta, con o sin filtros.
 */
@ExtendWith(MockitoExtension.class)
class QuotationSearchScopeTest {

    @Mock
    private IQuotationRepo repo;

    @InjectMocks
    private QuotationServiceImpl service;

    private final Pageable pageable = PageRequest.of(0, 10);

    @SuppressWarnings("unchecked")
    private final Root<Quotation> root = mock(Root.class, RETURNS_DEEP_STUBS);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @SuppressWarnings("unchecked")
    private Specification<Quotation> capturedSpec() {
        ArgumentCaptor<Specification<Quotation>> spec = ArgumentCaptor.forClass(Specification.class);
        verify(repo).findAll(spec.capture(), eq(pageable));
        return spec.getValue();
    }

    @Test
    @DisplayName("Tecnico que busca: la consulta sigue limitada a sus cotizaciones")
    void technicianSearchKeepsScope() {
        UUID technicianId = UUID.randomUUID();
        AuthenticatedUser technician = new AuthenticatedUser(technicianId, "t@test.com", Role.TECNICO);

        service.findAll(technician, "COT", QuotationStatus.BORRADOR, pageable);

        capturedSpec().toPredicate(root, query, cb);
        verify(cb).equal(root.get("createdByUser").get("id"), technicianId);
    }

    @Test
    @DisplayName("Administrador que busca: sin limite por creador")
    @SuppressWarnings("unchecked")
    void adminSearchIsNotScoped() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "a@test.com", Role.ADMINISTRADOR);

        service.findAll(admin, "COT", null, pageable);

        capturedSpec().toPredicate(root, query, cb);
        verify(cb, never()).equal(any(Expression.class), any(UUID.class));
    }

    @Test
    @DisplayName("Sin filtros usa la consulta de siempre")
    void withoutFiltersUsesPlainQuery() {
        UUID technicianId = UUID.randomUUID();
        AuthenticatedUser technician = new AuthenticatedUser(technicianId, "t@test.com", Role.TECNICO);
        when(repo.findByCreatedByUser_Id(technicianId, pageable)).thenReturn(Page.empty());

        service.findAll(technician, "  ", null, pageable);

        verify(repo).findByCreatedByUser_Id(technicianId, pageable);
    }
}
