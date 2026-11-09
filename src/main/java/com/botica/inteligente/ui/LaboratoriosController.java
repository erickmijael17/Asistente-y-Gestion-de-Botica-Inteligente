package com.botica.inteligente.ui;

import com.botica.inteligente.laboratorio.service.LaboratorioComando;
import com.botica.inteligente.laboratorio.service.LaboratorioResumen;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.i18n.Mensajes;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class LaboratoriosController extends CatalogoController<LaboratorioResumen, LaboratoriosController.Filtro> {

    record Filtro(String texto, Boolean estado) {
    }

    private final LaboratorioService laboratorioService;

    public LaboratoriosController(
        LaboratorioService laboratorioService, SesionActual sesionActual, Notificador notificador
    ) {
        super(sesionActual, notificador);
        this.laboratorioService = laboratorioService;
    }

    @Override
    protected String titulo() {
        return Mensajes.get("laboratorio.titulo");
    }

    @Override
    protected String textoBotonNuevo() {
        return Mensajes.get("laboratorio.boton.nuevo");
    }

    @Override
    protected void configurarColumnas() {
        columna(Mensajes.get("laboratorio.columna.nombre"), 220, LaboratorioResumen::nombre);
        columna(Mensajes.get("laboratorio.columna.ruc"), 110, l -> vacio(l.datos().ruc()));
        columna(Mensajes.get("laboratorio.columna.telefono"), 110, l -> vacio(l.datos().telefono()));
        columna(Mensajes.get("laboratorio.columna.correo"), 200, l -> vacio(l.datos().correo()));
        columna(Mensajes.get("catalogo.columna.estado"), 90, l -> l.estado()
            ? Mensajes.get("laboratorio.estado.activo")
            : Mensajes.get("laboratorio.estado.inactivo"));
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
    protected Page<LaboratorioResumen> consultar(Filtro filtro, Pageable pageable) {
        return laboratorioService.listar(filtro.texto(), filtro.estado(), pageable);
    }

    @Override
    protected boolean estaActivo(LaboratorioResumen fila) {
        return fila.estado();
    }

    @Override
    protected void cambiarEstado(LaboratorioResumen fila, boolean activo) {
        laboratorioService.cambiarEstado(fila.id(), activo);
    }

    @Override
    protected void abrirFormulario(LaboratorioResumen fila) {
        Formulario formulario = new Formulario(2);
        TextField nombre = formulario.texto(
            "nombre", Mensajes.get("laboratorio.form.nombreLabel"), Mensajes.get("laboratorio.form.nombrePrompt"), true
        );
        TextField ruc = formulario.texto(
            "ruc", Mensajes.get("laboratorio.columna.ruc"), Mensajes.get("laboratorio.form.rucPrompt"), false
        );
        TextField telefono = formulario.texto("telefono", Mensajes.get("laboratorio.columna.telefono"), "", false);
        TextField correo = formulario.texto(
            "correo", Mensajes.get("laboratorio.columna.correo"), "contacto@laboratorio.com", false
        );
        TextField sitioWeb = formulario.texto("sitioWeb", Mensajes.get("laboratorio.form.sitioWebLabel"), "", false);
        TextField direccion = formulario.texto("direccion", Mensajes.get("laboratorio.form.direccionLabel"), "", true);
        if (fila != null) {
            LaboratorioComando datos = fila.datos();
            nombre.setText(datos.nombre());
            ruc.setText(datos.ruc());
            telefono.setText(datos.telefono());
            correo.setText(datos.correo());
            sitioWeb.setText(datos.sitioWeb());
            direccion.setText(datos.direccion());
        }
        mostrarDialogo(
            fila == null ? Mensajes.get("laboratorio.dialogo.nuevo") : Mensajes.get("laboratorio.dialogo.editar"),
            formulario, () -> {
            LaboratorioComando comando = new LaboratorioComando(
                nombre.getText(), ruc.getText(), telefono.getText(),
                correo.getText(), direccion.getText(), sitioWeb.getText()
            );
            return () -> {
                if (fila == null) {
                    laboratorioService.crear(comando);
                } else {
                    laboratorioService.actualizar(fila.id(), comando);
                }
                return null;
            };
        });
    }

    private static String vacio(String valor) {
        return valor == null ? "" : valor;
    }
}
