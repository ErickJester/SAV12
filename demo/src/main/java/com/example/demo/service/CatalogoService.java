package com.example.demo.service;

import com.example.demo.DTO.CategoriaDTO;
import com.example.demo.DTO.UbicacionDTO;
import com.example.demo.entity.Categoria;
import com.example.demo.entity.Ubicacion;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.UbicacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    // Categorías
    public Categoria crearCategoria(CategoriaDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
        return categoriaRepository.save(categoria);
    }

    public List<Categoria> obtenerTodasCategorias() {
        return categoriaRepository.findAll();
    }

    public List<Categoria> obtenerCategoriasActivas() {
        return categoriaRepository.findByActivoTrue();
    }

    public void desactivarCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    // Ubicaciones
    public Ubicacion crearUbicacion(UbicacionDTO dto) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setEdificio(dto.getEdificio());
        ubicacion.setPiso(dto.getPiso());
        ubicacion.setSalon(dto.getSalon());
        return ubicacionRepository.save(ubicacion);
    }

    public List<Ubicacion> obtenerTodasUbicaciones() {
        return ubicacionRepository.findAll();
    }

    public List<Ubicacion> obtenerUbicacionesActivas() {
        return ubicacionRepository.findByActivoTrue();
    }

    public void desactivarUbicacion(Long id) {
        Ubicacion ubicacion = ubicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        ubicacion.setActivo(false);
        ubicacionRepository.save(ubicacion);
    }
}
