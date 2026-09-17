package org.bibliotecaviva.backend.domain.exceptions;

public class BookClubNotFoundException extends NotFoundException {
    public BookClubNotFoundException(String message) {
        super(message);
    }
}
