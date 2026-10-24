package com.sfd.kyc.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> proprietes;

    public ApiException(HttpStatus status, String code, String detail) {
        this(status, code, detail, Map.of());
    }

    public ApiException(HttpStatus status, String code, String detail, Map<String, Object> proprietes) {
        super(detail);
        this.status = status;
        this.code = code;
        this.proprietes = proprietes == null ? Map.of() : Map.copyOf(proprietes);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, Object> proprietes() {
        return proprietes;
    }

    public static ApiException introuvable(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.RESSOURCE_INTROUVABLE, detail);
    }

    public static ApiException transition(String detail) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.TRANSITION_INVALIDE, detail);
    }

    public static ApiException validation(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ECHEC, detail);
    }
}
