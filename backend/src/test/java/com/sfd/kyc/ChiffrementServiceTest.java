package com.sfd.kyc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfd.kyc.common.config.AppProperties;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import com.sfd.kyc.kyc.service.ChiffrementService;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChiffrementServiceTest {

    private ChiffrementService chiffrementService;
    private byte[] clair;

    @BeforeEach
    void preparer() {
        byte[] cle = new byte[32];
        new SecureRandom().nextBytes(cle);
        AppProperties proprietes = new AppProperties(
                new AppProperties.Jwt(
                        Base64.getEncoder().encodeToString(new byte[32]), "test", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AppProperties.Security(5, Duration.ofMinutes(15), "http://localhost:4200"),
                new AppProperties.Stockage("/tmp/opencode/kyc-test"),
                new AppProperties.Chiffrement(Base64.getEncoder().encodeToString(cle)),
                new AppProperties.Documents(5 * 1024 * 1024));
        chiffrementService = new ChiffrementService(proprietes);
        clair = "contenu-jpeg-header-bidon".getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void le_contenu_stocke_est_illisible_et_le_dechiffrement_est_fidele() {
        ChiffrementService.Chiffre chiffre = chiffrementService.chiffrer(clair);

        assertThat(new String(chiffre.contenu(), StandardCharsets.ISO_8859_1))
                .doesNotContain("contenu-jpeg");
        assertThat(chiffrementService.dechiffrer(chiffre.contenu(), chiffre.iv())).isEqualTo(clair);
    }

    @Test
    void une_alteration_est_detectee_par_le_tag_gcm() {
        ChiffrementService.Chiffre chiffre = chiffrementService.chiffrer(clair);
        chiffre.contenu()[0] ^= 0x01;

        assertThatThrownBy(() -> chiffrementService.dechiffrer(chiffre.contenu(), chiffre.iv()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).code())
                        .isEqualTo(ErrorCodes.DOCUMENT_ALTERE));
    }
}
