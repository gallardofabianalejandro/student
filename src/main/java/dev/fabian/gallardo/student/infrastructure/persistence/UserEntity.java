package dev.fabian.gallardo.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.nacionservicios.emv.persistence.audit.BaseAuditableEntity;

@Entity
@Table(name = "app_user")
public class UserEntity extends BaseAuditableEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Roles separados por coma (USER,ADMIN). */
    @Column(nullable = false, length = 100)
    private String roles;

    protected UserEntity() {
    }

    public UserEntity(String id, String username, String passwordHash, String roles) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRoles() {
        return roles;
    }
}
