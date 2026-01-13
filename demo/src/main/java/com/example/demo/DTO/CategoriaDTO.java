package com.example.demo.DTO;

public class CategoriaDTO {
    private String nombre;
    private String descripcion;

    // Constructores
    public CategoriaDTO() {}

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
