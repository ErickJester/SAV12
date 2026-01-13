package com.example.demo.DTO;

public class RegistroDTO {

    private String nombre;
    private String correo;
    private String password;
    private String password2;
    private String rol;

    // Campos condicionales
    private String boleta;         // Usuario
    private String idTrabajador;   // Admin / Técnico

    // ===== Getters & Setters =====

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

    public String getPassword2() { 
        return password2; 
    }
    public void setPassword2(String password2) { 
        this.password2 = password2; 
    }

    public String getRol() { 
        return rol; 
    }
    public void setRol(String rol) { 
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
}
