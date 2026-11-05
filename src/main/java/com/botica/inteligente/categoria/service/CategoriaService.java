package com.botica.inteligente.categoria.service;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.categoria.specification.CategoriaSpecification;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.util.Textos;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaService.class);
    private static final String SIN_PERMISO = "No tiene permisos para modificar categorías.";
    private static final int LIMITE_AUTOCOMPLETAR = 10;

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final SesionActual sesionActual;

    public CategoriaService(
        CategoriaRepository categoriaRepository,
        ProductoRepository productoRepository,
        SesionActual sesionActual
    ) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.sesionActual = sesionActual;
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResumen> listar(String texto, Boolean estado, Pageable pageable) {
        sesionActual.exigirAutenticado();
        return categoriaRepository.findAll(CategoriaSpecification.filtrar(texto, estado), pageable)
            .map(this::resumen);
    }

    /** Categorias activas para selectores y autocompletado. */
    @Transactional(readOnly = true)
    public List<CategoriaResumen> buscarActivas(String texto) {
        sesionActual.exigirAutenticado();
        return categoriaRepository.findAll(
            CategoriaSpecification.filtrar(texto, true),
            PageRequest.of(0, LIMITE_AUTOCOMPLETAR, Sort.by("nombre"))
        ).map(this::resumen).getContent();
    }

    @Transactional
    public CategoriaResumen crear(CategoriaComando comando) {
        sesionActual.exigirOwner(SIN_PERMISO);
        String nombre = validarNombre(comando);
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicado();
        }
        Categoria categoria = new Categoria();
        aplicar(categoria, comando, nombre);
        Categoria guardada = categoriaRepository.save(categoria);
        log.info("Categoría creada: {}", guardada.getNombre());
        return resumen(guardada);
    }

    @Transactional
    public CategoriaResumen actualizar(Long id, CategoriaComando comando) {
        sesionActual.exigirOwner(SIN_PERMISO);
        Categoria categoria = buscar(id);
        String nombre = validarNombre(comando);
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicado();
        }
        aplicar(categoria, comando, nombre);
        return resumen(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResumen cambiarEstado(Long id, boolean estado) {
        sesionActual.exigirOwner(SIN_PERMISO);
        Categoria categoria = buscar(id);
        if (!estado && productoRepository.existsByCategoriaIdAndEstadoTrue(id)) {
            throw new NegocioException("No se puede desactivar: la categoría tiene productos activos.");
        }
        categoria.setEstado(estado);
        return resumen(categoriaRepository.save(categoria));
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new NegocioException("No se encontró la categoría seleccionada."));
    }

    private String validarNombre(CategoriaComando comando) {
        return Textos.requerido("nombre", "el nombre", comando.nombre(), 100);
    }

    private void aplicar(Categoria categoria, CategoriaComando comando, String nombre) {
        categoria.setNombre(nombre);
        categoria.setDescripcion(Textos.opcional("descripcion", "descripción", comando.descripcion(), 500));
    }

    private CampoInvalidoException nombreDuplicado() {
        return new CampoInvalidoException("nombre", "Ya existe una categoría con ese nombre.");
    }

    private CategoriaResumen resumen(Categoria categoria) {
        return new CategoriaResumen(
            categoria.getId(), categoria.getNombre(), categoria.getDescripcion(),
            Boolean.TRUE.equals(categoria.getEstado())
        );
    }
}
