package com.botica.inteligente.producto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.producto.dto.request.ProductoCreateRequest;
import com.botica.inteligente.producto.dto.request.ProductoFilter;
import com.botica.inteligente.producto.dto.request.ProductoUpdateRequest;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.mapper.ProductoMapperImpl;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private LaboratorioRepository laboratorioRepository;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoServiceImpl(productoRepository, categoriaRepository, laboratorioRepository, new ProductoMapperImpl());
    }

    @Test
    void createProducto() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorio()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            producto.setId(1L);
            return producto;
        });

        var response = service.create(createRequest("P001", "775"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.categoriaNombre()).isEqualTo("Vitaminas");
        assertThat(response.laboratorioNombre()).isEqualTo("Demo");
    }

    @Test
    void duplicateCodesThrowConflict() {
        when(productoRepository.existsByCodigoInternoIgnoreCase("P001")).thenReturn(true);
        assertThatThrownBy(() -> service.create(createRequest("P001", null))).isInstanceOf(ConflictException.class);

        when(productoRepository.existsByCodigoInternoIgnoreCase("P002")).thenReturn(false);
        when(productoRepository.existsByCodigoBarras("775")).thenReturn(true);
        assertThatThrownBy(() -> service.create(createRequest("P002", "775"))).isInstanceOf(ConflictException.class);
    }

    @Test
    void missingCategoriaOrLaboratorioThrowsNotFound() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());
        ProductoCreateRequest missingCategoria = new ProductoCreateRequest("P003", null, "Producto", null, null, TipoProducto.OTRO, null, null, null, null, null, null, null, null, null, false, null, BigDecimal.ONE, BigDecimal.TEN, 0, null, 99L, 1L, null, null);
        assertThatThrownBy(() -> service.create(missingCategoria)).isInstanceOf(ResourceNotFoundException.class);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));
        when(laboratorioRepository.findById(99L)).thenReturn(Optional.empty());
        ProductoCreateRequest missingLaboratorio = new ProductoCreateRequest("P004", null, "Producto", null, null, TipoProducto.OTRO, null, null, null, null, null, null, null, null, null, false, null, BigDecimal.ONE, BigDecimal.TEN, 0, null, 1L, 99L, null, null);
        assertThatThrownBy(() -> service.create(missingLaboratorio)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findUpdateChangeStatusFindBarcodeAndList() {
        Producto producto = producto();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.findByCodigoBarras("775")).thenReturn(Optional.of(producto));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorio()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(producto)));

        assertThat(service.findById(1L).codigoInterno()).isEqualTo("P001");
        assertThat(service.findByCodigoBarras("775").codigoBarras()).isEqualTo("775");
        assertThat(service.update(1L, updateRequest()).nombreComercial()).isEqualTo("Producto 2");
        assertThat(service.changeStatus(1L, false).estado()).isFalse();
        assertThat(service.findAll(new ProductoFilter(null, null, "Producto", null, null, null, 1L, 1L, null, null), PageRequest.of(0, 10))).hasSize(1);
    }

    private ProductoCreateRequest createRequest(String codigoInterno, String codigoBarras) {
        return new ProductoCreateRequest(codigoInterno, codigoBarras, "Producto", null, null, TipoProducto.VITAMINA, null, null, null, null, null, null, null, null, null, false, null, BigDecimal.ONE, BigDecimal.TEN, 1, null, 1L, 1L, null, null);
    }

    private ProductoUpdateRequest updateRequest() {
        return new ProductoUpdateRequest("P001", "775", "Producto 2", null, null, TipoProducto.VITAMINA, null, null, null, null, null, null, null, null, null, false, null, BigDecimal.ONE, BigDecimal.TEN, 1, null, 1L, 1L, null, null);
    }

    private Producto producto() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setCodigoInterno("P001");
        producto.setCodigoBarras("775");
        producto.setNombreComercial("Producto");
        producto.setTipoProducto(TipoProducto.VITAMINA);
        producto.setPrecioCompra(BigDecimal.ONE);
        producto.setPrecioVenta(BigDecimal.TEN);
        producto.setStockMinimo(1);
        producto.setEstado(true);
        producto.setRequiereReceta(false);
        producto.setCategoria(categoria());
        producto.setLaboratorio(laboratorio());
        return producto;
    }

    private Categoria categoria() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Vitaminas");
        categoria.setEstado(true);
        return categoria;
    }

    private Laboratorio laboratorio() {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setId(1L);
        laboratorio.setNombre("Demo");
        laboratorio.setEstado(true);
        return laboratorio;
    }
}
