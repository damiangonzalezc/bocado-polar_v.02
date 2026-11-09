package cl.bocadopolar.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.auth.dto.AuthDtos.AuthResponse;
import cl.bocadopolar.auth.dto.AuthDtos.CambioPasswordRequest;
import cl.bocadopolar.auth.dto.AuthDtos.LoginRequest;
import cl.bocadopolar.auth.dto.AuthDtos.RegistroRequest;
import cl.bocadopolar.auth.dto.AuthDtos.ValidacionResponse;
import cl.bocadopolar.auth.model.Usuario;
import cl.bocadopolar.auth.security.JwtService;
import cl.bocadopolar.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest req, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req, http.getRemoteAddr()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return service.login(req, http.getRemoteAddr());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization") String authorization,
            HttpServletRequest http) {
        service.logout(JwtService.extraer(authorization), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Usuario me(@RequestHeader(name = "Authorization") String authorization) {
        return service.usuarioDelToken(JwtService.extraer(authorization));
    }

    /** Ej: GET /api/v1/auth/validar?seccion=inventario */
    @GetMapping("/validar")
    public ValidacionResponse validar(@RequestHeader(name = "Authorization") String authorization,
            @RequestParam(name = "seccion", required = false) String seccion) {
        return service.validar(JwtService.extraer(authorization), seccion);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@RequestHeader(name = "Authorization") String authorization,
            @Valid @RequestBody CambioPasswordRequest req, HttpServletRequest http) {
        service.cambiarPassword(JwtService.extraer(authorization), req, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
