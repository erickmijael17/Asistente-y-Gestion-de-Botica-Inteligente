package com.botica.inteligente.usuario.service;

import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final int LONGITUD_MINIMA_PASSWORD = 8;
    private static final String MENSAJE_SIN_PERMISO = "No tiene permisos para gestionar usuarios.";

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SesionActual sesionActual;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        BCryptPasswordEncoder passwordEncoder,
        SesionActual sesionActual
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.sesionActual = sesionActual;
    }

    @Transactional(readOnly = true)
    public boolean existeOwner() {
        return usuarioRepository.existsByRoles(Rol.OWNER);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumen> listarUsuarios() {
        validarPermisoOwner();
        return usuarioRepository.findAllByOrderByUsernameAsc().stream()
            .map(this::resumen)
            .toList();
    }

    @Transactional
    public UsuarioResumen crearUsuario(String username, String password, Rol rol) {
        validarPermisoOwner();
        String usernameNormalizado = validarUsername(username);
        validarPassword(password);
        if (rol == null) {
            throw new NegocioException("Seleccione un rol para el usuario.");
        }
        validarUsernameUnico(usernameNormalizado);

        Usuario usuario = nuevoUsuario(usernameNormalizado, password, rol);
        return resumen(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResumen crearOwnerInicial(String username, String password, String confirmacion) {
        String usernameNormalizado = validarUsername(username);
        validarPassword(password);
        if (!password.equals(confirmacion)) {
            throw new NegocioException("Las contraseñas no coinciden.");
        }
        if (usuarioRepository.existsByRoles(Rol.OWNER)) {
            throw new NegocioException("La configuración inicial ya fue completada.");
        }
        validarUsernameUnico(usernameNormalizado);

        Usuario owner = nuevoUsuario(usernameNormalizado, password, Rol.OWNER);
        return resumen(usuarioRepository.save(owner));
    }

    @Transactional
    public UsuarioResumen cambiarEstado(Long id, boolean estado) {
        validarPermisoOwner();
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new NegocioException("No se encontró el usuario seleccionado."));

        if (!estado && Boolean.TRUE.equals(usuario.getEstado()) && usuario.getRoles() == Rol.OWNER
            && usuarioRepository.countByRolesAndEstado(Rol.OWNER, true) <= 1) {
            throw new NegocioException("Debe mantenerse al menos un OWNER activo.");
        }

        usuario.setEstado(estado);
        return resumen(usuarioRepository.save(usuario));
    }

    private Usuario nuevoUsuario(String username, String password, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRoles(rol);
        usuario.setEstado(true);
        return usuario;
    }

    private String validarUsername(String username) {
        String usernameNormalizado = username == null ? "" : username.trim();
        if (usernameNormalizado.isBlank()) {
            throw new NegocioException("Ingrese un nombre de usuario.");
        }
        if (usernameNormalizado.length() > 100) {
            throw new NegocioException("El nombre de usuario no puede superar 100 caracteres.");
        }
        return usernameNormalizado;
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new NegocioException("La contraseña debe tener al menos 8 caracteres.");
        }
    }

    private void validarUsernameUnico(String username) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new NegocioException("El nombre de usuario ya está en uso.");
        }
    }

    private void validarPermisoOwner() {
        if (!sesionActual.esOwner()) {
            throw new NegocioException(MENSAJE_SIN_PERMISO);
        }
    }

    private UsuarioResumen resumen(Usuario usuario) {
        return new UsuarioResumen(
            usuario.getId(),
            usuario.getUsername(),
            usuario.getRoles(),
            Boolean.TRUE.equals(usuario.getEstado())
        );
    }
}
