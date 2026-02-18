package com.example.demo.service;

import com.example.demo.entity.EstadoTicket;
import com.example.demo.entity.Prioridad;
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
                if (r.cumpleGlobal)           cumpleGlobal++;  else incumpleGlobal++;
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

        // ---- campos que template/js puede usar directo ----
        reporte.put("slaPrimeraRespuestaCumplen", cumplePrimera);
        reporte.put("slaPrimeraRespuestaIncumplen", incumplePrimera);
        reporte.put("slaPrimeraRespuestaPorcentaje", pct(cumplePrimera, total));

        reporte.put("slaResolucionCumplen", cumpleResol);
        reporte.put("slaResolucionIncumplen", incumpleResol);
        reporte.put("slaResolucionPorcentaje", pct(cumpleResol, total));

        reporte.put("slaCumplen", cumpleGlobal);
        reporte.put("slaIncumplen", incumpleGlobal);
        reporte.put("slaPorcentaje", pct(cumpleGlobal, total));

        // alias
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
        // Devuelve SIEMPRE todos los estados (con ceros)
        Map<String, Long> base = new LinkedHashMap<>();
        for (EstadoTicket e : EstadoTicket.values()) base.put(e.name(), 0L);

        Map<String, Long> conteo = ticketRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(t -> t.getEstado().name(), Collectors.counting()));

        conteo.forEach(base::put);
        return base;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> generarReportePorEstadoPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        // Devuelve SIEMPRE todos los estados (con ceros)
        Map<String, Long> base = new LinkedHashMap<>();
        for (EstadoTicket e : EstadoTicket.values()) base.put(e.name(), 0L);

        Map<String, Long> conteo = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.groupingBy(t -> t.getEstado().name(), Collectors.counting()));

        conteo.forEach(base::put);
        return base;
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

        // ✅ FIX: abiertos NO incluye REABIERTO (si no, doble conteo)
        long abiertos = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.ABIERTO)
                .count();

        long reabiertos = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.REABIERTO)
                .count();

        long enProceso = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_PROCESO).count();
        long enEspera  = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.EN_ESPERA).count();
        long resueltos = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.RESUELTO).count();
        long cerrados  = tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CERRADO).count();
        long cancelados= tickets.stream().filter(t -> t.getEstado() == EstadoTicket.CANCELADO).count();

        reporte.put("ticketsAbiertos", abiertos);
        reporte.put("ticketsReabiertos", reabiertos);
        reporte.put("ticketsEnProceso", enProceso);
        reporte.put("ticketsEnEspera", enEspera);
        reporte.put("ticketsResueltos", resueltos);
        reporte.put("ticketsCerrados", cerrados);
        reporte.put("ticketsCancelados", cancelados);

        long resueltosTotal = resueltos + cerrados;
        long noResueltos = abiertos + reabiertos + enProceso + enEspera;

        reporte.put("ticketsResueltosTotal", resueltosTotal);
        reporte.put("ticketsNoResueltos", noResueltos);

        // top categorías también lo expones aquí (por si lo consume otra vista)
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
    // ===== ANÁLISIS DE TIEMPOS =====
    // =========================

    @Transactional(readOnly = true)
    public Map<String, Object> generarAnalisisTiempos(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> ticketsResueltos = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaResolucion() != null
                        && t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        Map<String, Object> analisis = new HashMap<>();

        if (ticketsResueltos.isEmpty()) {
            analisis.put("tiempoPromedioRespuestaMin", 0.0);
            analisis.put("tiempoPromedioResolucionMin", 0.0);
            analisis.put("tiempoPromedioEsperaMin", 0.0);
            analisis.put("ticketsAnalizados", 0);
            return analisis;
        }

        // Tiempo promedio primera respuesta
        double promedioRespuesta = ticketsResueltos.stream()
                .filter(t -> t.getTiempoPrimeraRespuestaSeg() != null)
                .mapToInt(Ticket::getTiempoPrimeraRespuestaSeg)
                .average()
                .orElse(0.0) / 60.0;

        // Tiempo promedio resolución (neto, sin espera)
        double promedioResolucion = ticketsResueltos.stream()
                .filter(t -> t.getTiempoResolucionSeg() != null)
                .mapToInt(t -> Math.max(0, t.getTiempoResolucionSeg() - (t.getTiempoEsperaSeg() != null ? t.getTiempoEsperaSeg() : 0)))
                .average()
                .orElse(0.0) / 60.0;

        // Tiempo promedio en espera
        double promedioEspera = ticketsResueltos.stream()
                .filter(t -> t.getTiempoEsperaSeg() != null && t.getTiempoEsperaSeg() > 0)
                .mapToInt(Ticket::getTiempoEsperaSeg)
                .average()
                .orElse(0.0) / 60.0;

        analisis.put("tiempoPromedioRespuestaMin", Math.round(promedioRespuesta * 100.0) / 100.0);
        analisis.put("tiempoPromedioResolucionMin", Math.round(promedioResolucion * 100.0) / 100.0);
        analisis.put("tiempoPromedioEsperaMin", Math.round(promedioEspera * 100.0) / 100.0);
        analisis.put("ticketsAnalizados", ticketsResueltos.size());

        return analisis;
    }

    // =========================
    // ===== DESEMPEÑO TÉCNICOS =====
    // =========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> generarDesempenoTecnicos(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getAsignadoA() != null
                        && t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        Map<String, List<Ticket>> ticketsPorTecnico = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getAsignadoA().getNombre()));

        return ticketsPorTecnico.entrySet().stream()
                .map(entry -> {
                    String tecnico = entry.getKey();
                    List<Ticket> ticketsTecnico = entry.getValue();

                    int totalAsignados = ticketsTecnico.size();
                    int resueltos = (int) ticketsTecnico.stream()
                            .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                            .count();
                    int enProceso = (int) ticketsTecnico.stream()
                            .filter(t -> t.getEstado() == EstadoTicket.EN_PROCESO)
                            .count();
                    int reabiertos = (int) ticketsTecnico.stream()
                            .filter(t -> t.getReabiertoCount() != null && t.getReabiertoCount() > 0)
                            .count();

                    // SLA
                    int cumpleSLA = 0;
                    int incumpleSLA = 0;
                    
                    for (Ticket t : ticketsTecnico) {
                        if (t.getFechaResolucion() != null && t.getSlaPolitica() != null) {
                            SlaResultado sla = evaluarSla(t);
                            if (sla.cumpleGlobal) cumpleSLA++;
                            else incumpleSLA++;
                        }
                    }

                    double tasaExito = totalAsignados > 0 ? (resueltos * 100.0 / totalAsignados) : 0.0;
                    double tasaReapertura = resueltos > 0 ? (reabiertos * 100.0 / resueltos) : 0.0;
                    double cumplimientoSLA = (cumpleSLA + incumpleSLA) > 0 ? (cumpleSLA * 100.0 / (cumpleSLA + incumpleSLA)) : 0.0;

                    // Tiempos promedio
                    double tiempoPromedioResolucion = ticketsTecnico.stream()
                            .filter(t -> t.getTiempoResolucionSeg() != null && t.getFechaResolucion() != null)
                            .mapToInt(t -> Math.max(0, t.getTiempoResolucionSeg() - (t.getTiempoEsperaSeg() != null ? t.getTiempoEsperaSeg() : 0)))
                            .average()
                            .orElse(0.0) / 60.0;

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("tecnico", tecnico);
                    stats.put("totalAsignados", totalAsignados);
                    stats.put("resueltos", resueltos);
                    stats.put("enProceso", enProceso);
                    stats.put("reabiertos", reabiertos);
                    stats.put("tasaExito", Math.round(tasaExito * 100.0) / 100.0);
                    stats.put("tasaReapertura", Math.round(tasaReapertura * 100.0) / 100.0);
                    stats.put("cumplimientoSLA", Math.round(cumplimientoSLA * 100.0) / 100.0);
                    stats.put("tiempoPromedioResolucionMin", Math.round(tiempoPromedioResolucion * 100.0) / 100.0);

                    return stats;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("resueltos"), (Integer) a.get("resueltos")))
                .collect(Collectors.toList());
    }

    // =========================
    // ===== KPIs EJECUTIVOS =====
    // =========================

    @Transactional(readOnly = true)
    public Map<String, Object> generarKPIsEjecutivos(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        Map<String, Object> kpis = new HashMap<>();

        // Totales básicos
        int total = tickets.size();
        long resueltos = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                .count();
        long pendientes = tickets.stream()
                .filter(t -> t.getEstado() == EstadoTicket.ABIERTO 
                        || t.getEstado() == EstadoTicket.REABIERTO
                        || t.getEstado() == EstadoTicket.EN_PROCESO
                        || t.getEstado() == EstadoTicket.EN_ESPERA)
                .count();
        long sinAsignar = tickets.stream()
                .filter(t -> t.getAsignadoA() == null && t.getEstado() != EstadoTicket.CERRADO && t.getEstado() != EstadoTicket.CANCELADO)
                .count();

        // Tasa de resolución
        double tasaResolucion = total > 0 ? (resueltos * 100.0 / total) : 0.0;

        // Tickets críticos (alta prioridad sin resolver)
        long ticketsCriticos = tickets.stream()
                .filter(t -> t.getPrioridad() == Prioridad.ALTA 
                        && t.getEstado() != EstadoTicket.RESUELTO 
                        && t.getEstado() != EstadoTicket.CERRADO)
                .count();

        // Tiempo promedio total de resolución
        double tiempoPromedioTotal = tickets.stream()
                .filter(t -> t.getTiempoResolucionSeg() != null && t.getFechaResolucion() != null)
                .mapToInt(Ticket::getTiempoResolucionSeg)
                .average()
                .orElse(0.0) / 60.0;

        // SLA global simple
        Map<String, Object> slaData = construirReporteSla(tickets);
        String slaGlobal = (String) slaData.get("slaPorcentaje");

        kpis.put("totalTickets", total);
        kpis.put("ticketsResueltos", resueltos);
        kpis.put("ticketsPendientes", pendientes);
        kpis.put("ticketsSinAsignar", sinAsignar);
        kpis.put("ticketsCriticos", ticketsCriticos);
        kpis.put("tasaResolucion", Math.round(tasaResolucion * 100.0) / 100.0);
        kpis.put("tiempoPromedioTotalMin", Math.round(tiempoPromedioTotal * 100.0) / 100.0);
        kpis.put("slaGlobalPorcentaje", slaGlobal);

        return kpis;
    }

    // =========================
    // ===== ANÁLISIS POR PRIORIDAD =====
    // =========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> generarAnalisisPorPrioridad(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        Map<Prioridad, List<Ticket>> porPrioridad = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getPrioridad));

        return porPrioridad.entrySet().stream()
                .map(entry -> {
                    Prioridad prioridad = entry.getKey();
                    List<Ticket> ticketsPrioridad = entry.getValue();

                    int total = ticketsPrioridad.size();
                    int resueltos = (int) ticketsPrioridad.stream()
                            .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                            .count();
                    int pendientes = total - resueltos;

                    double tiempoPromedio = ticketsPrioridad.stream()
                            .filter(t -> t.getTiempoResolucionSeg() != null && t.getFechaResolucion() != null)
                            .mapToInt(t -> Math.max(0, t.getTiempoResolucionSeg() - (t.getTiempoEsperaSeg() != null ? t.getTiempoEsperaSeg() : 0)))
                            .average()
                            .orElse(0.0) / 60.0;

                    Map<String, Object> datos = new HashMap<>();
                    datos.put("prioridad", prioridad.name());
                    datos.put("total", total);
                    datos.put("resueltos", resueltos);
                    datos.put("pendientes", pendientes);
                    datos.put("tiempoPromedioMin", Math.round(tiempoPromedio * 100.0) / 100.0);

                    return datos;
                })
                .sorted((a, b) -> {
                    // Ordenar: ALTA > MEDIA > BAJA
                    String prioA = (String) a.get("prioridad");
                    String prioB = (String) b.get("prioridad");
                    if (prioA.equals("ALTA")) return -1;
                    if (prioB.equals("ALTA")) return 1;
                    if (prioA.equals("MEDIA")) return -1;
                    if (prioB.equals("MEDIA")) return 1;
                    return 0;
                })
                .collect(Collectors.toList());
    }

