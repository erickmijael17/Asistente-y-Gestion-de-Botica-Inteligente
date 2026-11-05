package com.botica.inteligente.ui;

import com.botica.inteligente.categoria.service.CategoriaComando;
import com.botica.inteligente.categoria.service.CategoriaResumen;
import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.security.SesionActual;
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
        return "Categorías";
    }

    @Override
    protected String textoBotonNuevo() {
        return "Nueva categoría";
    }

    @Override
    protected void configurarColumnas() {
        columna("Nombre", 220, CategoriaResumen::nombre);
        columna("Descripción", 380, c -> c.descripcion() == null ? "" : c.descripcion());
        columna("Estado", 90, c -> c.estado() ? "Activa" : "Inactiva");
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
        TextField nombre = formulario.texto("nombre", "Nombre *", "Ej. Analgésicos", false);
        TextArea descripcion = formulario.area("descripcion", "Descripción", 3);
        if (fila != null) {
            nombre.setText(fila.nombre());
            descripcion.setText(fila.descripcion());
        }
        mostrarDialogo(fila == null ? "Nueva categoría" : "Editar categoría", formulario, () -> {
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
