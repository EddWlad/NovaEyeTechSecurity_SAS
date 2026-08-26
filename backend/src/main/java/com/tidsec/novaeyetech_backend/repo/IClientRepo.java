package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.Client;
import java.util.UUID;

public interface IClientRepo extends IGenericRepo<Client, UUID> {

    boolean existsByDocumentNumber(String documentNumber);
}
