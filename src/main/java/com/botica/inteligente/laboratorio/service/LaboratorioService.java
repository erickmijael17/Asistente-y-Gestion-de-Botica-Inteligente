package com.botica.inteligente.laboratorio.service;

import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.laboratorio.specification.LaboratorioSpecification;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import com.botica.inteligente.shared.util.Textos;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LaboratorioService {

    private static final Logger log = LoggerFactory.getLogger(LaboratorioService.class);
    private static final int LIMITE_AUTOCOMPLETAR = 10;
    private static final Pattern RUC = Pattern.compile("\\d{11}");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final LaboratorioRepository laboratorioRepository;
    private final ProductoRepository productoRepository;
    private final SesionActual sesionActual;

    public LaboratorioService(
        LaboratorioRepository laboratorioRepository,
        ProductoRepository productoRepository,
        SesionActual sesionActual
    ) {
        this.laboratorioRepository = laboratorioRepository;
        this.productoRepository = productoRepository;
        this.sesionActual = sesionActual;
    }

    @Transactional(readOnly = true)
    public Page<LaboratorioResumen> listar(String texto, Boolean estado, Pageable pageable) {
        sesionActual.exigirAutenticado();
        return laboratorioRepository.findAll(LaboratorioSpecification.filtrar(texto, estado), pageable)
            .map(this::resumen);
    }

    @Transactional(readOnly = true)
    public List<LaboratorioResumen> buscarActivos(String texto) {
        sesionActual.exigirAutenticado();
        return laboratorioRepository.findAll(
            LaboratorioSpecification.filtrar(texto, true),
            PageRequest.of(0, LIMITE_AUTOCOMPLETAR, Sort.by("nombre"))
        ).map(this::resumen).getContent();
    }

    @Transactional
    public LaboratorioResumen crear(LaboratorioComando comando) {
        sesionActual.exigirOwner(Mensajes.get("error.laboratorio.sinPermiso"));
        LaboratorioComando datos = normalizar(comando);
        if (laboratorioRepository.existsByNombreIgnoreCase(datos.nombre())) {
            throw nombreDuplicado();
        }
        if (datos.ruc() != null && laboratorioRepository.existsByRuc(datos.ruc())) {
            throw rucDuplicado();
        }
        Laboratorio laboratorio = new Laboratorio();
        aplicar(laboratorio, datos);
        Laboratorio guardado = laboratorioRepository.save(laboratorio);
        log.info("Laboratorio creado: {}", guardado.getNombre());
        return resumen(guardado);
    }

    @Transactional
    public LaboratorioResumen actualizar(Long id, LaboratorioComando comando) {
        sesionActual.exigirOwner(Mensajes.get("error.laboratorio.sinPermiso"));
        Laboratorio laboratorio = buscar(id);
        LaboratorioComando datos = normalizar(comando);
        if (laboratorioRepository.existsByNombreIgnoreCaseAndIdNot(datos.nombre(), id)) {
            throw nombreDuplicado();
        }
        if (datos.ruc() != null && laboratorioRepository.existsByRucAndIdNot(datos.ruc(), id)) {
            throw rucDuplicado();
        }
        aplicar(laboratorio, datos);
        return resumen(laboratorioRepository.save(laboratorio));
    }

    @Transactional
    public LaboratorioResumen cambiarEstado(Long id, boolean estado) {
        sesionActual.exigirOwner(Mensajes.get("error.laboratorio.sinPermiso"));
        Laboratorio laboratorio = buscar(id);
        if (!estado && productoRepository.existsByLaboratorioIdAndEstadoTrue(id)) {
            throw new NegocioException(Mensajes.get("error.laboratorio.tieneProductosActivos"));
        }
        laboratorio.setEstado(estado);
        return resumen(laboratorioRepository.save(laboratorio));
    }

    private Laboratorio buscar(Long id) {
        return laboratorioRepository.findById(id)
            .orElseThrow(() -> new NegocioException(Mensajes.get("error.laboratorio.noEncontrado")));
    }

    private LaboratorioComando normalizar(LaboratorioComando c) {
        String ruc = Textos.limpiar(c.ruc());
        if (ruc != null && !RUC.matcher(ruc).matches()) {
            throw new CampoInvalidoException("ruc", Mensajes.get("error.laboratorio.rucInvalido"));
        }
        String correo = Textos.opcional("correo", "correo", c.correo(), 150);
        if (correo != null && !CORREO.matcher(correo).matches()) {
            throw new CampoInvalidoException("correo", Mensajes.get("error.laboratorio.correoInvalido"));
        }
        return new LaboratorioComando(
            Textos.requerido("nombre", "el nombre", c.nombre(), 150),
            ruc,
            Textos.opcional("telefono", "teléfono", c.telefono(), 30),
            correo,
            Textos.opcional("direccion", "dirección", c.direccion(), 250),
            Textos.opcional("sitioWeb", "sitio web", c.sitioWeb(), 150)
        );
    }

    private void aplicar(Laboratorio laboratorio, LaboratorioComando datos) {
        laboratorio.setNombre(datos.nombre());
        laboratorio.setRuc(datos.ruc());
        laboratorio.setTelefono(datos.telefono());
        laboratorio.setCorreo(datos.correo());
        laboratorio.setDireccion(datos.direccion());
        laboratorio.setSitioWeb(datos.sitioWeb());
    }

    private CampoInvalidoException nombreDuplicado() {
        return new CampoInvalidoException("nombre", Mensajes.get("error.laboratorio.nombreDuplicado"));
    }

    private CampoInvalidoException rucDuplicado() {
        return new CampoInvalidoException("ruc", Mensajes.get("error.laboratorio.rucDuplicado"));
    }

    private LaboratorioResumen resumen(Laboratorio l) {
        return new LaboratorioResumen(
            l.getId(),
            new LaboratorioComando(
                l.getNombre(), l.getRuc(), l.getTelefono(), l.getCorreo(), l.getDireccion(), l.getSitioWeb()
            ),
            Boolean.TRUE.equals(l.getEstado())
        );
    }
}
