package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.Maintenance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface IMaintenanceRepo extends IGenericRepo<Maintenance, UUID> {

    List<Maintenance> findByTechnician_Id(UUID technicianId, Sort sort);

    Page<Maintenance> findByTechnician_Id(UUID technicianId, Pageable pageable);

    Optional<Maintenance> findByIdAndTechnician_Id(UUID id, UUID technicianId);
}
