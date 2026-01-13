package com.example.demo.repository;

import com.example.demo.entity.HistorialAccion;
import com.example.demo.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialAccionRepository extends JpaRepository<HistorialAccion, Long> {
    List<HistorialAccion> findByTicketOrderByFechaAccionDesc(Ticket ticket);
    List<HistorialAccion> findByTicket(Ticket ticket);
}
