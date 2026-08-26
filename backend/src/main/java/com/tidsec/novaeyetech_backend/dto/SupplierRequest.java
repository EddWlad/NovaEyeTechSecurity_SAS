package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRequest {

    @NotBlank(groups = OnCreate.class)
    @Size(max = 180)
    private String businessName;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 30)
    private String ruc;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 180)
    private String contact;

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

    private Boolean active;
}
