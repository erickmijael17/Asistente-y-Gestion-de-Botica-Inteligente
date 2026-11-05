package com.botica.inteligente.ui;

import com.botica.inteligente.categoria.service.CategoriaResumen;
import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.laboratorio.service.LaboratorioResumen;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.service.ProductoComando;
import com.botica.inteligente.producto.service.ProductoResumen;
import com.botica.inteligente.producto.service.ProductoService;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

/**
 * Alta rapida de un producto nuevo a partir de un codigo de barras escaneado: pide solo lo
 * imprescindible y asocia el codigo al producto para que el catalogo crezca al recibir mercaderia.
 */
public final class AltaRapidaProducto {

    private AltaRapidaProducto() {
    }

    public static void abrir(
        Window propietario, String codigoBarras, ProductoService productoService,
        CategoriaService categoriaService, LaboratorioService laboratorioService,
        Consumer<ProductoResumen> alCrear, Consumer<String> avisar
    ) {
        Formulario f = new Formulario(2);
        f.titulo("Producto nuevo — código de barras " + codigoBarras);
        TextField barras = f.texto("codigoBarras", "Código de barras", "", false);
        barras.setText(codigoBarras);
        barras.setEditable(false);
        TextField codigoInterno = f.texto("codigoInterno", "Código interno *", "Ej. MED-0001", false);
        TextField nombre = f.texto("nombreComercial", "Nombre comercial *", "", true);
        ComboBox<TipoProducto> tipo = f.combo("tipoProducto", "Tipo *", Arrays.asList(TipoProducto.values()), false);
        tipo.setValue(TipoProducto.MEDICAMENTO);
        TextField stockMinimo = f.texto("stockMinimo", "Stock mínimo *", "", false);
        stockMinimo.setText("5");
        Autocompletar<CategoriaResumen> categoria =
            new Autocompletar<>(categoriaService::buscarActivas, CategoriaResumen::nombre);
        categoria.setPromptText("Escriba para buscar");
        f.agregar("categoria", "Categoría *", categoria, false);
        Autocompletar<LaboratorioResumen> laboratorio =
            new Autocompletar<>(laboratorioService::buscarActivos, LaboratorioResumen::nombre);
        laboratorio.setPromptText("Escriba para buscar");
        f.agregar("laboratorio", "Laboratorio *", laboratorio, false);
        TextField precioCompra = f.texto("precioCompra", "Precio de compra (S/) *", "0.00", false);
        TextField precioVenta = f.texto("precioVenta", "Precio de venta (S/) *", "0.00", false);
        TextField principioActivo = f.texto("principioActivo", "Principio activo", "", false);
        TextField concentracion = f.texto("concentracion", "Concentración", "Ej. 500 mg", false);
        TextField presentacion = f.texto("presentacion", "Presentación", "Ej. Caja x 20 tabletas", true);

        AtomicReference<ProductoResumen> creado = new AtomicReference<>();
        DialogoFormulario.mostrar(propietario, "Registrar producto nuevo", f, () -> {
            CategoriaResumen categoriaElegida = categoria.getValue();
            LaboratorioResumen laboratorioElegido = laboratorio.getValue();
            ProductoComando comando = new ProductoComando(
                codigoInterno.getText(), codigoBarras, nombre.getText(), null, null, tipo.getValue(),
                principioActivo.getText(), concentracion.getText(), presentacion.getText(), null, null, null, null,
                null, null, false, null,
                EntradaNumerica.decimal("precioCompra", precioCompra.getText()),
                EntradaNumerica.decimal("precioVenta", precioVenta.getText()),
                EntradaNumerica.entero("stockMinimo", stockMinimo.getText()),
                categoriaElegida == null ? null : categoriaElegida.id(),
                laboratorioElegido == null ? null : laboratorioElegido.id(),
                null
            );
            return () -> {
                creado.set(productoService.crear(comando).producto());
                return "Producto registrado y agregado a la compra.";
            };
        }, aviso -> {
            avisar.accept(aviso);
            alCrear.accept(creado.get());
        });
    }
}
