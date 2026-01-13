package com.example.demo.DTO;

public class UbicacionDTO {
    private String edificio;
    private String piso;
    private String salon;

    // Constructores
    public UbicacionDTO() {}

    // Getters y Setters
    public String getEdificio() {
        return edificio;
    }

    public void setEdificio(String edificio) {
        this.edificio = edificio;
    }

    public String getPiso() {
        return piso;
    }

    public void setPiso(String piso) {
        this.piso = piso;
    }

    public String getSalon() {
        return salon;
    }

    public void setSalon(String salon) {
        this.salon = salon;
    }
}
