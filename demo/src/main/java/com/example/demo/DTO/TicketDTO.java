package com.example.demo.DTO;

public class TicketDTO {
    private String titulo;
    private String descripcion;
    private Long categoriaId;
    private Long ubicacionId;
    private String prioridad;
    private String evidenciaProblema;

    // Constructores
    public TicketDTO() {}

    // Getters y Setters
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

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getUbicacionId() {
        return ubicacionId;
    }

    public void setUbicacionId(Long ubicacionId) {
        this.ubicacionId = ubicacionId;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public String getEvidenciaProblema() {
        return evidenciaProblema;
    }

    public void setEvidenciaProblema(String evidenciaProblema) {
        this.evidenciaProblema = evidenciaProblema;
    }
}
