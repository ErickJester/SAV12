package com.example.demo.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.SolidBorder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    // Colores del diseño
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(16, 185, 129);
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(245, 158, 11);
    private static final DeviceRgb DANGER_COLOR = new DeviceRgb(239, 68, 68);

    public byte[] generarReportePDF(Map<String, Object> datos) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Título del reporte
        Paragraph titulo = new Paragraph("SAV12 - Reporte de Análisis")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(PRIMARY_COLOR);
        document.add(titulo);

        // Fecha de generación
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph fechaGen = new Paragraph("Generado: " + fecha)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY);
        document.add(fechaGen);

        document.add(new Paragraph("\n"));

        // Período seleccionado
        String periodo = (String) datos.get("periodo");
        Paragraph periodoP = new Paragraph("Período: " + periodo.toUpperCase())
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(periodoP);

        document.add(new Paragraph("\n"));

        // === KPIs PRINCIPALES ===
        agregarSeccion(document, "KPIs Principales");
        
        Map<String, Object> kpis = (Map<String, Object>) datos.get("kpis");
        Table kpiTable = new Table(4);
        kpiTable.setWidth(UnitValue.createPercentValue(100));

        agregarCeldaKPI(kpiTable, "Total Tickets", String.valueOf(kpis.get("totalTickets")), PRIMARY_COLOR);
        agregarCeldaKPI(kpiTable, "Resueltos", String.valueOf(kpis.get("ticketsResueltos")), SUCCESS_COLOR);
        agregarCeldaKPI(kpiTable, "Pendientes", String.valueOf(kpis.get("ticketsPendientes")), WARNING_COLOR);
        agregarCeldaKPI(kpiTable, "Críticos", String.valueOf(kpis.get("ticketsCriticos")), DANGER_COLOR);

        document.add(kpiTable);
        document.add(new Paragraph("\n"));

        // === CUMPLIMIENTO SLA ===
        agregarSeccion(document, "Cumplimiento de SLA");
        
        Map<String, Object> sla = (Map<String, Object>) datos.get("reporteSLA");
        Table slaTable = new Table(new float[]{3, 1, 1, 1});
        slaTable.setWidth(UnitValue.createPercentValue(100));

        agregarEncabezadoTabla(slaTable, "Métrica", "Cumplen", "Incumplen", "% Cumplimiento");
        
        agregarFilaTabla(slaTable, 
            "Primera Respuesta",
            String.valueOf(sla.get("ticketsCumplenPrimeraRespuesta")),
            String.valueOf(sla.get("ticketsIncumplenPrimeraRespuesta")),
            sla.get("slaPrimeraRespuestaPorcentaje") + "%"
        );
        
        agregarFilaTabla(slaTable,
            "Resolución",
            String.valueOf(sla.get("ticketsCumplenResolucion")),
            String.valueOf(sla.get("ticketsIncumplenResolucion")),
            sla.get("slaResolucionPorcentaje") + "%"
        );

        document.add(slaTable);
        document.add(new Paragraph("\n"));

        // === ANÁLISIS DE TIEMPOS ===
        agregarSeccion(document, "Análisis de Tiempos Promedio (minutos)");
        
        Map<String, Object> tiempos = (Map<String, Object>) datos.get("analisisTiempos");
        Table tiemposTable = new Table(3);
        tiemposTable.setWidth(UnitValue.createPercentValue(100));

        agregarEncabezadoTabla(tiemposTable, "Primera Respuesta", "Resolución", "En Espera");
        agregarFilaTabla(tiemposTable,
            String.valueOf(tiempos.get("tiempoPromedioRespuestaMin")),
            String.valueOf(tiempos.get("tiempoPromedioResolucionMin")),
            String.valueOf(tiempos.get("tiempoPromedioEsperaMin"))
        );

        document.add(tiemposTable);
        document.add(new Paragraph("\n"));

        // === DESEMPEÑO DE TÉCNICOS ===
        agregarSeccion(document, "Desempeño de Técnicos");
        
        List<Map<String, Object>> tecnicos = (List<Map<String, Object>>) datos.get("desempenoTecnicos");
        Table tecnicosTable = new Table(new float[]{2, 1, 1, 1, 1, 1, 1, 1});
        tecnicosTable.setWidth(UnitValue.createPercentValue(100));

        agregarEncabezadoTabla(tecnicosTable, "Técnico", "Total", "Resueltos", "En Proceso", 
                              "Reabiertos", "Tasa Éxito", "SLA %", "Tiempo Prom.");

        for (Map<String, Object> tec : tecnicos) {
            agregarFilaTabla(tecnicosTable,
                String.valueOf(tec.get("tecnico")),
                String.valueOf(tec.get("totalAsignados")),
                String.valueOf(tec.get("resueltos")),
                String.valueOf(tec.get("enProceso")),
                String.valueOf(tec.get("reabiertos")),
                tec.get("tasaExito") + "%",
                tec.get("cumplimientoSLA") + "%",
                tec.get("tiempoPromedioResolucionMin") + " min"
            );
        }

        document.add(tecnicosTable);
        document.add(new Paragraph("\n"));

        // === ANÁLISIS POR PRIORIDAD ===
        agregarSeccion(document, "Análisis por Prioridad");
        
        List<Map<String, Object>> prioridades = (List<Map<String, Object>>) datos.get("analisisPorPrioridad");
        Table prioridadTable = new Table(new float[]{2, 1, 1, 1, 1});
        prioridadTable.setWidth(UnitValue.createPercentValue(100));

        agregarEncabezadoTabla(prioridadTable, "Prioridad", "Total", "Resueltos", "Pendientes", "% Resolución");

        for (Map<String, Object> prio : prioridades) {
            int total = (int) prio.get("total");
            int resueltos = (int) prio.get("resueltos");
            int pendientes = (int) prio.get("pendientes");
            double porcentaje = total > 0 ? (resueltos * 100.0 / total) : 0;
            
            agregarFilaTabla(prioridadTable,
                String.valueOf(prio.get("prioridad")),
                String.valueOf(total),
                String.valueOf(resueltos),
                String.valueOf(pendientes),
                String.format("%.1f%%", porcentaje)
            );
        }

        document.add(prioridadTable);
        document.add(new Paragraph("\n"));

        // === TOP 10 UBICACIONES ===
        agregarSeccion(document, "Top 10 Ubicaciones con Más Tickets");
        
        List<Map<String, Object>> ubicaciones = (List<Map<String, Object>>) datos.get("analisisPorUbicaciones");
        Table ubicacionesTable = new Table(new float[]{3, 1, 1, 1});
        ubicacionesTable.setWidth(UnitValue.createPercentValue(100));

        agregarEncabezadoTabla(ubicacionesTable, "Ubicación", "Total", "Resueltos", "Pendientes");

        for (Map<String, Object> ubi : ubicaciones) {
            agregarFilaTabla(ubicacionesTable,
                String.valueOf(ubi.get("ubicacion")),
                String.valueOf(ubi.get("total")),
                String.valueOf(ubi.get("resueltos")),
                String.valueOf(ubi.get("pendientes"))
            );
        }

        document.add(ubicacionesTable);
        document.add(new Paragraph("\n"));

        // === TICKETS PROBLEMÁTICOS ===
        agregarSeccion(document, "Tickets Problemáticos");
        
        Map<String, Object> problematicos = (Map<String, Object>) datos.get("ticketsProblematicos");
        
        // Más reabiertos
        Paragraph subTitulo1 = new Paragraph("Tickets Más Reabiertos")
                .setFontSize(12)
                .setBold()
                .setFontColor(DANGER_COLOR);
        document.add(subTitulo1);
        
        List<Map<String, Object>> masReabiertos = (List<Map<String, Object>>) problematicos.get("masReabiertos");
        Table reabiertosTable = new Table(new float[]{1, 3, 2, 1});
        reabiertosTable.setWidth(UnitValue.createPercentValue(100));
        
        agregarEncabezadoTabla(reabiertosTable, "ID", "Título", "Técnico", "Reaperturas");
        
        for (Map<String, Object> ticket : masReabiertos) {
            agregarFilaTabla(reabiertosTable,
                "#" + ticket.get("id"),
                String.valueOf(ticket.get("titulo")),
                String.valueOf(ticket.get("tecnicoAsignado")),
                String.valueOf(ticket.get("contadorReaperturas"))
            );
        }
        
        document.add(reabiertosTable);
        document.add(new Paragraph("\n"));

        // Pie de página
        Paragraph footer = new Paragraph("Este reporte fue generado automáticamente por SAV12")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setFixedPosition(50, 30, 500);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    public String generarReporteCSV(Map<String, Object> datos) throws Exception {
        StringWriter sw = new StringWriter();
        CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL);

        // Encabezado del reporte
        printer.printRecord("SAV12 - REPORTE DE ANÁLISIS");
        printer.printRecord("Generado:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        printer.printRecord("Período:", datos.get("periodo"));
        printer.println();

        // KPIs
        printer.printRecord("=== KPIs PRINCIPALES ===");
        Map<String, Object> kpis = (Map<String, Object>) datos.get("kpis");
        printer.printRecord("Métrica", "Valor");
        printer.printRecord("Total Tickets", kpis.get("totalTickets"));
        printer.printRecord("Tickets Resueltos", kpis.get("ticketsResueltos"));
        printer.printRecord("Tickets Pendientes", kpis.get("ticketsPendientes"));
        printer.printRecord("Tickets Críticos", kpis.get("ticketsCriticos"));
        printer.printRecord("Tasa de Resolución", kpis.get("tasaResolucion") + "%");
        printer.println();

        // SLA
        printer.printRecord("=== CUMPLIMIENTO SLA ===");
        Map<String, Object> sla = (Map<String, Object>) datos.get("reporteSLA");
        printer.printRecord("Métrica", "Cumplen", "Incumplen", "% Cumplimiento");
        printer.printRecord("Primera Respuesta", 
            sla.get("ticketsCumplenPrimeraRespuesta"),
            sla.get("ticketsIncumplenPrimeraRespuesta"),
            sla.get("slaPrimeraRespuestaPorcentaje") + "%");
        printer.printRecord("Resolución",
            sla.get("ticketsCumplenResolucion"),
            sla.get("ticketsIncumplenResolucion"),
            sla.get("slaResolucionPorcentaje") + "%");
        printer.println();

        // Tiempos
        printer.printRecord("=== TIEMPOS PROMEDIO (minutos) ===");
        Map<String, Object> tiempos = (Map<String, Object>) datos.get("analisisTiempos");
        printer.printRecord("Primera Respuesta", "Resolución", "En Espera");
        printer.printRecord(
            tiempos.get("tiempoPromedioRespuestaMin"),
            tiempos.get("tiempoPromedioResolucionMin"),
            tiempos.get("tiempoPromedioEsperaMin"));
        printer.println();

        // Técnicos
        printer.printRecord("=== DESEMPEÑO TÉCNICOS ===");
        List<Map<String, Object>> tecnicos = (List<Map<String, Object>>) datos.get("desempenoTecnicos");
        printer.printRecord("Técnico", "Total", "Resueltos", "En Proceso", "Reabiertos", 
                          "Tasa Éxito %", "SLA %", "Tiempo Prom. (min)");
        for (Map<String, Object> tec : tecnicos) {
            printer.printRecord(
                tec.get("tecnico"),
                tec.get("totalAsignados"),
                tec.get("resueltos"),
                tec.get("enProceso"),
                tec.get("reabiertos"),
                tec.get("tasaExito"),
                tec.get("cumplimientoSLA"),
                tec.get("tiempoPromedioResolucionMin"));
        }
        printer.println();

        // Prioridades
        printer.printRecord("=== ANÁLISIS POR PRIORIDAD ===");
        List<Map<String, Object>> prioridades = (List<Map<String, Object>>) datos.get("analisisPorPrioridad");
        printer.printRecord("Prioridad", "Total", "Resueltos", "Pendientes");
        for (Map<String, Object> prio : prioridades) {
            printer.printRecord(
                prio.get("prioridad"),
                prio.get("total"),
                prio.get("resueltos"),
                prio.get("pendientes"));
        }
        printer.println();

        // Ubicaciones
        printer.printRecord("=== TOP 10 UBICACIONES ===");
        List<Map<String, Object>> ubicaciones = (List<Map<String, Object>>) datos.get("analisisPorUbicaciones");
        printer.printRecord("Ubicación", "Total", "Resueltos", "Pendientes");
        for (Map<String, Object> ubi : ubicaciones) {
            printer.printRecord(
                ubi.get("ubicacion"),
                ubi.get("total"),
                ubi.get("resueltos"),
                ubi.get("pendientes"));
        }

        printer.flush();
        return sw.toString();
    }

    // Métodos auxiliares para el PDF
    private void agregarSeccion(Document document, String titulo) {
        Paragraph seccion = new Paragraph(titulo)
                .setFontSize(16)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(seccion);
    }

    private void agregarCeldaKPI(Table table, String label, String valor, DeviceRgb color) {
        Cell cell = new Cell();
        cell.add(new Paragraph(label).setFontSize(10).setFontColor(ColorConstants.GRAY));
        cell.add(new Paragraph(valor).setFontSize(20).setBold().setFontColor(color));
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setPadding(10);
        cell.setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
        table.addCell(cell);
    }

    private void agregarEncabezadoTabla(Table table, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell();
            cell.add(new Paragraph(header).setBold().setFontSize(10));
            cell.setBackgroundColor(new DeviceRgb(248, 250, 252));
            cell.setTextAlignment(TextAlignment.CENTER);
            cell.setPadding(8);
            table.addHeaderCell(cell);
        }
    }

    private void agregarFilaTabla(Table table, String... valores) {
        for (String valor : valores) {
            Cell cell = new Cell();
            cell.add(new Paragraph(valor).setFontSize(9));
            cell.setTextAlignment(TextAlignment.CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }
}
