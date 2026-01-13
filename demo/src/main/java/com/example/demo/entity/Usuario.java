package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String correo;
    private String password;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private String boleta;

    @Column(name = "id_trabajador")
    private String idTrabajador;

    private Boolean activo = true;

    // getters y setters

    public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}

public String getNombre() {
    return nombre;
}

public void setNombre(String nombre) {
    this.nombre = nombre;
}

public String getCorreo() {
    return correo;
}

public void setCorreo(String correo) {
    this.correo = correo;
}

public String getPassword() {
    return password;
}

public void setPassword(String password) {
    this.password = password;
}

public Rol getRol() {
    return rol;
}

public void setRol(Rol rol) {
    this.rol = rol;
}

public String getBoleta() {
    return boleta;
}

public void setBoleta(String boleta) {
    this.boleta = boleta;
}

public String getIdTrabajador() {
    return idTrabajador;
}

public void setIdTrabajador(String idTrabajador) {
    this.idTrabajador = idTrabajador;
}

public Boolean getActivo() {
    return activo;
}

public void setActivo(Boolean activo) {
    this.activo = activo;
}

}
