package com.botica.inteligente.security;

import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SesionActual {

    private volatile UsuarioAutenticado usuario;

    public void autenticar(Usuario usuario) {
        String nombre = nombreVisible(usuario);
        this.usuario = new UsuarioAutenticado(
            usuario.getId(),
            usuario.getUsername(),
            usuario.getRoles(),
            nombre
        );
    }

    public Optional<UsuarioAutenticado> usuarioAutenticado() {
        return Optional.ofNullable(usuario);
    }

    public boolean esOwner() {
        return tieneRol(Rol.OWNER);
    }

    public boolean tieneRol(Rol rol) {
        UsuarioAutenticado actual = usuario;
        return actual != null && actual.rol() == rol;
    }

    /** Exige que haya un usuario autenticado (OWNER o SELLER). */
    public void exigirAutenticado() {
        if (usuario == null) {
            throw new NegocioException("Debe iniciar sesión para continuar.");
        }
    }

    /** Exige que el usuario autenticado tenga el rol OWNER. */
    public void exigirOwner(String mensajeSinPermiso) {
        if (!esOwner()) {
            throw new NegocioException(mensajeSinPermiso);
        }
    }

    public void cerrarSesion() {
        usuario = null;
    }

    private String nombreVisible(Usuario usuario) {
        String nombres = usuario.getNombres() == null ? "" : usuario.getNombres().trim();
        String apellidos = usuario.getApellidos() == null ? "" : usuario.getApellidos().trim();
        String nombre = (nombres + " " + apellidos).trim();
        return nombre.isBlank() ? usuario.getUsername() : nombre;
    }

    public record UsuarioAutenticado(Long id, String username, Rol rol, String nombreVisible) {
    }
}
