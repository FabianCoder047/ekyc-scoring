package com.sfd.kyc.kyc.service;

import com.sfd.kyc.common.config.AppProperties;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FileSystemFileStorage implements FileStorage {

    private static final Pattern CLE_VALIDE =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final Path racine;

    public FileSystemFileStorage(AppProperties proprietes) {
        this.racine = Path.of(proprietes.stockage().chemin()).toAbsolutePath().normalize();
    }

    @Override
    public void enregistrer(String cle, byte[] contenu) {
        Path destination = resoudre(cle);
        try {
            Files.createDirectories(racine);
            Path temporaire = Files.createTempFile(racine, "upload-", ".tmp");
            Files.write(temporaire, contenu);
            Files.move(temporaire, destination,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.STOCKAGE_ERREUR,
                    "L'enregistrement du document a échoué.");
        }
    }

    @Override
    public byte[] lire(String cle) {
        Path fichier = resoudre(cle);
        try {
            return Files.readAllBytes(fichier);
        } catch (IOException e) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.STOCKAGE_ERREUR,
                    "La lecture du document a échoué.");
        }
    }

    private Path resoudre(String cle) {
        if (cle == null || !CLE_VALIDE.matcher(cle).matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, ErrorCodes.REQUETE_INVALIDE, "Référence de stockage invalide.");
        }
        Path chemin = racine.resolve(cle + ".bin").normalize();
        if (!chemin.startsWith(racine)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, ErrorCodes.REQUETE_INVALIDE, "Référence de stockage invalide.");
        }
        return chemin;
    }
}
