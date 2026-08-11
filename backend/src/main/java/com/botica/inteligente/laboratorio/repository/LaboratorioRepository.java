package com.botica.inteligente.laboratorio.repository;

import com.botica.inteligente.laboratorio.entity.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LaboratorioRepository extends JpaRepository<Laboratorio, Long>, JpaSpecificationExecutor<Laboratorio> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Long id);
}
