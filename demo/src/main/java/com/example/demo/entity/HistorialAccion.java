package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_acciones")
public class HistorialAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String accion; // "Ticket creado", "Estado cambiado a EN_PROCESO", etc.

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "estado_anterior")
    @Enumerated(EnumType.STRING)
    private EstadoTicket estadoAnterior;

    @Column(name = "estado_nuevo")
    @Enumerated(EnumType.STRING)
    private EstadoTicket estadoNuevo;

    @ManyToOne
    @JoinColumn(name = "asignado_anterior_id")
    private Usuario asignadoAnterior;

    @ManyToOne
    @JoinColumn(name = "asignado_nuevo_id")
    private Usuario asignadoNuevo;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String detalles;

    // Constructores
    public HistorialAccion() {}

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public EstadoTicket getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoTicket estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoTicket getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoTicket estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public Usuario getAsignadoAnterior() {
        return asignadoAnterior;
    }

    public void setAsignadoAnterior(Usuario asignadoAnterior) {
        this.asignadoAnterior = asignadoAnterior;
    }

    public Usuario getAsignadoNuevo() {
        return asignadoNuevo;
    }

    public void setAsignadoNuevo(Usuario asignadoNuevo) {
        this.asignadoNuevo = asignadoNuevo;
    }

    public LocalDateTime getFechaAccion() {
        return fechaAccion;
    }

    public void setFechaAccion(LocalDateTime fechaAccion) {
        this.fechaAccion = fechaAccion;
    }

    public String getDetalles() {
        return detalles;
    }

    public void setDetalles(String detalles) {
        this.detalles = detalles;
    }
}
