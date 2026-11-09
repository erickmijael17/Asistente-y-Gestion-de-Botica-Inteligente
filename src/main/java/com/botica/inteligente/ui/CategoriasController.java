package com.botica.inteligente.ui;

import com.botica.inteligente.categoria.service.CategoriaComando;
import com.botica.inteligente.categoria.service.CategoriaResumen;
import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.i18n.Mensajes;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CategoriasController extends CatalogoController<CategoriaResumen, CategoriasController.Filtro> {

    record Filtro(String texto, Boolean estado) {
    }

    private final CategoriaService categoriaService;

    public CategoriasController(
        CategoriaService categoriaService, SesionActual sesionActual, Notificador notificador
    ) {
        super(sesionActual, notificador);
        this.categoriaService = categoriaService;
    }

    @Override
    protected String titulo() {
        return Mensajes.get("categoria.titulo");
    }

    @Override
    protected String textoBotonNuevo() {
        return Mensajes.get("categoria.boton.nuevo");
    }

    @Override
    protected void configurarColumnas() {
        columna(Mensajes.get("categoria.columna.nombre"), 220, CategoriaResumen::nombre);
        columna(Mensajes.get("categoria.columna.descripcion"), 380, c -> c.descripcion() == null ? "" : c.descripcion());
        columna(Mensajes.get("catalogo.columna.estado"), 90, c -> c.estado()
            ? Mensajes.get("categoria.estado.activa")
            : Mensajes.get("categoria.estado.inactiva"));
    }

    @Override
    protected Sort orden() {
        return Sort.by("nombre");
    }

    @Override
    protected Filtro capturarFiltros(String texto, Boolean estado) {
        return new Filtro(texto, estado);
    }

    @Override
    protected Page<CategoriaResumen> consultar(Filtro filtro, Pageable pageable) {
        return categoriaService.listar(filtro.texto(), filtro.estado(), pageable);
    }

    @Override
    protected boolean estaActivo(CategoriaResumen fila) {
        return fila.estado();
    }

    @Override
    protected void cambiarEstado(CategoriaResumen fila, boolean activo) {
        categoriaService.cambiarEstado(fila.id(), activo);
    }

    @Override
    protected void abrirFormulario(CategoriaResumen fila) {
        Formulario formulario = new Formulario(1);
        TextField nombre = formulario.texto(
            "nombre", Mensajes.get("categoria.form.nombreLabel"), Mensajes.get("categoria.form.nombrePrompt"), false
        );
        TextArea descripcion = formulario.area("descripcion", Mensajes.get("categoria.columna.descripcion"), 3);
        if (fila != null) {
            nombre.setText(fila.nombre());
            descripcion.setText(fila.descripcion());
        }
        mostrarDialogo(
            fila == null ? Mensajes.get("categoria.dialogo.nuevo") : Mensajes.get("categoria.dialogo.editar"),
            formulario, () -> {
            CategoriaComando comando = new CategoriaComando(nombre.getText(), descripcion.getText());
            return () -> {
                if (fila == null) {
                    categoriaService.crear(comando);
                } else {
                    categoriaService.actualizar(fila.id(), comando);
                }
                return null;
            };
        });
    }
}
