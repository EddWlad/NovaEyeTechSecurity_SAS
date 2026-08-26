package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.ServiceRequest;
import com.tidsec.novaeyetech_backend.model.ServiceItem;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.UUID;

public interface IServiceItemService extends ICRUD<ServiceItem, UUID> {

    ServiceItem create(ServiceRequest request, AuthenticatedUser actor);

    ServiceItem update(UUID id, ServiceRequest request, AuthenticatedUser actor);

    void delete(UUID id, AuthenticatedUser actor);
}
