package com.botica.inteligente.usuario.service;

import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SesionActual sesionActual;

    public AuthService(
        UsuarioRepository usuarioRepository,
        BCryptPasswordEncoder passwordEncoder,
        SesionActual sesionActual
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.sesionActual = sesionActual;
    }

    @Transactional(readOnly = true)
    public SesionActual.UsuarioAutenticado autenticar(String username, String password) {
        sesionActual.cerrarSesion();
        String usernameNormalizado = username == null ? "" : username.trim();
        String passwordIngresado = password == null ? "" : password;

        Usuario usuario = usuarioRepository.findByUsername(usernameNormalizado)
            .orElseThrow(() -> new NegocioException(Mensajes.get("error.auth.credencialesInvalidas")));

        boolean passwordValido = passwordEncoder.matches(passwordIngresado, usuario.getPassword());
        if (!passwordValido || !Boolean.TRUE.equals(usuario.getEstado())) {
            throw new NegocioException(Mensajes.get("error.auth.credencialesInvalidas"));
        }

        sesionActual.autenticar(usuario);
        return sesionActual.usuarioAutenticado().orElseThrow();
    }
}
