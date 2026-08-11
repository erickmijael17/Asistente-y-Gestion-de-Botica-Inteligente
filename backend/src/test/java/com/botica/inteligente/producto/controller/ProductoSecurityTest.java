package com.botica.inteligente.producto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.botica.inteligente.config.CorsProperties;
import com.botica.inteligente.producto.dto.response.ProductoResponse;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.service.ProductoService;
import com.botica.inteligente.security.JwtAuthenticationFilter;
import com.botica.inteligente.security.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.AuthenticationProvider;
import com.botica.inteligente.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@EnableConfigurationProperties(CorsProperties.class)
class ProductoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductoService productoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void endpointWithoutTokenReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerCanQueryProducts() throws Exception {
        when(productoService.findAll(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/productos").with(user("test").roles("SELLER")))
                .andExpect(status().isOk());
    }

    @Test
    void sellerCannotCreateProducts() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                        .with(user("test").roles("SELLER"))
                        .contentType("application/json")
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCreateProducts() throws Exception {
        when(productoService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/productos")
                        .with(user("owner").roles("OWNER"))
                        .contentType("application/json")
                        .content(validBody()))
                .andExpect(status().isCreated());
    }

    private String validBody() {
        return """
                {
                  "codigoInterno": "P001",
                  "codigoBarras": "775000000001",
                  "nombreComercial": "Producto demo",
                  "tipoProducto": "VITAMINA",
                  "precioCompra": 1.00,
                  "precioVenta": 2.00,
                  "stockMinimo": 1,
                  "categoriaId": 1,
                  "laboratorioId": 1
                }
                """;
    }

    private ProductoResponse response() {
        return new ProductoResponse(1L, "P001", "775000000001", "Producto demo", null, null,
                TipoProducto.VITAMINA, null, null, null, null, null, null, null, null, null,
                false, null, BigDecimal.ONE, BigDecimal.TEN, 1, null, true,
                1L, "Vitaminas", 1L, "Demo", null, null, null, null);
    }
}
