package cl.bocadopolar.reportes.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.reportes.dto.ReporteDtos.AlertaOperativa;
import cl.bocadopolar.reportes.dto.ReporteDtos.KpiResponse;
import cl.bocadopolar.reportes.dto.ReporteDtos.TopProducto;
import cl.bocadopolar.reportes.dto.ReporteDtos.VentaDiaria;
import cl.bocadopolar.reportes.dto.ReporteDtos.VisitaRequest;
import cl.bocadopolar.reportes.model.KpiPeriodo;
import cl.bocadopolar.reportes.model.Periodo;
import cl.bocadopolar.reportes.model.VisitaDiaria;
import cl.bocadopolar.reportes.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;

    /** Ej: GET /api/v1/kpis?periodo=SIETE_DIAS  (HOY | SIETE_DIAS | MES_ACTUAL | TREINTA_DIAS) */
    @GetMapping("/kpis")
    public KpiResponse kpis(@RequestParam(name = "periodo", defaultValue = "SIETE_DIAS") Periodo periodo) {
        return service.kpis(periodo);
    }

    @GetMapping("/kpis/top-productos")
    public List<TopProducto> top(@RequestParam(name = "periodo", defaultValue = "TREINTA_DIAS") Periodo periodo,
            @RequestParam(name = "limite", defaultValue = "5") int limite) {
        return service.topProductos(periodo, limite);
    }

    @GetMapping("/kpis/ventas-diarias")
    public List<VentaDiaria> ventasDiarias(@RequestParam(name = "periodo", defaultValue = "TREINTA_DIAS") Periodo periodo) {
        return service.ventasDiarias(periodo);
    }

    /** cantidad = -1 significa que el microservicio correspondiente no respondió. */
    @GetMapping("/kpis/alertas-operativas")
    public List<AlertaOperativa> alertas() {
        return service.alertasOperativas();
    }

    @PostMapping("/kpis/snapshot")
    public KpiPeriodo snapshot(@RequestParam(name = "periodo", defaultValue = "SIETE_DIAS") Periodo periodo) {
        return service.guardarSnapshot(periodo);
    }

    @GetMapping("/kpis/historial")
    public List<KpiPeriodo> historial() {
        return service.historial();
    }

    /** Lo llama el front una vez por sesión del navegador. Body opcional: {"canal":"Instagram"} */
    @PostMapping("/visitas")
    public VisitaDiaria visita(@Valid @RequestBody(required = false) VisitaRequest req) {
        return service.registrarVisita(req == null ? null : req.canal());
    }

    @GetMapping("/visitas")
    public List<VisitaDiaria> visitas(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.visitas(desde, hasta);
    }
}
