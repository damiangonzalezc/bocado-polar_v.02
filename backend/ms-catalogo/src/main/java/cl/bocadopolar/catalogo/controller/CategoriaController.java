package cl.bocadopolar.catalogo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.catalogo.dto.CatalogoDtos.CategoriaRequest;
import cl.bocadopolar.catalogo.model.Categoria;
import cl.bocadopolar.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CatalogoService service;

    @GetMapping
    public List<Categoria> listar() {
        return service.listarCategorias();
    }

    @GetMapping("/{id}")
    public Categoria obtener(@PathVariable("id") Long id) {
        return service.obtenerCategoria(id);
    }

    @PostMapping
    public ResponseEntity<Categoria> crear(@Valid @RequestBody CategoriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCategoria(req));
    }

    @PutMapping("/{id}")
    public Categoria actualizar(@PathVariable("id") Long id, @Valid @RequestBody CategoriaRequest req) {
        return service.actualizarCategoria(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
