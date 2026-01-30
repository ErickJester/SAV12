package com.example.demo.repository;

import com.example.demo.entity.SlaPolitica;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaPoliticaRepository extends JpaRepository<SlaPolitica, Long> {
    Optional<SlaPolitica> findByNombre(String nombre);
}

package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.SlaPolitica;

public interface SlaPoliticaRepository extends JpaRepository<SlaPolitica, Long> {

    Optional<SlaPolitica> findFirstByRolSolicitanteAndActivoTrue(String rolSolicitante);
}
