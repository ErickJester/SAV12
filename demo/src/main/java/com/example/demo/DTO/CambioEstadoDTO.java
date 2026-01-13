package com.example.demo.DTO;

public class CambioEstadoDTO {
    private Long ticketId;
    private String nuevoEstado;
    private String observaciones;

    // Constructores
    public CambioEstadoDTO() {}

    // Getters y Setters
    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(String nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
