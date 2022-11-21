package org.comroid.status.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String detail) {
        super("Invalid data: " + detail);
    }
}
