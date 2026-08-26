package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.Supplier;
import java.util.UUID;

public interface ISupplierRepo extends IGenericRepo<Supplier, UUID> {

    boolean existsByRuc(String ruc);
}
