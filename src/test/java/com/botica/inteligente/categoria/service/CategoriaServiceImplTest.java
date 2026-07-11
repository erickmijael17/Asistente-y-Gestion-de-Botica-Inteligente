package com.botica.inteligente.categoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.botica.inteligente.categoria.dto.request.CategoriaCreateRequest;
import com.botica.inteligente.categoria.dto.request.CategoriaFilter;
import com.botica.inteligente.categoria.dto.request.CategoriaUpdateRequest;
import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.mapper.CategoriaMapper;
import com.botica.inteligente.categoria.mapper.CategoriaMapperImpl;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.shared.exception.ResourceNotFoundException;
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
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository repository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        CategoriaMapper mapper = new CategoriaMapperImpl();
        service = new CategoriaServiceImpl(repository, mapper);
    }

    @Test
    void createCategoria() {
        when(repository.existsByNombreIgnoreCase("Vitaminas")).thenReturn(false);
        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria categoria = invocation.getArgument(0);
            categoria.setId(1L);
            return categoria;
        });

        var response = service.create(new CategoriaCreateRequest("Vitaminas", "General"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Vitaminas");
    }

    @Test
    void duplicateNameThrowsConflict() {
        when(repository.existsByNombreIgnoreCase("Vitaminas")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoriaCreateRequest("Vitaminas", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void findUpdateChangeStatusAndList() {
        Categoria categoria = categoria(1L, "Analgesicos", true);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.existsByNombreIgnoreCaseAndIdNot("Analgesicos Plus", 1L)).thenReturn(false);
        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of(categoria)));

        assertThat(service.findById(1L).nombre()).isEqualTo("Analgesicos");
        assertThat(service.update(1L, new CategoriaUpdateRequest("Analgesicos Plus", "Nueva")).nombre()).isEqualTo("Analgesicos Plus");
        assertThat(service.changeStatus(1L, false).estado()).isFalse();
        assertThat(service.findAll(new CategoriaFilter(null, null), PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void findByIdNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository).findById(99L);
    }

    private Categoria categoria(Long id, String nombre, boolean estado) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        categoria.setEstado(estado);
        return categoria;
    }
}
