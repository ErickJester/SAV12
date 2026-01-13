package com.example.demo.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReporteDTO {

    private String titulo;
    private String descripcion;
    private String prioridad;

    private String boleta;
    private String salon;
    private LocalDate fecha;
    private LocalTime hora;

    // Getters y setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getBoleta() { return boleta; }
    public void setBoleta(String boleta) { this.boleta = boleta; }

    public String getSalon() { return salon; }
    public void setSalon(String salon) { this.salon = salon; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
}
