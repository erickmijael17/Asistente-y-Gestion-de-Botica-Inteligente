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
    private static final String SIN_PERMISO = "No tiene permisos para modificar productos.";
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
        sesionActual.exigirOwner(SIN_PERMISO);
        ProductoComando datos = normalizar(comando);
        validarUnicidad(datos, null);
        Categoria categoria = categoriaActiva(datos.categoriaId());
        Laboratorio laboratorio = laboratorioActivo(datos.laboratorioId());

        Producto producto = new Producto();
        aplicar(producto, datos, categoria, laboratorio);
        Producto guardado = productoRepository.save(producto);
        log.info("Producto creado: {}", guardado.getCodigoInterno());
        return resultado(guardado);
    }

    @Transactional
    public ProductoResultado actualizar(Long id, ProductoComando comando) {
        sesionActual.exigirOwner(SIN_PERMISO);
        Producto producto = buscar(id);
        ProductoComando datos = normalizar(comando);
        validarUnicidad(datos, id);
        Categoria categoria = datos.categoriaId().equals(producto.getCategoria().getId())
            ? producto.getCategoria()
            : categoriaActiva(datos.categoriaId());
        Laboratorio laboratorio = datos.laboratorioId().equals(producto.getLaboratorio().getId())
            ? producto.getLaboratorio()
            : laboratorioActivo(datos.laboratorioId());

        aplicar(producto, datos, categoria, laboratorio);
        return resultado(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResumen cambiarEstado(Long id, boolean estado) {
        sesionActual.exigirOwner(SIN_PERMISO);
        Producto producto = buscar(id);
        producto.setEstado(estado);
        return resumen(productoRepository.save(producto));
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new NegocioException("No se encontró el producto seleccionado."));
    }

    private Categoria categoriaActiva(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new CampoInvalidoException("categoria", "La categoría seleccionada no existe."));
        if (!Boolean.TRUE.equals(categoria.getEstado())) {
            throw new CampoInvalidoException("categoria", "La categoría seleccionada está inactiva.");
        }
        return categoria;
    }

    private Laboratorio laboratorioActivo(Long id) {
        Laboratorio laboratorio = laboratorioRepository.findById(id)
            .orElseThrow(() -> new CampoInvalidoException("laboratorio", "El laboratorio seleccionado no existe."));
        if (!Boolean.TRUE.equals(laboratorio.getEstado())) {
            throw new CampoInvalidoException("laboratorio", "El laboratorio seleccionado está inactivo.");
        }
        return laboratorio;
    }

    private void validarUnicidad(ProductoComando datos, Long id) {
        boolean codigoInternoRepetido = id == null
            ? productoRepository.existsByCodigoInternoIgnoreCase(datos.codigoInterno())
            : productoRepository.existsByCodigoInternoIgnoreCaseAndIdNot(datos.codigoInterno(), id);
        if (codigoInternoRepetido) {
            throw new CampoInvalidoException("codigoInterno", "Ya existe un producto con ese código interno.");
        }
        if (datos.codigoBarras() != null) {
            boolean barrasRepetido = id == null
                ? productoRepository.existsByCodigoBarras(datos.codigoBarras())
                : productoRepository.existsByCodigoBarrasAndIdNot(datos.codigoBarras(), id);
            if (barrasRepetido) {
                throw new CampoInvalidoException("codigoBarras", "Ya existe un producto con ese código de barras.");
            }
        }
    }

    private ProductoComando normalizar(ProductoComando c) {
        String codigoBarras = Textos.limpiar(c.codigoBarras());
        if (codigoBarras != null && !Ean13.esValido(codigoBarras)) {
            throw new CampoInvalidoException(
                "codigoBarras", "Código EAN-13 inválido: deben ser 13 dígitos con dígito verificador correcto."
            );
        }
        if (c.tipoProducto() == null) {
            throw new CampoInvalidoException("tipoProducto", "Seleccione el tipo de producto.");
        }
        if (c.categoriaId() == null) {
            throw new CampoInvalidoException("categoria", "Seleccione una categoría.");
        }
        if (c.laboratorioId() == null) {
            throw new CampoInvalidoException("laboratorio", "Seleccione un laboratorio.");
        }
        BigDecimal precioCompra = precio("precioCompra", "el precio de compra", c.precioCompra());
        BigDecimal precioVenta = precio("precioVenta", "el precio de venta", c.precioVenta());
        if (c.stockMinimo() == null || c.stockMinimo() < 0 || c.stockMinimo() > STOCK_MINIMO_MAXIMO) {
            throw new CampoInvalidoException(
                "stockMinimo", "El stock mínimo debe estar entre 0 y " + STOCK_MINIMO_MAXIMO + "."
            );
        }
        return new ProductoComando(
            Textos.requerido("codigoInterno", "el código interno", c.codigoInterno(), 60),
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
            throw new CampoInvalidoException(campo, "Ingrese " + etiqueta + ".");
        }
        if (valor.signum() < 0) {
            throw new CampoInvalidoException(campo, "Ingrese " + etiqueta + " mayor o igual a cero.");
        }
        BigDecimal redondeado = valor.setScale(2, java.math.RoundingMode.HALF_UP);
        if (redondeado.compareTo(PRECIO_MAXIMO) > 0) {
            throw new CampoInvalidoException(campo, "El valor de " + etiqueta + " es demasiado grande.");
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
            ? "El precio de venta es menor que el precio de compra."
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
            p.getCategoria().getId(), p.getLaboratorio().getId(), p.getFuenteInformacion()
        );
        return new ProductoResumen(
            p.getId(), datos, p.getCategoria().getNombre(), p.getLaboratorio().getNombre(),
            Boolean.TRUE.equals(p.getEstado())
        );
    }
}
