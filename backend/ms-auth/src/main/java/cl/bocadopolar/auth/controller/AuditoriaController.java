package cl.bocadopolar.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.auth.model.AuditoriaAcceso;
import cl.bocadopolar.auth.service.AuditoriaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService service;

    /** Últimos 200 accesos, o los de un usuario: GET /api/v1/auditoria?usuarioId=7 */
    @GetMapping
    public List<AuditoriaAcceso> listar(@RequestParam(name = "usuarioId", required = false) Long usuarioId) {
        return service.listar(usuarioId);
    }
}
