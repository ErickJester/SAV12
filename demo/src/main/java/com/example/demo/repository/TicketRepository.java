package com.example.demo.repository;

import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.entity.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCreadoPor(Usuario usuario);
    List<Ticket> findByAsignadoA(Usuario tecnico);
    List<Ticket> findByEstado(EstadoTicket estado);
    List<Ticket> findByCreadoPorOrderByFechaCreacionDesc(Usuario usuario);
    List<Ticket> findByAsignadoAOrderByFechaCreacionDesc(Usuario tecnico);
    List<Ticket> findAllByOrderByFechaCreacionDesc();
}
