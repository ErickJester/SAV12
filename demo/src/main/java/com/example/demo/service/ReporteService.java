package com.example.demo.service;

import com.example.demo.entity.EstadoTicket;
import com.example.demo.entity.Ticket;
import com.example.demo.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    @Autowired
    private TicketRepository ticketRepository;

    // =========================
    // ===== SLA GLOBAL ========
    // =========================

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteSLA() {
        return construirReporteSla(ticketRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteSLAPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        return construirReporteSla(tickets);
    }

    private Map<String, Object> construirReporteSla(List<Ticket> tickets) {
        Map<String, Object> reporte = new HashMap<>();

        int total = 0;

        int cumpleGlobal = 0;
        int incumpleGlobal = 0;

        int cumplePrimera = 0;
        int incumplePrimera = 0;

        int cumpleResol = 0;
        int incumpleResol = 0;

        for (Ticket ticket : tickets) {
            if ((ticket.getEstado() == EstadoTicket.RESUELTO || ticket.getEstado() == EstadoTicket.CERRADO)
                    && ticket.getFechaResolucion() != null) {

                total++;

                SlaResultado r = evaluarSla(ticket);

                if (r.cumplePrimeraRespuesta) cumplePrimera++; else incumplePrimera++;
                if (r.cumpleResolucion)       cumpleResol++;   else incumpleResol++;
                if (r.cumpleGlobal)          cumpleGlobal++;  else incumpleGlobal++;
            }
        }

        // ---- contadores base ----
        reporte.put("totalTickets", total);

        reporte.put("ticketsCumplenSLA", cumpleGlobal);
        reporte.put("ticketsIncumplenSLA", incumpleGlobal);

        reporte.put("ticketsCumplenPrimeraRespuesta", cumplePrimera);
        reporte.put("ticketsIncumplenPrimeraRespuesta", incumplePrimera);

        reporte.put("ticketsCumplenResolucion", cumpleResol);
        reporte.put("ticketsIncumplenResolucion", incumpleResol);

        // ---- campos que tu template usa directo ----
        reporte.put("slaPrimeraRespuestaCumplen", cumplePrimera);
        reporte.put("slaPrimeraRespuestaIncumplen", incumplePrimera);
        reporte.put("slaPrimeraRespuestaPorcentaje", pct(cumplePrimera, total));

        reporte.put("slaResolucionCumplen", cumpleResol);
        reporte.put("slaResolucionIncumplen", incumpleResol);
        reporte.put("slaResolucionPorcentaje", pct(cumpleResol, total));

        reporte.put("slaCumplen", cumpleGlobal);
        reporte.put("slaIncumplen", incumpleGlobal);
        reporte.put("slaPorcentaje", pct(cumpleGlobal, total));

        reporte.put("porcentajeCumplimiento", pct(cumpleGlobal, total));

        return reporte;
    }

    private String pct(int ok, int total) {
        if (total <= 0) return "0.00";
        return String.format(Locale.US, "%.2f", (ok * 100.0) / total);
    }

    // =========================
    // ===== POR ESTADO ========
    // =========================

    @Transactional(readOnly = true)
    public Map<String, Long> generarReportePorEstado() {
        Map<String, Long> reporte = new HashMap<>();
        for (EstadoTicket estado : EstadoTicket.values()) {
            reporte.put(estado.name(), (long) ticketRepository.findByEstado(estado).size());
        }
        return reporte;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> generarReportePorEstadoPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.groupingBy(t -> t.getEstado().name(), Collectors.counting()));
    }

    // =========================
    // ===== GENERAL ===========
    // =========================

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteGeneral() {
        return generarReporteGeneralDesdeLista(ticketRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteGeneralPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        return generarReporteGeneralDesdeLista(tickets);
    }

    private Map<String, Object> generarReporteGeneralDesdeLista(List<Ticket> tickets) {
        Map<String, Object> reporte = new HashMap<>();

        reporte.put("totalTickets", tickets.size());

        long abiertos = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.ABIERTO || t.getEstado() == EstadoTicket.REABIERTO)
                .count();

        long enProceso = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_PROCESO).count();
        long enEspera = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_ESPERA).count();
        long resueltos = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.RESUELTO).count();
        long cerrados = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CERRADO).count();
        long cancelados = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CANCELADO).count();
        long reabiertos = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.REABIERTO).count();

        reporte.put("ticketsAbiertos", abiertos);
        reporte.put("ticketsReabiertos", reabiertos);
        reporte.put("ticketsEnProceso", enProceso);
        reporte.put("ticketsEnEspera", enEspera);
        reporte.put("ticketsResueltos", resueltos);
        reporte.put("ticketsCerrados", cerrados);
        reporte.put("ticketsCancelados", cancelados);

        long resueltosTotal = resueltos + cerrados;
        long noResueltos = abiertos + enProceso + enEspera;

        reporte.put("ticketsResueltosTotal", resueltosTotal);
        reporte.put("ticketsNoResueltos", noResueltos);

        reporte.put("topCategorias", obtenerTopCategorias(tickets));

        return reporte;
    }

    // =========================
    // ===== TOP CATEGORÍAS ====
    // =========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> generarTopCategorias() {
        return obtenerTopCategorias(ticketRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> generarTopCategoriasPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        return obtenerTopCategorias(tickets);
    }

    private List<Map<String, Object>> obtenerTopCategorias(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> (t.getCategoria() != null ? t.getCategoria().getNombre() : "Sin categoría"),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("nombre", e.getKey());
                    m.put("total", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // =========================
    // ===== SLA CORE ==========
    // =========================

    private SlaResultado evaluarSla(Ticket ticket) {
        if (ticket.getSlaPolitica() == null) {
            return new SlaResultado(false, false, false);
        }

        Integer tPrimera = ticket.getTiempoPrimeraRespuestaSeg();
        Integer tResol = ticket.getTiempoResolucionSeg();

        int slaPrimeraMin = ticket.getSlaPolitica().getSlaPrimeraRespuestaMin();
        int slaResolMin = ticket.getSlaPolitica().getSlaResolucionMin();

        int esperaSeg = ticket.getTiempoEsperaSeg() != null ? ticket.getTiempoEsperaSeg() : 0;

        boolean cumplePrimera = ticket.getFechaPrimeraRespuesta() != null
                && tPrimera != null
                && (tPrimera / 60.0) <= slaPrimeraMin;

        boolean cumpleResol = ticket.getFechaResolucion() != null
                && tResol != null
                && (Math.max(0, tResol - esperaSeg) / 60.0) <= slaResolMin;

        return new SlaResultado(cumplePrimera, cumpleResol, cumplePrimera && cumpleResol);
    }

    private static class SlaResultado {
        private final boolean cumplePrimeraRespuesta;
        private final boolean cumpleResolucion;
        private final boolean cumpleGlobal;

        private SlaResultado(boolean a, boolean b, boolean c) {
            this.cumplePrimeraRespuesta = a;
            this.cumpleResolucion = b;
            this.cumpleGlobal = c;
        }
    }
}
