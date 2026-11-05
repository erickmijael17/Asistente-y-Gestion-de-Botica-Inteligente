package com.botica.inteligente.usuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
class UsuarioServiceTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private SesionActual sesionActual;

    private Usuario owner;

    @DynamicPropertySource
    static void configureTemporarySqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMPORARY_DATABASE);
    }

    @BeforeEach
    void prepararOwner() {
        sesionActual.cerrarSesion();
        usuarioRepository.deleteAll();
        owner = guardarUsuario("owner", "ClaveOwner123", Rol.OWNER, true);
        sesionActual.autenticar(owner);
    }

    @Test
    void rechazaUsernameDuplicado() {
        NegocioException error = assertThrows(
            NegocioException.class,
            () -> usuarioService.crearUsuario("owner", "ClaveNueva123", Rol.SELLER)
        );

        assertEquals("El nombre de usuario ya está en uso.", error.getMessage());
    }

    @Test
    void sellerNoPuedeGestionarUsuariosYOwnerSiPuede() {
        Usuario seller = guardarUsuario("vendedor", "ClaveVendedor123", Rol.SELLER, true);
        sesionActual.autenticar(seller);

        assertThrows(NegocioException.class, () -> usuarioService.listarUsuarios());
        assertThrows(
            NegocioException.class,
            () -> usuarioService.crearUsuario("nuevo", "ClaveNueva123", Rol.SELLER)
        );
        assertThrows(NegocioException.class, () -> usuarioService.cambiarEstado(seller.getId(), false));

        sesionActual.autenticar(owner);
        assertTrue(usuarioService.listarUsuarios().stream().anyMatch(usuario -> usuario.username().equals("owner")));
        UsuarioResumen creado = usuarioService.crearUsuario("nuevo", "ClaveNueva123", Rol.SELLER);
        assertFalse(usuarioService.cambiarEstado(creado.id(), false).estado());
    }

    @Test
    void guardaPasswordHasheadoYPermiteVerificarlo() {
        String password = "ClaveNueva123";
        usuarioService.crearUsuario("nuevo", password, Rol.SELLER);

        Usuario guardado = usuarioRepository.findByUsername("nuevo").orElseThrow();

        assertNotEquals(password, guardado.getPassword());
        assertTrue(guardado.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches(password, guardado.getPassword()));
    }

    @Test
    void permiteCrearOwnerInicialSoloSiAunNoExisteOwner() {
        sesionActual.cerrarSesion();
        usuarioRepository.deleteAll();
        UsuarioResumen creado = usuarioService.crearOwnerInicial("primer-owner", "ClaveInicial123", "ClaveInicial123");

        assertEquals(Rol.OWNER, creado.rol());
        assertTrue(usuarioService.existeOwner());
        assertThrows(
            NegocioException.class,
            () -> usuarioService.crearOwnerInicial("otro-owner", "ClaveInicial123", "ClaveInicial123")
        );
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
            return Files.createTempDirectory("botica-usuarios-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
