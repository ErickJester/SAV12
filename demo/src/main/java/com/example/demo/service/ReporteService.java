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
        int totalTickets = todosTickets.size();
        int ticketsCumplenSLA = 0;
        int ticketsIncumplenSLA = 0;

        for (Ticket ticket : todosTickets) {
            if (ticket.getFechaResolucion() != null) {
                if (cumpleSla(ticket)) {
                    ticketsCumplenSLA++;
                } else {
                    ticketsIncumplenSLA++;
                }
            }
        }

        reporte.put("totalTickets", totalTickets);
        reporte.put("ticketsCumplenSLA", ticketsCumplenSLA);
        reporte.put("ticketsIncumplenSLA", ticketsIncumplenSLA);
        
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
        reporte.put("ticketsResueltos", ticketRepository.findByEstado(EstadoTicket.RESUELTO).size());
        reporte.put("ticketsCerrados", ticketRepository.findByEstado(EstadoTicket.CERRADO).size());
        reporte.put("ticketsReabiertos", ticketRepository.findByEstado(EstadoTicket.REABIERTO).size());

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
        reporte.put("ticketsResueltos", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.RESUELTO).count());
        reporte.put("ticketsCerrados", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CERRADO).count());
        reporte.put("ticketsReabiertos", tickets.stream().filter(t -> t.getEstado() == EstadoTicket.REABIERTO).count());

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

        for (Ticket ticket : todosTickets) {
            if (ticket.getFechaResolucion() != null) {
                if (cumpleSla(ticket)) {
                    ticketsCumplenSLA++;
                } else {
                    ticketsIncumplenSLA++;
                }
            }
        }

        reporte.put("totalTickets", totalTickets);
        reporte.put("ticketsCumplenSLA", ticketsCumplenSLA);
        reporte.put("ticketsIncumplenSLA", ticketsIncumplenSLA);

        if (totalTickets > 0) {
            double porcentajeCumplimiento = (ticketsCumplenSLA * 100.0) / totalTickets;
            reporte.put("porcentajeCumplimiento", String.format("%.2f", porcentajeCumplimiento));
        } else {
            reporte.put("porcentajeCumplimiento", "0.00");
        }

        return reporte;
    }

    private boolean cumpleSla(Ticket ticket) {
        if (ticket.getSlaPolitica() == null) {
            return false;
        }

        Integer tiempoPrimeraRespuestaSeg = ticket.getTiempoPrimeraRespuestaSeg();
        Integer tiempoResolucionSeg = ticket.getTiempoResolucionSeg();
        if (tiempoPrimeraRespuestaSeg == null || tiempoResolucionSeg == null) {
            return false;
        }

        int slaPrimeraRespuestaSeg = ticket.getSlaPolitica().getSlaPrimeraRespuestaMin() * 60;
        int slaResolucionSeg = ticket.getSlaPolitica().getSlaResolucionMin() * 60;
        int tiempoEsperaSeg = ticket.getTiempoEsperaSeg() != null ? ticket.getTiempoEsperaSeg() : 0;
        int resolucionEfectivaSeg = Math.max(0, tiempoResolucionSeg - tiempoEsperaSeg);

        boolean cumplePrimeraRespuesta = tiempoPrimeraRespuestaSeg <= slaPrimeraRespuestaSeg;
        boolean cumpleResolucion = resolucionEfectivaSeg <= slaResolucionSeg;

        return cumplePrimeraRespuesta && cumpleResolucion;
    }
}
