package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(groups = OnCreate.class)
    @Size(max = 120)
    private String name;

    private String description;

    private Boolean active;
}
