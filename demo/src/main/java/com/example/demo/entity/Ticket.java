package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTicket estado = EstadoTicket.ABIERTO;

    @Enumerated(EnumType.STRING)
    private Prioridad prioridad = Prioridad.MEDIA;

    @ManyToOne
    @JoinColumn(name = "creado_por_id", nullable = false)
    private Usuario creadoPor;

    @ManyToOne
    @JoinColumn(name = "asignado_a_id")
    private Usuario asignadoA;

    @ManyToOne
    @JoinColumn(name = "sla_politica_id", nullable = false)
    private SlaPolitica slaPolitica;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "ubicacion_id")
    private Ubicacion ubicacion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "fecha_primera_respuesta")
    private LocalDateTime fechaPrimeraRespuesta;

    @Column(name = "evidencia_problema")
    private String evidenciaProblema; // ruta del archivo

    @Column(name = "evidencia_resolucion")
    private String evidenciaResolucion;

    @Column(name = "tiempo_primera_respuesta_seg")
    private Integer tiempoPrimeraRespuestaSeg;

    @Column(name = "tiempo_resolucion_seg")
    private Integer tiempoResolucionSeg;

    @Column(name = "tiempo_espera_seg", nullable = false)
    private Integer tiempoEsperaSeg = 0;

    @Column(name = "espera_desde")
    private LocalDateTime esperaDesde;

    @Column(name = "reabierto_count", nullable = false)
    private Integer reabiertoCount = 0;


    // Constructores
    public Ticket() {}

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoTicket getEstado() {
        return estado;
    }

    public void setEstado(EstadoTicket estado) {
        this.estado = estado;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public Usuario getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Usuario creadoPor) {
        this.creadoPor = creadoPor;
    }

    public Usuario getAsignadoA() {
        return asignadoA;
    }

    public void setAsignadoA(Usuario asignadoA) {
        this.asignadoA = asignadoA;
    }

    public SlaPolitica getSlaPolitica() {
        return slaPolitica;
    }

    public void setSlaPolitica(SlaPolitica slaPolitica) {
        this.slaPolitica = slaPolitica;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public LocalDateTime getFechaPrimeraRespuesta() {
        return fechaPrimeraRespuesta;
    }

    public void setFechaPrimeraRespuesta(LocalDateTime fechaPrimeraRespuesta) {
        this.fechaPrimeraRespuesta = fechaPrimeraRespuesta;
    }

    public String getEvidenciaProblema() {
        return evidenciaProblema;
    }

    public void setEvidenciaProblema(String evidenciaProblema) {
        this.evidenciaProblema = evidenciaProblema;
    }

    public String getEvidenciaResolucion() {
        return evidenciaResolucion;
    }

    public void setEvidenciaResolucion(String evidenciaResolucion) {
        this.evidenciaResolucion = evidenciaResolucion;
    }

    public Integer getTiempoPrimeraRespuestaSeg() {
        return tiempoPrimeraRespuestaSeg;
    }

    public void setTiempoPrimeraRespuestaSeg(Integer tiempoPrimeraRespuestaSeg) {
        this.tiempoPrimeraRespuestaSeg = tiempoPrimeraRespuestaSeg;
    }

    public Integer getTiempoResolucionSeg() {
        return tiempoResolucionSeg;
    }

    public void setTiempoResolucionSeg(Integer tiempoResolucionSeg) {
        this.tiempoResolucionSeg = tiempoResolucionSeg;
    }

    public Integer getTiempoEsperaSeg() {
        return tiempoEsperaSeg;
    }

    public void setTiempoEsperaSeg(Integer tiempoEsperaSeg) {
        this.tiempoEsperaSeg = tiempoEsperaSeg;
    }

    public LocalDateTime getEsperaDesde() {
        return esperaDesde;
    }

    public void setEsperaDesde(LocalDateTime esperaDesde) {
        this.esperaDesde = esperaDesde;
    }

    public Integer getReabiertoCount() {
        return reabiertoCount;
    }

    public void setReabiertoCount(Integer reabiertoCount) {
        this.reabiertoCount = reabiertoCount;
    }

}
