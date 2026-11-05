package com.botica.inteligente.ui;

import com.botica.inteligente.laboratorio.service.LaboratorioComando;
import com.botica.inteligente.laboratorio.service.LaboratorioResumen;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.security.SesionActual;
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
        return "Laboratorios";
    }

    @Override
    protected String textoBotonNuevo() {
        return "Nuevo laboratorio";
    }

    @Override
    protected void configurarColumnas() {
        columna("Nombre", 220, LaboratorioResumen::nombre);
        columna("RUC", 110, l -> vacio(l.datos().ruc()));
        columna("Teléfono", 110, l -> vacio(l.datos().telefono()));
        columna("Correo", 200, l -> vacio(l.datos().correo()));
        columna("Estado", 90, l -> l.estado() ? "Activo" : "Inactivo");
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
        TextField nombre = formulario.texto("nombre", "Nombre *", "Nombre del laboratorio", true);
        TextField ruc = formulario.texto("ruc", "RUC", "11 dígitos", false);
        TextField telefono = formulario.texto("telefono", "Teléfono", "", false);
        TextField correo = formulario.texto("correo", "Correo", "contacto@laboratorio.com", false);
        TextField sitioWeb = formulario.texto("sitioWeb", "Sitio web", "", false);
        TextField direccion = formulario.texto("direccion", "Dirección", "", true);
        if (fila != null) {
            LaboratorioComando datos = fila.datos();
            nombre.setText(datos.nombre());
            ruc.setText(datos.ruc());
            telefono.setText(datos.telefono());
            correo.setText(datos.correo());
            sitioWeb.setText(datos.sitioWeb());
            direccion.setText(datos.direccion());
        }
        mostrarDialogo(fila == null ? "Nuevo laboratorio" : "Editar laboratorio", formulario, () -> {
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