// =========================
// ===== ANÁLISIS POR UBICACIONES =====
// =========================

@Transactional(readOnly = true)
public List<Map<String, Object>> generarAnalisisPorUbicaciones(LocalDateTime desde, LocalDateTime hasta) {
    List<Ticket> tickets = ticketRepository.findAll()
            .stream()
            .filter(t -> t.getFechaCreacion() != null
                    && !t.getFechaCreacion().isBefore(desde)
                    && !t.getFechaCreacion().isAfter(hasta))
            .collect(Collectors.toList());

    // ✅ SOLUCIÓN: Crear mapa manualmente
    Map<String, List<Ticket>> porUbicacion = new HashMap<>();
    
    for (Ticket t : tickets) {
        String ubicacionNombre;
        if (t.getUbicacion() != null) {
            // ✅ Construir nombre descriptivo: "Edificio A - Piso 2 - Salón 201"
            StringBuilder sb = new StringBuilder();
            
            if (t.getUbicacion().getEdificio() != null) {
                sb.append(t.getUbicacion().getEdificio());
            }
            
            if (t.getUbicacion().getPiso() != null) {
                if (sb.length() > 0) sb.append(" - ");
                sb.append("Piso ").append(t.getUbicacion().getPiso());
            }
            
            if (t.getUbicacion().getSalon() != null) {
                if (sb.length() > 0) sb.append(" - ");
                sb.append("Salón ").append(t.getUbicacion().getSalon());
            }
            
            ubicacionNombre = sb.length() > 0 ? sb.toString() : "Sin ubicación";
        } else {
            ubicacionNombre = "Sin ubicación";
        }
        
        porUbicacion.computeIfAbsent(ubicacionNombre, k -> new ArrayList<>()).add(t);
    }

    return porUbicacion.entrySet().stream()
            .map(entry -> {
                String ubicacion = entry.getKey();
                List<Ticket> ticketsUbicacion = entry.getValue();

                int total = ticketsUbicacion.size();
                int resueltos = (int) ticketsUbicacion.stream()
                        .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                        .count();
                int pendientes = total - resueltos;

                Map<String, Object> datos = new HashMap<>();
                datos.put("ubicacion", ubicacion);
                datos.put("total", total);
                datos.put("resueltos", resueltos);
                datos.put("pendientes", pendientes);

                return datos;
            })
            .sorted((a, b) -> Integer.compare((Integer) b.get("total"), (Integer) a.get("total")))
            .limit(10)
            .collect(Collectors.toList());
}



    // =========================
    // ===== ALERTAS Y PROBLEMAS =====
    // =========================

    @Transactional(readOnly = true)
    public Map<String, Object> generarAlertas(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        Map<String, Object> alertas = new HashMap<>();

        // Tickets sin asignar
        long sinAsignar = tickets.stream()
                .filter(t -> t.getAsignadoA() == null 
                        && t.getEstado() != EstadoTicket.CERRADO 
                        && t.getEstado() != EstadoTicket.CANCELADO)
                .count();

        // Tickets vencidos (fuera de SLA y no resueltos)
        long ticketsVencidos = tickets.stream()
                .filter(t -> {
                    if (t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO) {
                        return false;
                    }
                    if (t.getSlaPolitica() == null) return false;

                    // Verificar si ha superado el tiempo de respuesta
                    if (t.getFechaPrimeraRespuesta() == null) {
                        long minutosDesdeCreacion = java.time.Duration.between(t.getFechaCreacion(), LocalDateTime.now()).toMinutes();
                        return minutosDesdeCreacion > t.getSlaPolitica().getSlaPrimeraRespuestaMin();
                    }

                    return false;
                })
                .count();

        // Tickets críticos pendientes
        long criticosPendientes = tickets.stream()
                .filter(t -> t.getPrioridad() == Prioridad.ALTA
                        && t.getEstado() != EstadoTicket.RESUELTO
                        && t.getEstado() != EstadoTicket.CERRADO)
                .count();

        // Tickets reabiertos recientemente
        long reabiertosMasDeUnaVez = tickets.stream()
                .filter(t -> t.getReabiertoCount() != null && t.getReabiertoCount() > 1)
                .count();

        // Técnicos con sobrecarga (más de 10 tickets activos)
        Map<String, Long> cargaPorTecnico = tickets.stream()
                .filter(t -> t.getAsignadoA() != null
                        && (t.getEstado() == EstadoTicket.ABIERTO
                        || t.getEstado() == EstadoTicket.REABIERTO
                        || t.getEstado() == EstadoTicket.EN_PROCESO
                        || t.getEstado() == EstadoTicket.EN_ESPERA))
                .collect(Collectors.groupingBy(
                        t -> t.getAsignadoA().getNombre(),
                        Collectors.counting()
                ));

        long tecnicosSobrecargados = cargaPorTecnico.values().stream()
                .filter(carga -> carga > 10)
                .count();

        alertas.put("ticketsSinAsignar", sinAsignar);
        alertas.put("ticketsVencidos", ticketsVencidos);
        alertas.put("ticketsCriticosPendientes", criticosPendientes);
        alertas.put("ticketsReabiertosMuchasVeces", reabiertosMasDeUnaVez);
        alertas.put("tecnicosSobrecargados", tecnicosSobrecargados);
        alertas.put("tieneAlertas", sinAsignar > 0 || ticketsVencidos > 0 || criticosPendientes > 0 || tecnicosSobrecargados > 0);

        return alertas;
    }

    // =========================
    // ===== TOP TICKETS PROBLEMÁTICOS =====
    // =========================

    @Transactional(readOnly = true)
    public Map<String, Object> generarTopTicketsProblematicos(LocalDateTime desde, LocalDateTime hasta) {
        List<Ticket> tickets = ticketRepository.findAll()
                .stream()
                .filter(t -> t.getFechaCreacion() != null
                        && !t.getFechaCreacion().isBefore(desde)
                        && !t.getFechaCreacion().isAfter(hasta))
                .collect(Collectors.toList());

        Map<String, Object> problematicos = new HashMap<>();

        // Top 5 tickets más reabiertos
        List<Map<String, Object>> masReabiertos = tickets.stream()
                .filter(t -> t.getReabiertoCount() != null && t.getReabiertoCount() > 0)
                .sorted((a, b) -> Integer.compare(b.getReabiertoCount(), a.getReabiertoCount()))
                .limit(5)
                .map(t -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", t.getId());
                    item.put("titulo", t.getTitulo());
                    item.put("reabiertos", t.getReabiertoCount());
                    item.put("estado", t.getEstado().name());
                    item.put("tecnico", t.getAsignadoA() != null ? t.getAsignadoA().getNombre() : "Sin asignar");
                    return item;
                })
                .collect(Collectors.toList());

        // Top 5 tickets con mayor tiempo de resolución
        List<Map<String, Object>> mayorTiempo = tickets.stream()
                .filter(t -> t.getTiempoResolucionSeg() != null && t.getFechaResolucion() != null)
                .sorted((a, b) -> Integer.compare(b.getTiempoResolucionSeg(), a.getTiempoResolucionSeg()))
                .limit(5)
                .map(t -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", t.getId());
                    item.put("titulo", t.getTitulo());
                    item.put("tiempoMin", Math.round(t.getTiempoResolucionSeg() / 60.0));
                    item.put("tecnico", t.getAsignadoA() != null ? t.getAsignadoA().getNombre() : "Sin asignar");
                    return item;
                })
                .collect(Collectors.toList());

        // Tickets críticos sin resolver
        List<Map<String, Object>> criticosSinResolver = tickets.stream()
                .filter(t -> t.getPrioridad() == Prioridad.ALTA
                        && t.getEstado() != EstadoTicket.RESUELTO
                        && t.getEstado() != EstadoTicket.CERRADO)
                .limit(5)
                .map(t -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", t.getId());
                    item.put("titulo", t.getTitulo());
                    item.put("estado", t.getEstado().name());
                    item.put("tecnico", t.getAsignadoA() != null ? t.getAsignadoA().getNombre() : "Sin asignar");
                    
                    // Calcular días desde creación
                    long diasDesdeCreacion = java.time.Duration.between(t.getFechaCreacion(), LocalDateTime.now()).toDays();
                    item.put("diasAbierto", diasDesdeCreacion);
                    
                    return item;
                })
                .collect(Collectors.toList());

        problematicos.put("masReabiertos", masReabiertos);
        problematicos.put("mayorTiempo", mayorTiempo);
        problematicos.put("criticosSinResolver", criticosSinResolver);

        return problematicos;
    }

    // =========================
    // ===== SLA CORE ==========
    // =========================

    private SlaResultado evaluarSla(Ticket ticket) {
        if (ticket.getSlaPolitica() == null) {
            return new SlaResultado(false, false, false);
        }

        Integer tPrimera = ticket.getTiempoPrimeraRespuestaSeg();
        Integer tResol   = ticket.getTiempoResolucionSeg();

        int slaPrimeraMin = ticket.getSlaPolitica().getSlaPrimeraRespuestaMin();
        int slaResolMin   = ticket.getSlaPolitica().getSlaResolucionMin();

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
