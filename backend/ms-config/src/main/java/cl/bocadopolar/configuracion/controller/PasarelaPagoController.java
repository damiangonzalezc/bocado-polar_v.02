package cl.bocadopolar.configuracion.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.configuracion.dto.ConfigDtos.PasarelaDisponibleResponse;
import cl.bocadopolar.configuracion.model.PasarelaPago;
import cl.bocadopolar.configuracion.service.ConfigService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pasarelas")
@RequiredArgsConstructor
public class PasarelaPagoController {

    private final ConfigService service;

    @GetMapping
    public List<PasarelaPago> listar() {
        return service.listarPasarelas();
    }

    /** Ej: GET /api/v1/pasarelas/disponible?nombre=Webpay (lo usa ms-pagos). */
    @GetMapping("/disponible")
    public PasarelaDisponibleResponse disponible(@RequestParam(name = "nombre") String nombre) {
        return new PasarelaDisponibleResponse(nombre, service.pasarelaDisponible(nombre));
    }

    @PatchMapping("/{id}/conectar")
    public PasarelaPago conectar(@PathVariable("id") Long id) {
        return service.cambiarConexion(id, true);
    }

    @PatchMapping("/{id}/desconectar")
    public PasarelaPago desconectar(@PathVariable("id") Long id) {
        return service.cambiarConexion(id, false);
    }
}
