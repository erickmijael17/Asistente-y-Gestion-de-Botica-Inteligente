package com.botica.inteligente.venta.service;

import com.botica.inteligente.config.EmpresaProperties;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.util.NumeroALetras;
import com.botica.inteligente.venta.enums.EstadoVenta;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/** Genera el comprobante de una venta en PDF A4. */
@Service
public class ComprobantePdfService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8);

    private final VentaService ventaService;
    private final EmpresaProperties empresa;

    public ComprobantePdfService(VentaService ventaService, EmpresaProperties empresa) {
        this.ventaService = ventaService;
        this.empresa = empresa;
    }

    public byte[] generar(Long ventaId) {
        return generar(ventaService.obtener(ventaId));
    }

    byte[] generar(VentaResumen venta) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            agregarCabecera(documento, venta);
            agregarCliente(documento, venta);
            agregarItems(documento, venta);
            agregarTotales(documento, venta);
            Paragraph pie = new Paragraph(
                "Documento generado por Botica Inteligente. No incluye validación ante SUNAT.", PEQUENA
            );
            pie.setSpacingBefore(24);
            pie.setAlignment(Element.ALIGN_CENTER);
            documento.add(pie);
        } catch (DocumentException error) {
            throw new NegocioException("No se pudo generar el comprobante en PDF.");
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    private void agregarCabecera(Document documento, VentaResumen venta) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[] {3, 2});
        tabla.setWidthPercentage(100);
        PdfPCell emisor = sinBorde(new Phrase(empresa.getNombre() + "\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        StringBuilder datos = new StringBuilder();
        if (!empresa.getRuc().isBlank()) {
            datos.append("RUC: ").append(empresa.getRuc()).append('\n');
        }
        if (!empresa.getDireccion().isBlank()) {
            datos.append(empresa.getDireccion()).append('\n');
        }
        if (!empresa.getTelefono().isBlank()) {
            datos.append("Tel.: ").append(empresa.getTelefono());
        }
        emisor.addElement(new Paragraph(datos.toString(), NORMAL));
        tabla.addCell(emisor);

        PdfPCell comprobante = new PdfPCell();
        comprobante.setPadding(8);
        comprobante.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph tipo = new Paragraph(venta.tipoComprobante().etiqueta().toUpperCase(), NEGRITA);
        tipo.setAlignment(Element.ALIGN_CENTER);
        Paragraph numero = new Paragraph(venta.comprobante(), NEGRITA);
        numero.setAlignment(Element.ALIGN_CENTER);
        comprobante.addElement(tipo);
        comprobante.addElement(numero);
        if (venta.estado() == EstadoVenta.ANULADA) {
            Paragraph anulada = new Paragraph("ANULADO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            anulada.setAlignment(Element.ALIGN_CENTER);
            comprobante.addElement(anulada);
        }
        tabla.addCell(comprobante);
        documento.add(tabla);
    }

    private void agregarCliente(Document documento, VentaResumen venta) throws DocumentException {
        String cliente = venta.clienteNombre() == null ? "CLIENTE VARIOS" : venta.clienteNombre();
        String documentoCliente = venta.clienteDocumento() == null ? "-" : venta.clienteDocumento();
        Paragraph datos = new Paragraph(
            "Fecha de emisión: " + FECHA_HORA.format(venta.fecha()) + "\n"
                + "Cliente: " + cliente + "\n"
                + "Documento: " + documentoCliente + "\n"
                + "Atendido por: " + venta.vendedor(),
            NORMAL
        );
        datos.setSpacingBefore(16);
        datos.setSpacingAfter(12);
        documento.add(datos);
    }

    private void agregarItems(Document documento, VentaResumen venta) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[] {1, 5, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);
        for (String titulo : new String[] {"Cant.", "Descripción", "P. unit.", "Importe"}) {
            PdfPCell cabecera = new PdfPCell(new Phrase(titulo, NEGRITA));
            cabecera.setBackgroundColor(new java.awt.Color(230, 230, 230));
            cabecera.setPadding(5);
            tabla.addCell(cabecera);
        }
        for (VentaResumen.Detalle detalle : venta.detalles()) {
            tabla.addCell(celda(String.valueOf(detalle.cantidad()), Element.ALIGN_CENTER));
            tabla.addCell(celda(detalle.codigoProducto() + " · " + detalle.nombreProducto(), Element.ALIGN_LEFT));
            tabla.addCell(celda(dinero(detalle.precioUnitario()), Element.ALIGN_RIGHT));
            tabla.addCell(celda(dinero(detalle.importe()), Element.ALIGN_RIGHT));
        }
        documento.add(tabla);
    }

    private void agregarTotales(Document documento, VentaResumen venta) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[] {4, 1.5f});
        tabla.setWidthPercentage(45);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setSpacingBefore(10);
        if (venta.aplicaIgv()) {
            tabla.addCell(celda("Op. gravada", Element.ALIGN_LEFT));
            tabla.addCell(celda(dinero(venta.baseImponible()), Element.ALIGN_RIGHT));
            tabla.addCell(celda("IGV", Element.ALIGN_LEFT));
            tabla.addCell(celda(dinero(venta.igv()), Element.ALIGN_RIGHT));
        }
        tabla.addCell(celdaNegrita("TOTAL"));
        PdfPCell total = celdaNegrita(dinero(venta.total()));
        total.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(total);
        documento.add(tabla);

        Paragraph letras = new Paragraph(NumeroALetras.soles(venta.total()), NORMAL);
        letras.setSpacingBefore(10);
        documento.add(letras);
        if (venta.observacion() != null) {
            documento.add(new Paragraph("Observación: " + venta.observacion(), PEQUENA));
        }
    }

    private static String dinero(BigDecimal valor) {
        return "S/ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static PdfPCell sinBorde(Phrase contenido) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(PdfPCell.NO_BORDER);
        return celda;
    }

    private static PdfPCell celda(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, NORMAL));
        celda.setPadding(4);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private static PdfPCell celdaNegrita(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, NEGRITA));
        celda.setPadding(4);
        return celda;
    }
}
