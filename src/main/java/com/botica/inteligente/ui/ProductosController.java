package com.botica.inteligente.ui;

import com.botica.inteligente.categoria.service.CategoriaResumen;
import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.laboratorio.service.LaboratorioResumen;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.service.ProductoComando;
import com.botica.inteligente.producto.service.ProductoFiltro;
import com.botica.inteligente.producto.service.ProductoResultado;
import com.botica.inteligente.producto.service.ProductoResumen;
import com.botica.inteligente.producto.service.ProductoService;
import com.botica.inteligente.security.SesionActual;
import java.util.Arrays;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ProductosController extends CatalogoController<ProductoResumen, ProductoFiltro> {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final LaboratorioService laboratorioService;

    private final Autocompletar<CategoriaResumen> filtroCategoria;
    private final Autocompletar<LaboratorioResumen> filtroLaboratorio;
    private final ComboBox<TipoProducto> filtroTipo = new ComboBox<>();

    public ProductosController(
        ProductoService productoService,
        CategoriaService categoriaService,
        LaboratorioService laboratorioService,
        SesionActual sesionActual,
        Notificador notificador
    ) {
        super(sesionActual, notificador);
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.laboratorioService = laboratorioService;
        this.filtroCategoria = new Autocompletar<>(categoriaService::buscarActivas, CategoriaResumen::nombre);
        this.filtroLaboratorio = new Autocompletar<>(laboratorioService::buscarActivos, LaboratorioResumen::nombre);
    }

    @Override
    protected String titulo() {
        return "Productos";
    }

    @Override
    protected String textoBotonNuevo() {
        return "Nuevo producto";
    }

    @Override
    protected List<Node> filtrosAdicionales() {
        filtroCategoria.setPromptText("Categoría");
        filtroLaboratorio.setPromptText("Laboratorio");
        filtroTipo.getItems().setAll(TipoProducto.values());
        filtroTipo.setPromptText("Tipo");
        filtroCategoria.valueProperty().addListener((obs, anterior, nuevo) -> reiniciar());
        filtroLaboratorio.valueProperty().addListener((obs, anterior, nuevo) -> reiniciar());
        filtroTipo.valueProperty().addListener((obs, anterior, nuevo) -> reiniciar());
        filtroCategoria.setPrefWidth(135);
        filtroLaboratorio.setPrefWidth(135);
        filtroTipo.setPrefWidth(110);
        filtroTipo.setMinWidth(Region.USE_PREF_SIZE);
        return List.of(filtroCategoria, filtroLaboratorio, filtroTipo);
    }

    @Override
    protected void configurarColumnas() {
        columna("Código", 80, p -> p.datos().codigoInterno());
        columna("Producto", 190, p -> p.datos().nombreComercial());
        columna("Categoría", 110, ProductoResumen::categoriaNombre);
        columna("Laboratorio", 120, ProductoResumen::laboratorioNombre);
        columna("P. venta", 80, p -> "S/ " + p.datos().precioVenta().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        columna("Cód. barras", 115, p -> p.datos().codigoBarras() == null ? "" : p.datos().codigoBarras());
        columna("Estado", 75, p -> p.estado() ? "Activo" : "Inactivo");
    }

    @Override
    protected Sort orden() {
        return Sort.by("nombreComercial");
    }

    @Override
    protected ProductoFiltro capturarFiltros(String texto, Boolean estado) {
        CategoriaResumen categoria = filtroCategoria.getValue();
        LaboratorioResumen laboratorio = filtroLaboratorio.getValue();
        return new ProductoFiltro(
            texto, estado,
            categoria == null ? null : categoria.id(),
            laboratorio == null ? null : laboratorio.id(),
            filtroTipo.getValue()
        );
    }

    @Override
    protected Page<ProductoResumen> consultar(ProductoFiltro filtro, Pageable pageable) {
        return productoService.listar(filtro, pageable);
    }

    @Override
    protected boolean estaActivo(ProductoResumen fila) {
        return fila.estado();
    }

    @Override
    protected void cambiarEstado(ProductoResumen fila, boolean activo) {
        productoService.cambiarEstado(fila.id(), activo);
    }

    @Override
    protected void abrirFormulario(ProductoResumen fila) {
        Formulario f = new Formulario(2);
        f.titulo("Identificación");
        TextField codigoInterno = f.texto("codigoInterno", "Código interno *", "Ej. MED-0001", false);
        TextField codigoBarras = f.texto("codigoBarras", "Código de barras (EAN-13)", "13 dígitos", false);
        TextField nombreComercial = f.texto("nombreComercial", "Nombre comercial *", "", false);
        TextField nombreGenerico = f.texto("nombreGenerico", "Nombre genérico", "", false);
        ComboBox<TipoProducto> tipo = f.combo("tipoProducto", "Tipo *", Arrays.asList(TipoProducto.values()), false);
        CheckBox requiereReceta = f.check("requiereReceta", "Requiere receta médica");
        Autocompletar<CategoriaResumen> categoria =
            new Autocompletar<>(categoriaService::buscarActivas, CategoriaResumen::nombre);
        categoria.setPromptText("Escriba para buscar");
        f.agregar("categoria", "Categoría *", categoria, false);
        Autocompletar<LaboratorioResumen> laboratorio =
            new Autocompletar<>(laboratorioService::buscarActivos, LaboratorioResumen::nombre);
        laboratorio.setPromptText("Escriba para buscar");
        f.agregar("laboratorio", "Laboratorio *", laboratorio, false);

        f.titulo("Precios y stock");
        TextField precioCompra = f.texto("precioCompra", "Precio de compra (S/) *", "0.00", false);
        TextField precioVenta = f.texto("precioVenta", "Precio de venta (S/) *", "0.00", false);
        TextField stockMinimo = f.texto("stockMinimo", "Stock mínimo *", "5", false);

        f.titulo("Información del medicamento");
        TextField principioActivo = f.texto("principioActivo", "Principio activo", "", false);
        TextField concentracion = f.texto("concentracion", "Concentración", "Ej. 500 mg", false);
        TextField presentacion = f.texto("presentacion", "Presentación", "Ej. Caja x 20 tabletas", false);
        TextField viaAdministracion = f.texto("viaAdministracion", "Vía de administración", "", false);
        TextField registroSanitario = f.texto("registroSanitario", "Registro sanitario", "", false);
        TextField fuenteInformacion = f.texto("fuenteInformacion", "Fuente de la información", "", false);
        TextArea descripcion = f.area("descripcion", "Descripción", 2);
        TextArea indicaciones = f.area("indicacionesOficiales", "Indicaciones", 2);
        TextArea contraindicaciones = f.area("contraindicaciones", "Contraindicaciones", 2);
        TextArea advertencias = f.area("advertencias", "Advertencias", 2);
        TextArea precauciones = f.area("precauciones", "Precauciones", 2);
        TextArea almacenamiento = f.area("condicionesAlmacenamiento", "Condiciones de almacenamiento", 2);

        if (fila == null) {
            tipo.setValue(TipoProducto.MEDICAMENTO);
            stockMinimo.setText("5");
        } else {
            ProductoComando d = fila.datos();
            codigoInterno.setText(d.codigoInterno());
            codigoBarras.setText(d.codigoBarras());
            nombreComercial.setText(d.nombreComercial());
            nombreGenerico.setText(d.nombreGenerico());
            tipo.setValue(d.tipoProducto());
            requiereReceta.setSelected(d.requiereReceta());
            categoria.seleccionar(new CategoriaResumen(d.categoriaId(), fila.categoriaNombre(), null, true));
            laboratorio.seleccionar(new LaboratorioResumen(
                d.laboratorioId(),
                new com.botica.inteligente.laboratorio.service.LaboratorioComando(
                    fila.laboratorioNombre(), null, null, null, null, null),
                true
            ));
            precioCompra.setText(d.precioCompra().toPlainString());
            precioVenta.setText(d.precioVenta().toPlainString());
            stockMinimo.setText(String.valueOf(d.stockMinimo()));
            principioActivo.setText(d.principioActivo());
            concentracion.setText(d.concentracion());
            presentacion.setText(d.presentacion());
            viaAdministracion.setText(d.viaAdministracion());
            registroSanitario.setText(d.registroSanitario());
            fuenteInformacion.setText(d.fuenteInformacion());
            descripcion.setText(d.descripcion());
            indicaciones.setText(d.indicacionesOficiales());
            contraindicaciones.setText(d.contraindicaciones());
            advertencias.setText(d.advertencias());
            precauciones.setText(d.precauciones());
            almacenamiento.setText(d.condicionesAlmacenamiento());
        }

        mostrarDialogo(fila == null ? "Nuevo producto" : "Editar producto", f, () -> {
            CategoriaResumen categoriaElegida = categoria.getValue();
            LaboratorioResumen laboratorioElegido = laboratorio.getValue();
            ProductoComando comando = new ProductoComando(
                codigoInterno.getText(), codigoBarras.getText(), nombreComercial.getText(),
                nombreGenerico.getText(), descripcion.getText(), tipo.getValue(), principioActivo.getText(),
                concentracion.getText(), presentacion.getText(), viaAdministracion.getText(),
                indicaciones.getText(), contraindicaciones.getText(), advertencias.getText(),
                precauciones.getText(), almacenamiento.getText(), requiereReceta.isSelected(),
                registroSanitario.getText(),
                EntradaNumerica.decimal("precioCompra", precioCompra.getText()),
                EntradaNumerica.decimal("precioVenta", precioVenta.getText()),
                EntradaNumerica.entero("stockMinimo", stockMinimo.getText()),
                categoriaElegida == null ? null : categoriaElegida.id(),
                laboratorioElegido == null ? null : laboratorioElegido.id(),
                fuenteInformacion.getText()
            );
            return () -> {
                ProductoResultado resultado = fila == null
                    ? productoService.crear(comando)
                    : productoService.actualizar(fila.id(), comando);
                return resultado.advertencia() == null
                    ? "Producto guardado."
                    : "Producto guardado. Aviso: " + resultado.advertencia();
            };
        });
    }
}
