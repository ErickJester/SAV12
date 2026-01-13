package com.example.demo.repository;

import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.entity.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUsuario(Usuario usuario);
    List<Ticket> findByTecnico(Usuario tecnico);
    List<Ticket> findByEstado(EstadoTicket estado);
    List<Ticket> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);
    List<Ticket> findByTecnicoOrderByFechaCreacionDesc(Usuario tecnico);
    List<Ticket> findAllByOrderByFechaCreacionDesc();
}
