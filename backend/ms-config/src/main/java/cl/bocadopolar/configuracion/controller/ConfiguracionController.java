package cl.bocadopolar.configuracion.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.configuracion.dto.ConfigDtos.ConfiguracionRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.IvaResponse;
import cl.bocadopolar.configuracion.model.ConfiguracionGlobal;
import cl.bocadopolar.configuracion.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfigService service;

    @GetMapping
    public ConfiguracionGlobal obtener() {
        return service.obtenerConfiguracion();
    }

    @PutMapping
    public ConfiguracionGlobal actualizar(@Valid @RequestBody ConfiguracionRequest req) {
        return service.actualizarConfiguracion(req);
    }

    @GetMapping("/iva")
    public IvaResponse iva() {
        return new IvaResponse(service.obtenerIva());
    }
}
