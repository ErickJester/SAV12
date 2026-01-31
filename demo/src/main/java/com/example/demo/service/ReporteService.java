package com.example.demo.service;

import com.example.demo.entity.EstadoTicket;
import com.example.demo.entity.Ticket;
import com.example.demo.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    @Autowired
    private TicketRepository ticketRepository;

    public Map<String, Object> generarReporteSLA() {
        Map<String, Object> reporte = new HashMap<>();
        
        List<Ticket> todosTickets = ticketRepository.findAll();
        int totalTickets = 0;
        int ticketsCumplenSLA = 0;
        int ticketsIncumplenSLA = 0;
        int ticketsCumplenPrimeraRespuesta = 0;
        int ticketsIncumplenPrimeraRespuesta = 0;
        int ticketsCumplenResolucion = 0;
        int ticketsIncumplenResolucion = 0;

        for (Ticket ticket : todosTickets) {
            if (ticket.getFechaResolucion() != null) {
                totalTickets++;
                SlaResultado resultado = evaluarSla(ticket);
                if (resultado.cumplePrimeraRespuesta) {
                    ticketsCumplenPrimeraRespuesta++;
                } else {
                    ticketsIncumplenPrimeraRespuesta++;
                }
                if (resultado.cumpleResolucion) {
                    ticketsCumplenResolucion++;
                } else {
                    ticketsIncumplenResolucion++;
                }
                if (resultado.cumpleGlobal) {
                    ticketsCumplenSLA++;
                } else {
                    ticketsIncumplenSLA++;
                }
            }
        }

        reporte.put("totalTickets", totalTickets);
        reporte.put("ticketsCumplenSLA", ticketsCumplenSLA);
        reporte.put("ticketsIncumplenSLA", ticketsIncumplenSLA);
        reporte.put("ticketsCumplenPrimeraRespuesta", ticketsCumplenPrimeraRespuesta);
        reporte.put("ticketsIncumplenPrimeraRespuesta", ticketsIncumplenPrimeraRespuesta);
        reporte.put("ticketsCumplenResolucion", ticketsCumplenResolucion);
        reporte.put("ticketsIncumplenResolucion", ticketsIncumplenResolucion);
        
        if (totalTickets > 0) {
            double porcentajeCumplimiento = (ticketsCumplenSLA * 100.0) / totalTickets;
            reporte.put("porcentajeCumplimiento", String.format("%.2f", porcentajeCumplimiento));
        } else {
            reporte.put("porcentajeCumplimiento", "0.00");
        }

        return reporte;
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
        reporte.put("totalTickets", tickets.size());
        reporte.put("ticketsAbiertos", ticketRepository.findByEstado(EstadoTicket.ABIERTO).size());
        reporte.put("ticketsEnProceso", ticketRepository.findByEstado(EstadoTicket.EN_PROCESO).size());
        reporte.put("ticketsEnEspera", ticketRepository.findByEstado(EstadoTicket.EN_ESPERA).size());
        reporte.put("ticketsResueltos", ticketRepository.findByEstado(EstadoTicket.RESUELTO).size());
        reporte.put("ticketsCerrados", ticketRepository.findByEstado(EstadoTicket.CERRADO).size());
        reporte.put("ticketsCancelados", ticketRepository.findByEstado(EstadoTicket.CANCELADO).size());
        long resueltosTotal = ticketRepository.findByEstado(EstadoTicket.RESUELTO).size()
                + ticketRepository.findByEstado(EstadoTicket.CERRADO).size();
        long noResueltos = ticketRepository.findByEstado(EstadoTicket.ABIERTO).size()
                + ticketRepository.findByEstado(EstadoTicket.EN_PROCESO).size()
                + ticketRepository.findByEstado(EstadoTicket.EN_ESPERA).size();
        reporte.put("ticketsResueltosTotal", resueltosTotal);
        reporte.put("ticketsNoResueltos", noResueltos);
        reporte.put("topCategorias", obtenerTopCategorias(tickets));

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
        reporte.put("ticketsAbiertos", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.ABIERTO).count());
        reporte.put("ticketsEnProceso", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_PROCESO).count());
        reporte.put("ticketsEnEspera", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_ESPERA).count());
        reporte.put("ticketsResueltos", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.RESUELTO).count());
        reporte.put("ticketsCerrados", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CERRADO).count());
        reporte.put("ticketsCancelados", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CANCELADO).count());
        long resueltosTotal = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                .count();
        long noResueltos = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.ABIERTO
                        || t.getEstado() == EstadoTicket.EN_PROCESO
                        || t.getEstado() == EstadoTicket.EN_ESPERA)
                .count();
        reporte.put("ticketsResueltosTotal", resueltosTotal);
        reporte.put("ticketsNoResueltos", noResueltos);
        reporte.put("topCategorias", obtenerTopCategorias(tickets));

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

    // Reporte SLA por periodo
    public Map<String, Object> generarReporteSLAPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        Map<String, Object> reporte = new HashMap<>();
        List<Ticket> todosTickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null &&
                        !t.getFechaCreacion().isBefore(desde) && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        int totalTickets = todosTickets.size();
        int ticketsCumplenSLA = 0;
        int ticketsIncumplenSLA = 0;
        int ticketsCumplenPrimeraRespuesta = 0;
        int ticketsIncumplenPrimeraRespuesta = 0;
        int ticketsCumplenResolucion = 0;
        int ticketsIncumplenResolucion = 0;

        for (Ticket ticket : todosTickets) {
            if (ticket.getFechaResolucion() != null) {
                totalTickets++;
                SlaResultado resultado = evaluarSla(ticket);
                if (resultado.cumplePrimeraRespuesta) {
                    ticketsCumplenPrimeraRespuesta++;
                } else {
                    ticketsIncumplenPrimeraRespuesta++;
                }
                if (resultado.cumpleResolucion) {
                    ticketsCumplenResolucion++;
                } else {
                    ticketsIncumplenResolucion++;
                }
                if (resultado.cumpleGlobal) {
                    ticketsCumplenSLA++;
                } else {
                    ticketsIncumplenSLA++;
                }
            }
        }

        reporte.put("totalTickets", totalTickets);
        reporte.put("ticketsCumplenSLA", ticketsCumplenSLA);
        reporte.put("ticketsIncumplenSLA", ticketsIncumplenSLA);
        reporte.put("ticketsCumplenPrimeraRespuesta", ticketsCumplenPrimeraRespuesta);
        reporte.put("ticketsIncumplenPrimeraRespuesta", ticketsIncumplenPrimeraRespuesta);
        reporte.put("ticketsCumplenResolucion", ticketsCumplenResolucion);
        reporte.put("ticketsIncumplenResolucion", ticketsIncumplenResolucion);

        if (totalTickets > 0) {
            double porcentajeCumplimiento = (ticketsCumplenSLA * 100.0) / totalTickets;
            reporte.put("porcentajeCumplimiento", String.format("%.2f", porcentajeCumplimiento));
        } else {
            reporte.put("porcentajeCumplimiento", "0.00");
        }

        return reporte;
    }

    private SlaResultado evaluarSla(Ticket ticket) {
        if (ticket.getSlaPolitica() == null) {
            return new SlaResultado(false, false, false);
        }

        Integer tiempoPrimeraRespuestaSeg = ticket.getTiempoPrimeraRespuestaSeg();
        Integer tiempoResolucionSeg = ticket.getTiempoResolucionSeg();
        int slaPrimeraRespuestaSeg = ticket.getSlaPolitica().getSlaPrimeraRespuestaMin() * 60;
        int slaResolucionSeg = ticket.getSlaPolitica().getSlaResolucionMin() * 60;
        int tiempoEsperaSeg = ticket.getTiempoEsperaSeg() != null ? ticket.getTiempoEsperaSeg() : 0;

        boolean cumplePrimeraRespuesta = ticket.getFechaPrimeraRespuesta() != null
                && tiempoPrimeraRespuestaSeg != null
                && tiempoPrimeraRespuestaSeg <= slaPrimeraRespuestaSeg;
        boolean cumpleResolucion = ticket.getFechaResolucion() != null
                && tiempoResolucionSeg != null
                && Math.max(0, tiempoResolucionSeg - tiempoEsperaSeg) <= slaResolucionSeg;

        return new SlaResultado(cumplePrimeraRespuesta, cumpleResolucion,
                cumplePrimeraRespuesta && cumpleResolucion);
    }

    private List<Map<String, Object>> obtenerTopCategorias(List<Ticket> tickets) {
        return tickets.stream()
                .filter(t -> t.getCategoria() != null)
                .collect(Collectors.groupingBy(t -> t.getCategoria().getNombre(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("nombre", entry.getKey());
                    item.put("total", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private static class SlaResultado {
        private final boolean cumplePrimeraRespuesta;
        private final boolean cumpleResolucion;
        private final boolean cumpleGlobal;

        private SlaResultado(boolean cumplePrimeraRespuesta, boolean cumpleResolucion, boolean cumpleGlobal) {
            this.cumplePrimeraRespuesta = cumplePrimeraRespuesta;
            this.cumpleResolucion = cumpleResolucion;
            this.cumpleGlobal = cumpleGlobal;
        }
    }
}
