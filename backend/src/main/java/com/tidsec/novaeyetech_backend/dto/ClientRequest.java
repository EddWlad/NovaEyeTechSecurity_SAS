package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientRequest {

    @NotBlank(groups = OnCreate.class)
    @Size(max = 180)
    private String nameOrBusinessName;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 30)
    private String documentNumber;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 30)
    private String phone;

    @Email
    @Size(max = 180)
    private String email;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 255)
    private String address;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 120)
    private String city;

    @Size(max = 255)
    private String commercialReference;

    private String observations;

    private Boolean active;
}
