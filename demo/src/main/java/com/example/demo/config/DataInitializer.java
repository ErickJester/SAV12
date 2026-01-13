package com.example.demo.config;

import com.example.demo.entity.Categoria;
import com.example.demo.entity.Ubicacion;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.UbicacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Inicializar categorías si no existen
        if (categoriaRepository.count() == 0) {
            System.out.println("Inicializando categorías...");
            
            categoriaRepository.save(new Categoria("Hardware", "Problemas relacionados con equipo físico"));
            categoriaRepository.save(new Categoria("Software", "Problemas con aplicaciones y sistemas operativos"));
            categoriaRepository.save(new Categoria("Red", "Problemas de conectividad y red"));
            categoriaRepository.save(new Categoria("Impresoras", "Problemas con impresoras y escáneres"));
            categoriaRepository.save(new Categoria("Audio/Video", "Problemas con proyectores, audio y video"));
            categoriaRepository.save(new Categoria("Acceso", "Problemas de acceso a sistemas y cuentas"));
            categoriaRepository.save(new Categoria("Otro", "Otros problemas no categorizados"));
            
            System.out.println("✓ Categorías creadas: " + categoriaRepository.count());
        }

        // Inicializar ubicaciones si no existen
        if (ubicacionRepository.count() == 0) {
            System.out.println("Inicializando ubicaciones...");
            
            ubicacionRepository.save(new Ubicacion("Edificio Central", "Planta Baja", "Sala 101"));
            ubicacionRepository.save(new Ubicacion("Edificio Central", "Planta Baja", "Sala 102"));
            ubicacionRepository.save(new Ubicacion("Edificio Central", "Primer Piso", "Sala 201"));
            ubicacionRepository.save(new Ubicacion("Edificio Central", "Primer Piso", "Sala 202"));
            ubicacionRepository.save(new Ubicacion("Edificio Central", "Segundo Piso", "Sala 301"));
            ubicacionRepository.save(new Ubicacion("Edificio Central", "Segundo Piso", "Sala 302"));
            ubicacionRepository.save(new Ubicacion("Edificio Norte", "Planta Baja", "Laboratorio 1"));
            ubicacionRepository.save(new Ubicacion("Edificio Norte", "Planta Baja", "Laboratorio 2"));
            ubicacionRepository.save(new Ubicacion("Edificio Norte", "Primer Piso", "Aula Magna"));
            ubicacionRepository.save(new Ubicacion("Edificio Sur", "Planta Baja", "Biblioteca"));
            ubicacionRepository.save(new Ubicacion("Edificio Sur", "Primer Piso", "Sala de Profesores"));
            ubicacionRepository.save(new Ubicacion("Edificio Oeste", "Planta Baja", "Cafetería"));
            ubicacionRepository.save(new Ubicacion("Edificio Oeste", "Primer Piso", "Auditorio"));
            
            System.out.println("✓ Ubicaciones creadas: " + ubicacionRepository.count());
        }

        System.out.println("=== Sistema SAV12 listo ===");
        System.out.println("Categorías disponibles: " + categoriaRepository.count());
        System.out.println("Ubicaciones disponibles: " + ubicacionRepository.count());
    }
}
