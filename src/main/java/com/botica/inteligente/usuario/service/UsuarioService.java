package com.botica.inteligente.usuario.service;

import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
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
            throw new NegocioException(Mensajes.get("error.usuario.rolRequerido"));
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
            throw new NegocioException(Mensajes.get("error.usuario.passwordsNoCoinciden"));
        }
        if (usuarioRepository.existsByRoles(Rol.OWNER)) {
            throw new NegocioException(Mensajes.get("error.usuario.setupYaCompletado"));
        }
        validarUsernameUnico(usernameNormalizado);

        Usuario owner = nuevoUsuario(usernameNormalizado, password, Rol.OWNER);
        return resumen(usuarioRepository.save(owner));
    }

    @Transactional
    public UsuarioResumen cambiarEstado(Long id, boolean estado) {
        validarPermisoOwner();
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new NegocioException(Mensajes.get("error.usuario.noEncontrado")));

        if (!estado && Boolean.TRUE.equals(usuario.getEstado()) && usuario.getRoles() == Rol.OWNER
            && usuarioRepository.countByRolesAndEstado(Rol.OWNER, true) <= 1) {
            throw new NegocioException(Mensajes.get("error.usuario.ultimoOwner"));
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
            throw new NegocioException(Mensajes.get("error.usuario.nombreRequerido"));
        }
        if (usernameNormalizado.length() > 100) {
            throw new NegocioException(Mensajes.get("error.usuario.nombreDemasiadoLargo"));
        }
        return usernameNormalizado;
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new NegocioException(Mensajes.get("error.usuario.passwordCorta"));
        }
    }

    private void validarUsernameUnico(String username) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new NegocioException(Mensajes.get("error.usuario.nombreEnUso"));
        }
    }

    private void validarPermisoOwner() {
        if (!sesionActual.esOwner()) {
            throw new NegocioException(Mensajes.get("error.usuario.sinPermiso"));
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
