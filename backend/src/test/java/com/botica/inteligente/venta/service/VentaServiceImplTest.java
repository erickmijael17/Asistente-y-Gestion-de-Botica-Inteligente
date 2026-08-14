package com.botica.inteligente.venta.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.security.CurrentUserService;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import com.botica.inteligente.venta.dto.request.VentaCreateRequest;
import com.botica.inteligente.venta.dto.request.VentaDetalleRequest;
import com.botica.inteligente.venta.dto.response.VentaResponse;
import com.botica.inteligente.venta.entity.Venta;
import com.botica.inteligente.venta.enums.EstadoVenta;
import com.botica.inteligente.venta.mapper.VentaMapper;
import com.botica.inteligente.venta.repository.VentaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VentaMapper ventaMapper;
    @Mock
    private CurrentUserService currentUserService;

    private VentaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VentaServiceImpl(ventaRepository, productoRepository, usuarioRepository, ventaMapper, currentUserService);
    }

    @Test
    void createUsesAuthenticatedUserAndRealPrice() {
        Usuario vendedor = new Usuario();
        vendedor.setId(3L);
        vendedor.setUsername("vendedor");
        vendedor.setRoles("ROLE_SELLER");

        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombreComercial("Paracetamol");
        producto.setPrecioVenta(new BigDecimal("25.50"));
        producto.setEstado(true);

        when(currentUserService.username()).thenReturn(Optional.of("vendedor"));
        when(usuarioRepository.findByUsername("vendedor")).thenReturn(Optional.of(vendedor));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ventaMapper.toResponse(any(Venta.class))).thenReturn(
                new VentaResponse(null, 3L, "vendedor", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, EstadoVenta.COMPLETADA, List.of()));

        VentaCreateRequest request = new VentaCreateRequest(List.of(new VentaDetalleRequest(1L, 2)));
        service.create(request);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());

        Venta venta = captor.getValue();
        assertThat(venta.getUsuario()).isSameAs(vendedor);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.COMPLETADA);
        assertThat(venta.getDetalles()).hasSize(1);
        assertThat(venta.getDetalles().get(0).getPrecioUnitario()).isEqualByComparingTo("25.50");
        assertThat(venta.getDetalles().get(0).getCantidad()).isEqualTo(2);
        assertThat(venta.getSubtotal()).isEqualByComparingTo("51.00");
        assertThat(venta.getTotal()).isEqualByComparingTo("51.00");
    }

    @Test
    void createRejectsInactiveProduct() {
        Usuario vendedor = new Usuario();
        vendedor.setUsername("vendedor");

        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombreComercial("Vencido");
        producto.setPrecioVenta(BigDecimal.TEN);
        producto.setEstado(false);

        when(currentUserService.username()).thenReturn(Optional.of("vendedor"));
        when(usuarioRepository.findByUsername("vendedor")).thenReturn(Optional.of(vendedor));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        VentaCreateRequest request = new VentaCreateRequest(List.of(new VentaDetalleRequest(1L, 1)));
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void anularAlreadyAnnulledThrowsConflict() {
        Venta venta = new Venta();
        venta.setId(9L);
        venta.setEstado(EstadoVenta.ANULADA);

        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> service.anular(9L)).isInstanceOf(ConflictException.class);
        verify(ventaRepository, never()).save(any(Venta.class));
    }
}