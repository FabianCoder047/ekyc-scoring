package com.sfd.kyc.admin.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin/utilisateurs")
    public List<UtilisateurAdminDto> listerUtilisateurs() {
        return adminService.listerUtilisateurs();
    }

    @GetMapping("/admin/agences")
    public List<AgenceDto> listerAgences() {
        return adminService.listerAgences();
    }

    @PostMapping("/admin/utilisateurs")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurAdminDto creerUtilisateur(
            @Valid @RequestBody UtilisateurCreationRequest demande) {
        return adminService.creerUtilisateur(demande);
    }

    @PutMapping("/admin/utilisateurs/{id}")
    public UtilisateurAdminDto modifierUtilisateur(
            @PathVariable Long id, @Valid @RequestBody UtilisateurModificationRequest demande) {
        return adminService.modifierUtilisateur(id, demande);
    }

    @DeleteMapping("/admin/utilisateurs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactiverUtilisateur(@PathVariable Long id) {
        adminService.desactiverUtilisateur(id);
    }

    @GetMapping("/admin/criteres-scoring")
    public List<CritereDto> listerCriteres() {
        return adminService.listerCriteres();
    }

    @PostMapping("/admin/criteres-scoring")
    @ResponseStatus(HttpStatus.CREATED)
    public CritereDto creerCritere(@Valid @RequestBody CritereRequest demande) {
        return adminService.creerCritere(demande);
    }

    @PutMapping("/admin/criteres-scoring/{id}")
    public CritereDto modifierCritere(
            @PathVariable Long id, @Valid @RequestBody CritereRequest demande) {
        return adminService.modifierCritere(id, demande);
    }

    @DeleteMapping("/admin/criteres-scoring/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerCritere(@PathVariable Long id) {
        adminService.supprimerCritere(id);
    }
}
