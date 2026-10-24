package com.sfd.kyc;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfd.kyc.common.util.Masquage;
import org.junit.jupiter.api.Test;

class MasquageTest {

    @Test
    void masque_tout_sauf_deux_prefixes_et_quatre_suffixes() {
        assertThat(Masquage.numeroPiece("TG1234567891234")).isEqualTo("TG****1234");
    }

    @Test
    void masque_entierement_les_numeros_courts() {
        assertThat(Masquage.numeroPiece("12345")).isEqualTo("****");
        assertThat(Masquage.numeroPiece(null)).isEqualTo("****");
    }
}
