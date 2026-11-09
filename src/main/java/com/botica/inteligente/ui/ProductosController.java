package com.botica.inteligente.ui;

import com.botica.inteligente.categoria.service.CategoriaResumen;
import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.laboratorio.service.LaboratorioResumen;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.informacion.BusquedaWebService;
import com.botica.inteligente.producto.service.ProductoComando;
import com.botica.inteligente.producto.service.ProductoFiltro;
import com.botica.inteligente.producto.service.ProductoResultado;
import com.botica.inteligente.producto.service.ProductoResumen;
import com.botica.inteligente.producto.service.ProductoService;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.i18n.Mensajes;
import java.util.Arrays;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
    private final BusquedaWebService busquedaWebService;

    private final Autocompletar<CategoriaResumen> filtroCategoria;
    private final Autocompletar<LaboratorioResumen> filtroLaboratorio;
    private final ComboBox<TipoProducto> filtroTipo = new ComboBox<>();

    public ProductosController(
        ProductoService productoService,
        CategoriaService categoriaService,
        LaboratorioService laboratorioService,
        BusquedaWebService busquedaWebService,
        SesionActual sesionActual,
        Notificador notificador
    ) {
        super(sesionActual, notificador);
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.laboratorioService = laboratorioService;
        this.busquedaWebService = busquedaWebService;
        this.filtroCategoria = new Autocompletar<>(categoriaService::buscarActivas, CategoriaResumen::nombre);
        this.filtroLaboratorio = new Autocompletar<>(laboratorioService::buscarActivos, LaboratorioResumen::nombre);
    }

    @Override
    protected String titulo() {
        return Mensajes.get("producto.titulo");
    }

    @Override
    protected String textoBotonNuevo() {
        return Mensajes.get("producto.boton.nuevo");
    }

    @Override
    protected List<Node> filtrosAdicionales() {
        filtroCategoria.setPromptText(Mensajes.get("producto.filtro.categoria"));
        filtroLaboratorio.setPromptText(Mensajes.get("producto.filtro.laboratorio"));
        filtroTipo.getItems().setAll(TipoProducto.values());
        filtroTipo.setPromptText(Mensajes.get("producto.filtro.tipo"));
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
        columna(Mensajes.get("producto.columna.codigo"), 80, p -> p.datos().codigoInterno());
        columna(Mensajes.get("producto.columna.producto"), 190, p -> p.datos().nombreComercial());
        columna(Mensajes.get("producto.columna.categoria"), 110, ProductoResumen::categoriaNombre);
        columna(Mensajes.get("producto.columna.laboratorio"), 120, ProductoResumen::laboratorioNombre);
        columna(Mensajes.get("producto.columna.precioVenta"), 80,
            p -> "S/ " + p.datos().precioVenta().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        columna(Mensajes.get("producto.columna.codigoBarras"), 115,
            p -> p.datos().codigoBarras() == null ? "" : p.datos().codigoBarras());
        columna(Mensajes.get("catalogo.columna.estado"), 75, p -> p.estado()
            ? Mensajes.get("laboratorio.estado.activo")
            : Mensajes.get("laboratorio.estado.inactivo"));
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
        f.titulo(Mensajes.get("producto.form.seccionIdentificacion"));
        final TextField codigoInterno = fila == null
            ? null
            : f.texto("codigoInterno", Mensajes.get("producto.form.codigoInternoLabel"), "", false);
        if (codigoInterno != null) {
            codigoInterno.setEditable(false);
            codigoInterno.setDisable(true);
        }
        TextField codigoBarras = f.texto(
            "codigoBarras", Mensajes.get("producto.form.codigoBarrasLabel"), Mensajes.get("producto.form.codigoBarrasPrompt"), false
        );
        TextField nombreComercial = f.texto("nombreComercial", Mensajes.get("producto.form.nombreComercialLabel"), "", false);
        TextField nombreGenerico = f.texto("nombreGenerico", Mensajes.get("producto.form.nombreGenericoLabel"), "", false);
        ComboBox<TipoProducto> tipo = f.combo(
            "tipoProducto", Mensajes.get("producto.form.tipoLabel"), Arrays.asList(TipoProducto.values()), false
        );
        CheckBox requiereReceta = f.check("requiereReceta", Mensajes.get("producto.form.requiereRecetaLabel"));
        Autocompletar<CategoriaResumen> categoria =
            new Autocompletar<>(categoriaService::buscarActivas, CategoriaResumen::nombre);
        categoria.setPromptText(Mensajes.get("producto.form.buscarPrompt"));
        f.agregar("categoria", Mensajes.get("producto.filtro.categoria") + " *", categoria, false);
        Autocompletar<LaboratorioResumen> laboratorio =
            new Autocompletar<>(laboratorioService::buscarActivos, LaboratorioResumen::nombre);
        laboratorio.setPromptText(Mensajes.get("producto.form.buscarPrompt"));
        f.agregar("laboratorio", Mensajes.get("producto.form.laboratorioLabel"), laboratorio, false);

        f.titulo(Mensajes.get("producto.form.seccionPrecios"));
        TextField precioCompra = f.texto("precioCompra", Mensajes.get("producto.form.precioCompraLabel"), "0.00", false);
        TextField precioVenta = f.texto("precioVenta", Mensajes.get("producto.form.precioVentaLabel"), "0.00", false);
        TextField stockMinimo = f.texto("stockMinimo", Mensajes.get("producto.form.stockMinimoLabel"), "5", false);

        f.titulo(Mensajes.get("producto.form.seccionMedicamento"));
        Button buscarInformacion = new Button(Mensajes.get("producto.busquedaWeb.boton"));
        HBox filaBusqueda = new HBox(buscarInformacion);
        f.agregarNodo(filaBusqueda);
        TextField principioActivo = f.texto("principioActivo", Mensajes.get("producto.form.principioActivoLabel"), "", false);
        TextField concentracion = f.texto(
            "concentracion", Mensajes.get("producto.form.concentracionLabel"), Mensajes.get("producto.form.concentracionPrompt"), false
        );
        TextField presentacion = f.texto(
            "presentacion", Mensajes.get("producto.form.presentacionLabel"), Mensajes.get("producto.form.presentacionPrompt"), false
        );
        TextField viaAdministracion = f.texto("viaAdministracion", Mensajes.get("producto.form.viaAdministracionLabel"), "", false);
        TextField registroSanitario = f.texto("registroSanitario", Mensajes.get("producto.form.registroSanitarioLabel"), "", false);
        TextField fuenteInformacion = f.texto("fuenteInformacion", Mensajes.get("producto.form.fuenteInformacionLabel"), "", false);
        TextArea descripcion = f.area("descripcion", Mensajes.get("categoria.columna.descripcion"), 2);
        TextArea indicaciones = f.area("indicacionesOficiales", Mensajes.get("producto.form.indicacionesLabel"), 2);
        TextArea contraindicaciones = f.area("contraindicaciones", Mensajes.get("producto.form.contraindicacionesLabel"), 2);
        TextArea advertencias = f.area("advertencias", Mensajes.get("producto.form.advertenciasLabel"), 2);
        TextArea precauciones = f.area("precauciones", Mensajes.get("producto.form.precaucionesLabel"), 2);
        TextArea almacenamiento = f.area("condicionesAlmacenamiento", Mensajes.get("producto.form.almacenamientoLabel"), 2);

        buscarInformacion.setOnAction(evento -> {
            String nombre = nombreComercial.getText() == null ? "" : nombreComercial.getText().trim();
            LaboratorioResumen laboratorioElegido = laboratorio.getValue();
            if (nombre.isEmpty() || laboratorioElegido == null) {
                avisar(Mensajes.get("producto.busquedaWeb.datosRequeridos"));
                return;
            }
            String concentracionTexto = concentracion.getText() == null ? "" : concentracion.getText().trim();
            String consulta = nombre
                + (concentracionTexto.isEmpty() ? "" : " " + concentracionTexto)
                + " " + laboratorioElegido.nombre()
                + " " + Mensajes.get("producto.busquedaWeb.sufijoConsulta");
            BusquedaInformacionDialogo.abrir(ventana(), consulta.trim(), busquedaWebService, ficha -> {
                if (ficha.principioActivo() != null) {
                    principioActivo.setText(ficha.principioActivo());
                }
                if (ficha.indicacionesOficiales() != null) {
                    indicaciones.setText(ficha.indicacionesOficiales());
                }
                if (ficha.contraindicaciones() != null) {
                    contraindicaciones.setText(ficha.contraindicaciones());
                }
                if (ficha.advertencias() != null) {
                    advertencias.setText(ficha.advertencias());
                }
                if (ficha.precauciones() != null) {
                    precauciones.setText(ficha.precauciones());
                }
                fuenteInformacion.setText(ficha.url());
            });
        });

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
            if (d.laboratorioId() != null) {
                laboratorio.seleccionar(new LaboratorioResumen(
                    d.laboratorioId(),
                    new com.botica.inteligente.laboratorio.service.LaboratorioComando(
                        fila.laboratorioNombre(), null, null, null, null, null),
                    true
                ));
            }
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

        mostrarDialogo(
            fila == null ? Mensajes.get("producto.dialogo.nuevo") : Mensajes.get("producto.dialogo.editar"), f, () -> {
            CategoriaResumen categoriaElegida = categoria.getValue();
            LaboratorioResumen laboratorioElegido = laboratorio.getValue();
            ProductoComando comando = new ProductoComando(
                codigoInterno == null ? null : codigoInterno.getText(), codigoBarras.getText(), nombreComercial.getText(),
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
                    ? Mensajes.get("producto.guardado")
                    : Mensajes.get("producto.guardadoConAviso", resultado.advertencia());
            };
        });
    }
}
