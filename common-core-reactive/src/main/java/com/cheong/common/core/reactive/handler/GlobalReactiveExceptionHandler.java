package com.cheong.common.core.reactive.handler;

import io.r2dbc.spi.R2dbcException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalReactiveExceptionHandler {

    @ExceptionHandler(value = R2dbcException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ProblemDetail> handleInternalServerError(Throwable throwable){
        ProblemDetail problemDetail = ProblemDetail.forStatus(500);
        problemDetail.setTitle("Internal server error");
        problemDetail.setDetail(throwable.getMessage());
        return Mono.just(problemDetail);
    }
}
