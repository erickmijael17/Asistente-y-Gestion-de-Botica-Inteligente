package com.botica.inteligente.usuario.repository;

import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRoles(Rol roles);

    long countByRolesAndEstado(Rol roles, Boolean estado);

    List<Usuario> findAllByOrderByUsernameAsc();
}
