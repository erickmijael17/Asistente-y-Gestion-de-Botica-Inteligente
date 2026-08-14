package com.botica.inteligente.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.botica.inteligente.security.JwtService;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(repository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void registerAlwaysAssignsSellerRole() {
        when(repository.findByUsername("vendedor")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encrypted");
        when(jwtService.generateToken(any())).thenReturn("token");
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("vendedor");
        request.setPassword("123456");
        request.setNombres("Juan");
        request.setApellidos("Perez");

        AuthResponse response = service.register(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRoles()).isEqualTo("ROLE_SELLER");
        assertThat(captor.getValue().getPassword()).isEqualTo("encrypted");
        assertThat(response.getRoles()).isEqualTo("ROLE_SELLER");
        assertThat(response.getToken()).isEqualTo("token");
    }

    @Test
    void registerThrowsConflictWhenUsernameAlreadyExists() {
        when(repository.findByUsername("duplicado")).thenReturn(Optional.of(new Usuario()));

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("duplicado");
        request.setPassword("123456");

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void loginReturnsTokenAndPersistedRoles() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("gerente");
        usuario.setPassword("encrypted");
        usuario.setRoles("ROLE_OWNER");

        when(repository.findByUsername("gerente")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any())).thenReturn("token");

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("gerente");
        request.setPassword("clave");

        AuthResponse response = service.login(request);

        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getRoles()).isEqualTo("ROLE_OWNER");
    }
}