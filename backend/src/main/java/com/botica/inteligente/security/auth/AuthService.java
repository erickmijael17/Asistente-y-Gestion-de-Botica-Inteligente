package com.botica.inteligente.security.auth;

import com.botica.inteligente.security.CustomUserDetails;
import com.botica.inteligente.security.JwtService;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(AuthRegisterRequest request) {
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists");
        }
        
        Usuario user = new Usuario();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles("ROLE_SELLER");
        user.setNombres(request.getNombres());
        user.setApellidos(request.getApellidos());
        
        repository.save(user);
        
        CustomUserDetails userDetails = new CustomUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles())
                .build();
    }

    public AuthResponse login(AuthLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        
        var user = repository.findByUsername(request.getUsername())
                .orElseThrow();
                
        CustomUserDetails userDetails = new CustomUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles())
                .build();
    }
}
