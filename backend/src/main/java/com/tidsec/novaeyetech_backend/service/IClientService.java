package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.ClientRequest;
import com.tidsec.novaeyetech_backend.model.Client;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.UUID;

public interface IClientService extends ICRUD<Client, UUID> {

    Client create(ClientRequest request, AuthenticatedUser actor);

    Client update(UUID id, ClientRequest request, AuthenticatedUser actor);

    void delete(UUID id, AuthenticatedUser actor);
}
