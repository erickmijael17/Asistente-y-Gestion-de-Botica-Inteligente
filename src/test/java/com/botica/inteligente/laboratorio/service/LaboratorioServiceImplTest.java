package com.botica.inteligente.laboratorio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.botica.inteligente.laboratorio.dto.request.LaboratorioCreateRequest;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioUpdateRequest;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.mapper.LaboratorioMapperImpl;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.shared.exception.ConflictException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaboratorioServiceImplTest {

    @Mock
    private LaboratorioRepository repository;

    private LaboratorioService service;

    @BeforeEach
    void setUp() {
        service = new LaboratorioServiceImpl(repository, new LaboratorioMapperImpl());
    }

    @Test
    void createLaboratorio() {
        when(repository.save(any(Laboratorio.class))).thenAnswer(invocation -> {
            Laboratorio laboratorio = invocation.getArgument(0);
            laboratorio.setId(1L);
            return laboratorio;
        });

        var response = service.create(new LaboratorioCreateRequest("Demo", "123", null, "demo@example.com", null, null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.ruc()).isEqualTo("123");
    }

    @Test
    void duplicateNameAndRucThrowConflict() {
        when(repository.existsByNombreIgnoreCase("Demo")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new LaboratorioCreateRequest("Demo", null, null, null, null, null)))
                .isInstanceOf(ConflictException.class);

        when(repository.existsByNombreIgnoreCase("Otro")).thenReturn(false);
        when(repository.existsByRuc("123")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new LaboratorioCreateRequest("Otro", "123", null, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateAndChangeStatus() {
        Laboratorio laboratorio = laboratorio();
        when(repository.findById(1L)).thenReturn(Optional.of(laboratorio));
        when(repository.save(any(Laboratorio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.update(1L, new LaboratorioUpdateRequest("Demo 2", "456", null, null, null, null)).nombre()).isEqualTo("Demo 2");
        assertThat(service.changeStatus(1L, false).estado()).isFalse();
    }

    private Laboratorio laboratorio() {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setId(1L);
        laboratorio.setNombre("Demo");
        laboratorio.setRuc("123");
        laboratorio.setEstado(true);
        return laboratorio;
    }
}
