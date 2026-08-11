package com.botica.inteligente.usuario.entity;

import com.botica.inteligente.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuario_referencia")
public class UsuarioReferencia extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true, length = 100)
    private String keycloakUserId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 150)
    private String nombres;

    @Column(length = 150)
    private String apellidos;

    @Column(nullable = false)
    private Boolean estado = true;
}
