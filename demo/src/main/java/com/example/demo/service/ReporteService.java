package com.example.demo.service;

import com.example.demo.entity.EstadoTicket;
import com.example.demo.entity.Ticket;
import com.example.demo.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    @Autowired
    private TicketRepository ticketRepository;

    public Map<String, Object> generarReporteSLA() {
        List<Ticket> todosTickets = ticketRepository.findAll();
        return construirReporteSLA(todosTickets);
    }

    public Map<String, Long> generarReportePorEstado() {
        Map<String, Long> reporte = new HashMap<>();
        
        for (EstadoTicket estado : EstadoTicket.values()) {
            long count = ticketRepository.findByEstado(estado).size();
            reporte.put(estado.name(), count);
        }

        return reporte;
    }

    public Map<String, Object> generarReporteGeneral() {
        Map<String, Object> reporte = new HashMap<>();
        
        List<Ticket> tickets = ticketRepository.findAll();
        long ticketsResueltos = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                .count();
        long ticketsNoResueltos = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.ABIERTO
                        || t.getEstado() == EstadoTicket.EN_PROCESO
                        || t.getEstado() == EstadoTicket.EN_ESPERA)
                .count();
        long ticketsCancelados = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.CANCELADO)
                .count();

        reporte.put("totalTickets", tickets.size());
        reporte.put("ticketsResueltosTotal", ticketsResueltos);
        reporte.put("ticketsNoResueltos", ticketsNoResueltos);
        reporte.put("ticketsCancelados", ticketsCancelados);
        reporte.put("ticketsAbiertos", ticketRepository.findByEstado(EstadoTicket.ABIERTO).size());
        reporte.put("ticketsEnProceso", ticketRepository.findByEstado(EstadoTicket.EN_PROCESO).size());
        reporte.put("ticketsEnEspera", ticketRepository.findByEstado(EstadoTicket.EN_ESPERA).size());
        reporte.put("ticketsResueltos", ticketRepository.findByEstado(EstadoTicket.RESUELTO).size());
        reporte.put("ticketsCerrados", ticketRepository.findByEstado(EstadoTicket.CERRADO).size());

        return reporte;
    }

    // Reporte general filtrado por un periodo (fechas inclusive)
    public Map<String, Object> generarReporteGeneralPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        Map<String, Object> reporte = new HashMap<>();

        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null &&
                        !t.getFechaCreacion().isBefore(desde) && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        reporte.put("totalTickets", tickets.size());
        reporte.put("ticketsResueltosTotal", tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                .count());
        reporte.put("ticketsNoResueltos", tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.ABIERTO
                        || t.getEstado() == EstadoTicket.EN_PROCESO
                        || t.getEstado() == EstadoTicket.EN_ESPERA)
                .count());
        reporte.put("ticketsCancelados", tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.CANCELADO)
                .count());
        reporte.put("ticketsAbiertos", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.ABIERTO).count());
        reporte.put("ticketsEnProceso", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_PROCESO).count());
        reporte.put("ticketsEnEspera", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_ESPERA).count());
        reporte.put("ticketsResueltos", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.RESUELTO).count());
        reporte.put("ticketsCerrados", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CERRADO).count());

        return reporte;
    }

    // Reporte por estado filtrado por periodo
    public Map<String, Long> generarReportePorEstadoPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null &&
                        !t.getFechaCreacion().isBefore(desde) && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.groupingBy(t -> t.getEstado().name(), Collectors.counting()));
    }

    public List<Map<String, Object>> generarTopCategorias() {
        return construirTopCategorias(ticketRepository.findAll());
    }

    public List<Map<String, Object>> generarTopCategoriasPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null &&
                        !t.getFechaCreacion().isBefore(desde) && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());
        return construirTopCategorias(tickets);
    }

    // Reporte SLA por periodo
    public Map<String, Object> generarReporteSLAPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> todosTickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null &&
                        !t.getFechaCreacion().isBefore(desde) && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        return construirReporteSLA(todosTickets);
    }

    private Map<String, Object> construirReporteSLA(List<Ticket> tickets) {
        Map<String, Object> reporte = new HashMap<>();
        int totalTickets = tickets.size();
        int primeraRespuestaCumplen = 0;
        int primeraRespuestaIncumplen = 0;
        int primeraRespuestaTotal = 0;
        int resolucionCumplen = 0;
        int resolucionIncumplen = 0;
        int resolucionTotal = 0;

        for (Ticket ticket : tickets) {
            Integer slaPrimeraRespuestaMin = ticket.getTiempoRespuestaSLA();
            Integer tiempoPrimeraRespuestaSeg = ticket.getTiempoPrimeraRespuestaSeg();
            if (ticket.getFechaPrimeraRespuesta() != null
                    && tiempoPrimeraRespuestaSeg != null
                    && slaPrimeraRespuestaMin != null) {
                primeraRespuestaTotal++;
                if (tiempoPrimeraRespuestaSeg <= slaPrimeraRespuestaMin * 60L) {
                    primeraRespuestaCumplen++;
                } else {
                    primeraRespuestaIncumplen++;
                }
            }

            Integer tiempoResolucionSeg = ticket.getTiempoResolucionSeg();
            if (tiempoResolucionSeg != null && slaPrimeraRespuestaMin != null) {
                int tiempoEsperaSeg = ticket.getTiempoEsperaSeg() != null ? ticket.getTiempoEsperaSeg() : 0;
                long resolucionEfectivaSeg = Math.max(0L, (long) tiempoResolucionSeg - tiempoEsperaSeg);
                resolucionTotal++;
                if (resolucionEfectivaSeg <= slaPrimeraRespuestaMin * 60L) {
                    resolucionCumplen++;
                } else {
                    resolucionIncumplen++;
                }
            }
        }

        reporte.put("totalTickets", totalTickets);
        reporte.put("slaPrimeraRespuestaCumplen", primeraRespuestaCumplen);
        reporte.put("slaPrimeraRespuestaIncumplen", primeraRespuestaIncumplen);
        reporte.put("slaPrimeraRespuestaTotal", primeraRespuestaTotal);
        reporte.put("slaPrimeraRespuestaPorcentaje", formatearPorcentaje(primeraRespuestaCumplen, primeraRespuestaTotal));
        reporte.put("slaResolucionCumplen", resolucionCumplen);
        reporte.put("slaResolucionIncumplen", resolucionIncumplen);
        reporte.put("slaResolucionTotal", resolucionTotal);
        reporte.put("slaResolucionPorcentaje", formatearPorcentaje(resolucionCumplen, resolucionTotal));

        return reporte;
    }

    private String formatearPorcentaje(int cumplen, int total) {
        if (total > 0) {
            double porcentaje = (cumplen * 100.0) / total;
            return String.format("%.2f", porcentaje);
        }
        return "0.00";
    }

    private List<Map<String, Object>> construirTopCategorias(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(ticket -> {
                    if (ticket.getCategoria() == null || ticket.getCategoria().getNombre() == null) {
                        return "Sin categoría";
                    }
                    String nombre = ticket.getCategoria().getNombre().trim();
                    return nombre.isEmpty() ? "Sin categoría" : nombre;
                }, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Entry.comparingByKey()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("nombre", entry.getKey());
                    item.put("cantidad", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }
}
