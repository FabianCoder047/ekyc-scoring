package com.sfd.kyc.auth.web;

import com.sfd.kyc.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse connecter(@Valid @RequestBody LoginRequest demande) {
        return authService.connecter(demande.email(), demande.password());
    }

    @PostMapping("/refresh")
    public AuthResponse rafraichir(@Valid @RequestBody RefreshRequest demande) {
        return authService.rafraichir(demande.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deconnecter(@Valid @RequestBody RefreshRequest demande) {
        authService.deconnecter(demande.refreshToken());
    }
}
