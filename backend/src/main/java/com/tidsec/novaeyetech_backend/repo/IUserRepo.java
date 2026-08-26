package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.User;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepo extends IGenericRepo<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
