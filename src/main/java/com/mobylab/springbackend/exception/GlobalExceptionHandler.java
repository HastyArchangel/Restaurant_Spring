package com.mobylab.springbackend.exception;


import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<ErrorObject> handleBadRequest(BadRequestException ex, WebRequest request) {
        logger.warn("Handling BadRequestException: {}", ex.getMessage());
        ErrorObject errorObject = new ErrorObject();
        errorObject
                .setStatusCode(HttpStatus.BAD_REQUEST.value())
                .setMessage(ex.getMessage())
                .setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(errorObject, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({EntityNotFoundException.class})
    public ResponseEntity<ErrorObject> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        logger.warn("Handling EntityNotFoundException: {}", ex.getMessage());
        ErrorObject errorObject = new ErrorObject();
        errorObject
                .setStatusCode(HttpStatus.NOT_FOUND.value())
                .setMessage(ex.getMessage()) // Usually informative enough
                .setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(errorObject, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ DataIntegrityViolationException.class })
    public ResponseEntity<ErrorObject> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        logger.error("Handling DataIntegrityViolationException: {}", ex.getMessage(), ex);
        ErrorObject errorObject = new ErrorObject();
        String message = "Database error: Constraint violation.";
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unique constraint") || ex.getMessage().toLowerCase().contains("duplicate key")) {
            message = "Failed due to a data constraint. Perhaps you tried to create a duplicate item?";
        }
        errorObject
                .setStatusCode(HttpStatus.BAD_REQUEST.value())
                .setMessage(message)
                .setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(errorObject, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ AccessDeniedException.class })
    public ResponseEntity<ErrorObject> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        logger.warn("Handling AccessDeniedException: {}", ex.getMessage());
        ErrorObject errorObject = new ErrorObject();
        errorObject
                .setStatusCode(HttpStatus.FORBIDDEN.value())
                .setMessage("Access Denied: You do not have permission to perform this action.")
                .setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(errorObject, HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler({InternalServerErrorException.class})
    public ResponseEntity<ErrorObject> handleInternalServerError(InternalServerErrorException ex, WebRequest request) {
        logger.error("Handling InternalServerErrorException: {}", ex.getMessage(), ex);
        ErrorObject errorObject = new ErrorObject();
        errorObject
                .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setMessage(ex.getMessage())
                .setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(errorObject, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Catch-all for other RuntimeExceptions (LAST RESORT)
    @ExceptionHandler({ RuntimeException.class })
    public ResponseEntity<ErrorObject> handleGenericRuntimeException(RuntimeException ex, WebRequest request) {
        if (ex instanceof BadRequestException || ex instanceof EntityNotFoundException ||
                ex instanceof DataIntegrityViolationException || ex instanceof AccessDeniedException ||
                ex instanceof InternalServerErrorException) {
            logger.error("Generic handler caught specific exception unexpectedly: {}", ex.getClass().getName(), ex);
        } else {
            logger.error("Handling Generic RuntimeException: {}", ex.getMessage(), ex);
        }

        ErrorObject errorObject = new ErrorObject();
        errorObject
                .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setMessage("An unexpected internal error occurred.")
                .setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(errorObject, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}