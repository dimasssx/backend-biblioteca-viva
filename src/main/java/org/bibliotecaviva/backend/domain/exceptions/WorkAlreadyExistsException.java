package org.bibliotecaviva.backend.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT,reason = "Work already exists")
public class WorkAlreadyExistsException extends RuntimeException {
    public WorkAlreadyExistsException(String message) {
        super(message);
    }
}
