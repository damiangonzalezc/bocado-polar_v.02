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

import cl.bocadopolar.catalogo.dto.CatalogoDtos.AtributoRequest;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.ValorRequest;
import cl.bocadopolar.catalogo.model.Atributo;
import cl.bocadopolar.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/atributos")
@RequiredArgsConstructor
public class AtributoController {

    private final CatalogoService service;

    @GetMapping
    public List<Atributo> listar() {
        return service.listarAtributos();
    }

    @GetMapping("/{id}")
    public Atributo obtener(@PathVariable("id") Long id) {
        return service.obtenerAtributo(id);
    }

    @PostMapping
    public ResponseEntity<Atributo> crear(@Valid @RequestBody AtributoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearAtributo(req));
    }

    @PutMapping("/{id}")
    public Atributo actualizar(@PathVariable("id") Long id, @Valid @RequestBody AtributoRequest req) {
        return service.actualizarAtributo(id, req);
    }

    @PostMapping("/{id}/valores")
    public Atributo agregarValor(@PathVariable("id") Long id, @Valid @RequestBody ValorRequest req) {
        return service.agregarValor(id, req.valor());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarAtributo(id);
        return ResponseEntity.noContent().build();
    }
}
