package com.botica.inteligente.usuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class AuthServiceTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private SesionActual sesionActual;

    @DynamicPropertySource
    static void configureTemporarySqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMPORARY_DATABASE);
    }

    @BeforeEach
    void limpiarDatos() {
        sesionActual.cerrarSesion();
        usuarioRepository.deleteAll();
    }

    @Test
    void autenticaCredencialesCorrectasYGuardaLaSesion() {
        guardarUsuario("vendedor", "ClaveSegura123", Rol.SELLER, true);

        SesionActual.UsuarioAutenticado autenticado = authService.autenticar("vendedor", "ClaveSegura123");

        assertEquals("vendedor", autenticado.username());
        assertEquals(Rol.SELLER, autenticado.rol());
        assertTrue(sesionActual.tieneRol(Rol.SELLER));
    }

    @Test
    void rechazaCredencialesIncorrectasConMensajeGenerico() {
        guardarUsuario("vendedor", "ClaveSegura123", Rol.SELLER, true);

        NegocioException error = assertThrows(
            NegocioException.class,
            () -> authService.autenticar("vendedor", "incorrecta")
        );

        assertEquals("Usuario o contraseña incorrectos.", error.getMessage());
        assertFalse(sesionActual.usuarioAutenticado().isPresent());
    }

    @Test
    void rechazaUsuarioInactivoConElMismoMensajeGenerico() {
        guardarUsuario("vendedor", "ClaveSegura123", Rol.SELLER, false);

        NegocioException error = assertThrows(
            NegocioException.class,
            () -> authService.autenticar("vendedor", "ClaveSegura123")
        );

        assertEquals("Usuario o contraseña incorrectos.", error.getMessage());
        assertFalse(sesionActual.usuarioAutenticado().isPresent());
    }

    private Usuario guardarUsuario(String username, String password, Rol rol, boolean estado) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRoles(rol);
        usuario.setEstado(estado);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private static Path createTemporaryDatabasePath() {
        try {
            return Files.createTempDirectory("botica-auth-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
