package io.github.anistor.jackpot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record ErrorResponse(int status, String error, String message) {

    public static ErrorResponse of(HttpStatusCode status, String message) {
        return new ErrorResponse(status.value(), status instanceof HttpStatus httpStatus ? httpStatus.getReasonPhrase() : String.valueOf(status.value()), message);
    }
}
