package com.botica.inteligente.producto.entity;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "productos")
public class Producto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_interno", nullable = false, length = 60)
    private String codigoInterno;

    @Column(name = "codigo_barras", length = 80)
    private String codigoBarras;

    @Column(name = "nombre_comercial", nullable = false, length = 180)
    private String nombreComercial;

    @Column(name = "nombre_generico", length = 180)
    private String nombreGenerico;

    @Column(length = 700)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false, length = 30)
    private TipoProducto tipoProducto;

    @Column(name = "principio_activo", length = 180)
    private String principioActivo;

    @Column(length = 100)
    private String concentracion;

    @Column(length = 120)
    private String presentacion;

    @Column(name = "via_administracion", length = 100)
    private String viaAdministracion;

    @Column(name = "indicaciones_oficiales", length = 1000)
    private String indicacionesOficiales;

    @Column(length = 1000)
    private String contraindicaciones;

    @Column(length = 1000)
    private String advertencias;

    @Column(length = 1000)
    private String precauciones;

    @Column(name = "condiciones_almacenamiento", length = 500)
    private String condicionesAlmacenamiento;

    @Column(name = "requiere_receta", nullable = false)
    private Boolean requiereReceta = false;

    @Column(name = "registro_sanitario", length = 100)
    private String registroSanitario;

    @Column(name = "precio_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private Boolean estado = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratorio_id", nullable = false)
    private Laboratorio laboratorio;

    @Column(name = "fuente_informacion", length = 200)
    private String fuenteInformacion;

    @Column(name = "fecha_actualizacion_informacion")
    private LocalDate fechaActualizacionInformacion;
}
