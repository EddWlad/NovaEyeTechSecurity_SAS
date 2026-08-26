package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.LoginRequest;
import com.tidsec.novaeyetech_backend.dto.LoginResponse;

public interface IAuthService {

    LoginResponse login(LoginRequest request);
}
