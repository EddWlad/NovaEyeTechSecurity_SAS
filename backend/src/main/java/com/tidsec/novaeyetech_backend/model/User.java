package com.tidsec.novaeyetech_backend.model;

import com.tidsec.novaeyetech_backend.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "users")
public class User extends Auditable implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 180)
    private String fullName;

    /** Hash BCrypt. Nunca se expone en un DTO de salida. */
    @ToString.Exclude
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "users_role_enum")
    private Role role;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "text")
    private String avatarDataUrl;

    @Column(nullable = false)
    private boolean active;
}
