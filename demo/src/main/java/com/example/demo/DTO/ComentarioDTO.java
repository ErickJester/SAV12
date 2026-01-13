package com.example.demo.DTO;

public class ComentarioDTO {
    private Long ticketId;
    private String contenido;

    // Constructores
    public ComentarioDTO() {}

    // Getters y Setters
    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
