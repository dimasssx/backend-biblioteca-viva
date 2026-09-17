package org.bibliotecaviva.backend.api.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.bibliotecaviva.backend.domain.exceptions.BookClubNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void typeMismatchShouldReturnBadRequestPayload() {
        HttpServletRequest request = request("/work/not-a-uuid");

        var response = handler.handleTypeMismatchException(
                new TypeMismatchException("not-a-uuid", UUID.class), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Argumento Inválido ", response.getBody().message());
        assertEquals("/work/not-a-uuid", response.getBody().path());
    }

    @Test
    void illegalArgumentShouldReturnBadRequestPayload() {
        HttpServletRequest request = request("/work/articles");

        var response = handler.handleIllegalArgumentException(new IllegalArgumentException("detail"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Argumento Inválido ", response.getBody().message());
        assertEquals("/work/articles", response.getBody().path());
    }

    @Test
    void bookClubNotFoundShouldReturn404() {
        HttpServletRequest request = request("/book-clubs/reviews");

        var response = handler.handleApiErrorException(
                new BookClubNotFoundException("BookClub not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("BookClub not found", response.getBody().message());
        assertEquals("/book-clubs/reviews", response.getBody().path());
    }

    @Test
    void unreadableJsonShouldReturnBadRequest() {
        HttpServletRequest request = request("/book-clubs/reviews");
        HttpInputMessage inputMessage = new HttpInputMessage() {
            @Override public InputStream getBody() { return new ByteArrayInputStream(new byte[0]); }
            @Override public org.springframework.http.HttpHeaders getHeaders() { return org.springframework.http.HttpHeaders.EMPTY; }
        };

        var response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("bad json", inputMessage), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Corpo da requisição inválido ou ilegível", response.getBody().message());
    }

    @Test
    void missingMultipartPartShouldReturnBadRequest() {
        HttpServletRequest request = request("/uploads");

        var response = handler.handleMissingServletRequestPart(
                new MissingServletRequestPartException("file"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertTrue(response.getBody().message().contains("file"));
    }

    @Test
    void oversizedUploadShouldReturn413() {
        HttpServletRequest request = request("/uploads");

        var response = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(1024L), request);

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(413, response.getBody().status());
        assertEquals("Arquivo enviado excede o tamanho máximo permitido", response.getBody().message());
    }

    @Test
    void unsupportedMediaTypeShouldReturn415() {
        HttpServletRequest request = request("/book-clubs/reviews");

        var response = handler.handleHttpMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException(APPLICATION_JSON, List.of(), null), request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(415, response.getBody().status());
        assertTrue(response.getBody().message().contains("application/json"));
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
