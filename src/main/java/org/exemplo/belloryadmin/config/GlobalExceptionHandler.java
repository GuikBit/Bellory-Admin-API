package org.exemplo.belloryadmin.config;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.entity.error.ResponseAPI;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseAPI<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseAPI.builder()
                .success(false)
                .message("Erro de validacao.")
                .errorCode(400)
                .errors(errors)
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseAPI<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseAPI.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(400)
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseAPI<Object> handleGeneralExceptions(Exception ex) {
        log.error("Erro nao esperado: ", ex);
        return ResponseAPI.builder()
                .success(false)
                .message("Ocorreu um erro interno no servidor.")
                .errorCode(500)
                .build();
    }
}
