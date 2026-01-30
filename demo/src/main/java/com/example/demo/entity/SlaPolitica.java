package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sla_politicas")
public class SlaPolitica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rol_solicitante", nullable = false)
    private String rolSolicitante;

    @Column(name = "sla_primera_respuesta_min", nullable = false)
    private Integer slaPrimeraRespuestaMin;

    @Column(name = "sla_resolucion_min", nullable = false)
    private Integer slaResolucionMin;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    public SlaPolitica() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRolSolicitante() {
        return rolSolicitante;
    }

    public void setRolSolicitante(String rolSolicitante) {
        this.rolSolicitante = rolSolicitante;
    }

    public Integer getSlaPrimeraRespuestaMin() {
        return slaPrimeraRespuestaMin;
    }

    public void setSlaPrimeraRespuestaMin(Integer slaPrimeraRespuestaMin) {
        this.slaPrimeraRespuestaMin = slaPrimeraRespuestaMin;
    }

    public Integer getSlaResolucionMin() {
        return slaResolucionMin;
    }

    public void setSlaResolucionMin(Integer slaResolucionMin) {
        this.slaResolucionMin = slaResolucionMin;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
