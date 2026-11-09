package com.botica.inteligente.producto.service;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.producto.specification.ProductoSpecification;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import com.botica.inteligente.shared.util.Ean13;
import com.botica.inteligente.shared.util.Textos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99");
    private static final int STOCK_MINIMO_MAXIMO = 99_999;

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final SesionActual sesionActual;

    public ProductoService(
        ProductoRepository productoRepository,
        CategoriaRepository categoriaRepository,
        LaboratorioRepository laboratorioRepository,
        SesionActual sesionActual
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.sesionActual = sesionActual;
    }

    @Transactional(readOnly = true)
    public Page<ProductoResumen> listar(ProductoFiltro filtro, Pageable pageable) {
        sesionActual.exigirAutenticado();
        return productoRepository.findAll(ProductoSpecification.filtrar(filtro), pageable)
            .map(this::resumen);
    }

    @Transactional(readOnly = true)
    public ProductoResumen obtener(Long id) {
        sesionActual.exigirAutenticado();
        return resumen(buscar(id));
    }

    /** Busqueda exacta por codigo de barras (base para el escaneo EAN-13). */
    @Transactional(readOnly = true)
    public Optional<ProductoResumen> buscarPorCodigoBarras(String codigoBarras) {
        sesionActual.exigirAutenticado();
        String codigo = Textos.limpiar(codigoBarras);
        return codigo == null
            ? Optional.empty()
            : productoRepository.findByCodigoBarras(codigo).map(this::resumen);
    }

    @Transactional
    public ProductoResultado crear(ProductoComando comando) {
        sesionActual.exigirOwner(Mensajes.get("error.producto.sinPermiso"));
        ProductoComando datos = normalizar(comando);
        String codigoInterno = datos.codigoInterno() != null ? datos.codigoInterno() : generarCodigoInterno();
        validarUnicidad(codigoInterno, datos.codigoBarras(), null);
        Categoria categoria = categoriaActiva(datos.categoriaId());
        Laboratorio laboratorio = laboratorioActivo(datos.laboratorioId());

        Producto producto = new Producto();
        aplicar(producto, datos, categoria, laboratorio);
        producto.setCodigoInterno(codigoInterno);
        Producto guardado = productoRepository.save(producto);
        log.info("Producto creado: {}", guardado.getCodigoInterno());
        return resultado(guardado);
    }

    @Transactional
    public ProductoResultado actualizar(Long id, ProductoComando comando) {
        sesionActual.exigirOwner(Mensajes.get("error.producto.sinPermiso"));
        Producto producto = buscar(id);
        ProductoComando datos = normalizar(comando);
        String codigoInterno = datos.codigoInterno() != null ? datos.codigoInterno() : producto.getCodigoInterno();
        validarUnicidad(codigoInterno, datos.codigoBarras(), id);
        Categoria categoria = datos.categoriaId().equals(producto.getCategoria().getId())
            ? producto.getCategoria()
            : categoriaActiva(datos.categoriaId());
        Laboratorio laboratorioActual = producto.getLaboratorio();
        Laboratorio laboratorio = datos.laboratorioId() == null
            ? null
            : (laboratorioActual != null && datos.laboratorioId().equals(laboratorioActual.getId())
                ? laboratorioActual
                : laboratorioActivo(datos.laboratorioId()));

        aplicar(producto, datos, categoria, laboratorio);
        producto.setCodigoInterno(codigoInterno);
        return resultado(productoRepository.save(producto));
    }

    private String generarCodigoInterno() {
        long numero = productoRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("PRD-%06d", numero++);
        } while (productoRepository.existsByCodigoInternoIgnoreCase(codigo));
        return codigo;
    }

    @Transactional
    public ProductoResumen cambiarEstado(Long id, boolean estado) {
        sesionActual.exigirOwner(Mensajes.get("error.producto.sinPermiso"));
        Producto producto = buscar(id);
        producto.setEstado(estado);
        return resumen(productoRepository.save(producto));
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new NegocioException(Mensajes.get("error.producto.noEncontrado")));
    }

    private Categoria categoriaActiva(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new CampoInvalidoException("categoria", Mensajes.get("error.producto.categoriaNoExiste")));
        if (!Boolean.TRUE.equals(categoria.getEstado())) {
            throw new CampoInvalidoException("categoria", Mensajes.get("error.producto.categoriaInactiva"));
        }
        return categoria;
    }

    private Laboratorio laboratorioActivo(Long id) {
        if (id == null) {
            return null;
        }
        Laboratorio laboratorio = laboratorioRepository.findById(id)
            .orElseThrow(() -> new CampoInvalidoException("laboratorio", Mensajes.get("error.producto.laboratorioNoExiste")));
        if (!Boolean.TRUE.equals(laboratorio.getEstado())) {
            throw new CampoInvalidoException("laboratorio", Mensajes.get("error.producto.laboratorioInactivo"));
        }
        return laboratorio;
    }

    private void validarUnicidad(String codigoInterno, String codigoBarras, Long id) {
        boolean codigoInternoRepetido = id == null
            ? productoRepository.existsByCodigoInternoIgnoreCase(codigoInterno)
            : productoRepository.existsByCodigoInternoIgnoreCaseAndIdNot(codigoInterno, id);
        if (codigoInternoRepetido) {
            throw new CampoInvalidoException("codigoInterno", Mensajes.get("error.producto.codigoInternoDuplicado"));
        }
        if (codigoBarras != null) {
            boolean barrasRepetido = id == null
                ? productoRepository.existsByCodigoBarras(codigoBarras)
                : productoRepository.existsByCodigoBarrasAndIdNot(codigoBarras, id);
            if (barrasRepetido) {
                throw new CampoInvalidoException("codigoBarras", Mensajes.get("error.producto.codigoBarrasDuplicado"));
            }
        }
    }

    private ProductoComando normalizar(ProductoComando c) {
        String codigoBarras = Textos.limpiar(c.codigoBarras());
        if (codigoBarras != null && !Ean13.esValido(codigoBarras)) {
            throw new CampoInvalidoException("codigoBarras", Mensajes.get("error.producto.eanInvalido"));
        }
        if (c.tipoProducto() == null) {
            throw new CampoInvalidoException("tipoProducto", Mensajes.get("error.producto.tipoRequerido"));
        }
        if (c.categoriaId() == null) {
            throw new CampoInvalidoException("categoria", Mensajes.get("error.producto.categoriaRequerida"));
        }
        BigDecimal precioCompra = precio("precioCompra", "el precio de compra", c.precioCompra());
        BigDecimal precioVenta = precio("precioVenta", "el precio de venta", c.precioVenta());
        if (c.stockMinimo() == null || c.stockMinimo() < 0 || c.stockMinimo() > STOCK_MINIMO_MAXIMO) {
            throw new CampoInvalidoException(
                "stockMinimo",
                Mensajes.get("error.producto.stockMinimoRango", String.valueOf(STOCK_MINIMO_MAXIMO))
            );
        }
        return new ProductoComando(
            Textos.opcional("codigoInterno", "código interno", c.codigoInterno(), 60),
            codigoBarras,
            Textos.requerido("nombreComercial", "el nombre comercial", c.nombreComercial(), 180),
            Textos.opcional("nombreGenerico", "nombre genérico", c.nombreGenerico(), 180),
            Textos.opcional("descripcion", "descripción", c.descripcion(), 700),
            c.tipoProducto(),
            Textos.opcional("principioActivo", "principio activo", c.principioActivo(), 180),
            Textos.opcional("concentracion", "concentración", c.concentracion(), 100),
            Textos.opcional("presentacion", "presentación", c.presentacion(), 120),
            Textos.opcional("viaAdministracion", "vía de administración", c.viaAdministracion(), 100),
            Textos.opcional("indicacionesOficiales", "indicaciones", c.indicacionesOficiales(), 1000),
            Textos.opcional("contraindicaciones", "contraindicaciones", c.contraindicaciones(), 1000),
            Textos.opcional("advertencias", "advertencias", c.advertencias(), 1000),
            Textos.opcional("precauciones", "precauciones", c.precauciones(), 1000),
            Textos.opcional("condicionesAlmacenamiento", "condiciones de almacenamiento",
                c.condicionesAlmacenamiento(), 500),
            c.requiereReceta(),
            Textos.opcional("registroSanitario", "registro sanitario", c.registroSanitario(), 100),
            precioCompra,
            precioVenta,
            c.stockMinimo(),
            c.categoriaId(),
            c.laboratorioId(),
            Textos.opcional("fuenteInformacion", "fuente de información", c.fuenteInformacion(), 200)
        );
    }

    private BigDecimal precio(String campo, String etiqueta, BigDecimal valor) {
        if (valor == null) {
            throw new CampoInvalidoException(campo, Mensajes.get("validacion.requerido", etiqueta));
        }
        if (valor.signum() < 0) {
            throw new CampoInvalidoException(campo, Mensajes.get("validacion.noNegativo", etiqueta));
        }
        BigDecimal redondeado = valor.setScale(2, java.math.RoundingMode.HALF_UP);
        if (redondeado.compareTo(PRECIO_MAXIMO) > 0) {
            throw new CampoInvalidoException(campo, Mensajes.get("validacion.valorDemasiadoGrande", etiqueta));
        }
        return redondeado;
    }

    private void aplicar(Producto p, ProductoComando d, Categoria categoria, Laboratorio laboratorio) {
        p.setCodigoInterno(d.codigoInterno());
        p.setCodigoBarras(d.codigoBarras());
        p.setNombreComercial(d.nombreComercial());
        p.setNombreGenerico(d.nombreGenerico());
        p.setDescripcion(d.descripcion());
        p.setTipoProducto(d.tipoProducto());
        p.setPrincipioActivo(d.principioActivo());
        p.setConcentracion(d.concentracion());
        p.setPresentacion(d.presentacion());
        p.setViaAdministracion(d.viaAdministracion());
        p.setIndicacionesOficiales(d.indicacionesOficiales());
        p.setContraindicaciones(d.contraindicaciones());
        p.setAdvertencias(d.advertencias());
        p.setPrecauciones(d.precauciones());
        p.setCondicionesAlmacenamiento(d.condicionesAlmacenamiento());
        p.setRequiereReceta(d.requiereReceta());
        p.setRegistroSanitario(d.registroSanitario());
        p.setPrecioCompra(d.precioCompra());
        p.setPrecioVenta(d.precioVenta());
        p.setStockMinimo(d.stockMinimo());
        p.setCategoria(categoria);
        p.setLaboratorio(laboratorio);
        p.setFuenteInformacion(d.fuenteInformacion());
        p.setFechaActualizacionInformacion(LocalDate.now());
    }

    private ProductoResultado resultado(Producto producto) {
        String advertencia = producto.getPrecioVenta().compareTo(producto.getPrecioCompra()) < 0
            ? Mensajes.get("error.producto.ventaMenorQueCompra")
            : null;
        return new ProductoResultado(resumen(producto), advertencia);
    }

    private ProductoResumen resumen(Producto p) {
        ProductoComando datos = new ProductoComando(
            p.getCodigoInterno(), p.getCodigoBarras(), p.getNombreComercial(), p.getNombreGenerico(),
            p.getDescripcion(), p.getTipoProducto(), p.getPrincipioActivo(), p.getConcentracion(),
            p.getPresentacion(), p.getViaAdministracion(), p.getIndicacionesOficiales(),
            p.getContraindicaciones(), p.getAdvertencias(), p.getPrecauciones(),
            p.getCondicionesAlmacenamiento(), Boolean.TRUE.equals(p.getRequiereReceta()),
            p.getRegistroSanitario(), p.getPrecioCompra(), p.getPrecioVenta(), p.getStockMinimo(),
            p.getCategoria().getId(), p.getLaboratorio() == null ? null : p.getLaboratorio().getId(),
            p.getFuenteInformacion()
        );
        return new ProductoResumen(
            p.getId(), datos, p.getCategoria().getNombre(),
            p.getLaboratorio() == null ? null : p.getLaboratorio().getNombre(),
            Boolean.TRUE.equals(p.getEstado())
        );
    }
}
