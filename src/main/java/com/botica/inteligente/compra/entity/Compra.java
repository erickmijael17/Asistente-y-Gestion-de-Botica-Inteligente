package com.botica.inteligente.compra.entity;

import com.botica.inteligente.compra.enums.EstadoCompra;
import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import com.botica.inteligente.proveedor.entity.Proveedor;
import com.botica.inteligente.shared.audit.AuditableEntity;
import com.botica.inteligente.usuario.entity.Usuario;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "compras")
public class Compra extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private Long id;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoCompra tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 40)
    private String numeroDocumento;

    @Column(name = "fecha_documento", nullable = false)
    private LocalDate fechaDocumento;

    @Column(name = "aplica_igv", nullable = false)
    private Boolean aplicaIgv;

    @Column(name = "precios_incluyen_igv", nullable = false)
    private Boolean preciosIncluyenIgv;

    @Column(name = "tasa_igv", nullable = false, precision = 5, scale = 4)
    private BigDecimal tasaIgv;

    @Column(name = "base_imponible", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseImponible;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoCompra estado;

    @Column(length = 500)
    private String observacion;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("item ASC")
    private List<CompraDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(CompraDetalle detalle) {
        detalle.setCompra(this);
        detalle.setItem(detalles.size() + 1);
        detalles.add(detalle);
    }

    public static String formatearCodigo(int numero) {
        return String.format("CP-%08d", numero);
    }
}
