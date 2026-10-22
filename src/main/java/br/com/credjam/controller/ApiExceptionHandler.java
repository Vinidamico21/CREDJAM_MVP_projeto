package br.com.credjam.controller;

import br.com.credjam.dto.Modelo;
import br.com.credjam.exception.ErroNegocioException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ErroNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Modelo.ErroResponse negocio(ErroNegocioException ex) {
        return new Modelo.ErroResponse(ex.getCodigo(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Modelo.ErroResponse jsonInvalido() {
        return new Modelo.ErroResponse(
            "JSON_INVALIDO",
            "Verifique o formato das datas e dos valores enviados.",
            Map.of()
        );
    }
}
