package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.SupplierRequest;
import com.tidsec.novaeyetech_backend.model.Supplier;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.UUID;

public interface ISupplierService extends ICRUD<Supplier, UUID> {

    Supplier create(SupplierRequest request, AuthenticatedUser actor);

    Supplier update(UUID id, SupplierRequest request, AuthenticatedUser actor);

    void delete(UUID id, AuthenticatedUser actor);
}
