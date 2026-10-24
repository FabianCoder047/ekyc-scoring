package com.sfd.kyc.common.web;

import java.util.List;

public record PageResponse<T>(List<T> contenu, int page, int taille, long total) {

    public static <T> PageResponse<T> de(List<T> contenu, int page, int taille, long total) {
        return new PageResponse<>(contenu, page, taille, total);
    }
}
