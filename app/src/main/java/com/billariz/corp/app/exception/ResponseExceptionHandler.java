/**
 * Copyright (C) 2025 Uppli SAS — Billariz
 *
 * This file is part of Billariz, licensed under the GNU Affero General
 * Public License v3.0 (AGPL-3.0). You may use, modify and distribute
 * this software under the terms of the AGPL-3.0.
 *
 * For commercial use without AGPL obligations, contact:
 * contact@billariz.com | contact@uppli.fr
 * https://billariz.com
 */

package com.billariz.corp.app.exception;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.billariz.corp.launcher.utils.JournalUtils;
import com.billariz.corp.notifier.exception.ErrorDto;
import lombok.extern.slf4j.Slf4j;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler
{
    // #region HAL

    private final Locale locale = LocaleContextHolder.getLocale();

    @Autowired
    private JournalUtils     journalUtils;

    @ExceptionHandler(ProcessInterruptedException.class)
    public ResponseEntity<Object> handleProcessInterruptedException(ProcessInterruptedException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getStatus());
    }

     @ExceptionHandler(ResponseStatusException.class)
     protected ResponseEntity<Object> handleResponseStatusException(ResponseStatusException e)
     {
         var errors = new ErrorDto().code(e.getStatus().toString()).description(e.getReason());
         log.error("ResponseStatusException", errors);
         return new ResponseEntity<>(errors, e.getStatus());
     }

    @ExceptionHandler
    protected ResponseEntity<Object> handleRepositoryConstraintViolationException(RepositoryConstraintViolationException e)
    {
        var message = journalUtils.getMessage("NOT_SUPPORTED_DATA",null, locale);
        var errors = e.getErrors().getAllErrors().stream().map(
                err -> new ErrorDto().code("DATA_INTEGRITY").description(err.getObjectName() + "." + err.getDefaultMessage() + message)).toList();

        log.error("RepositoryConstraintViolationException", e);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    protected ResponseEntity<ErrorDto> handleDataIntegrityViolation(DataIntegrityViolationException e)
    {
        var error = new ErrorDto().code("DATA_INTEGRITY").description(e.getMessage());

        log.error("handleDataIntegrityViolation", e);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    protected ResponseEntity<ErrorDto> handleResourceNotFound(ResourceNotFoundException e)
    {
        var error = new ErrorDto().code("NO_RESULT").description(e.getMessage());

        log.error("handleResourceNotFound", e);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // #endregion

    // // #region Controllers

    // @ExceptionHandler
    // protected ResponseEntity<ErrorDto>
    // handleInvalidParameter(IllegalArgumentException e)
    // {
    // var error = new
    // ErrorDto().code("MISSING_PARAMETER").description(e.getMessage());

    // log.error("handleInvalidParameter", e);
    // return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    // }

    // @ExceptionHandler
    // protected ResponseEntity<Object>
    // handleMultipleErrorException(MultipleErrorException e)
    // {
    // var errors = e.getErrors().stream().map(err -> new
    // ErrorDto().code("NO_RESULT").description(err)).toList();

    // log.error("handleMultipleErrorException", e);
    // return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    // }

    @ExceptionHandler
    protected ResponseEntity<ErrorDto>
    illegalArgumentException(IllegalArgumentException e)
    {
        var error = new
        ErrorDto().code("NOT_COMPLIANT").description(e.getMessage());

        log.error("illegalArgumentException", e);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // @ExceptionHandler
    // protected ResponseEntity<ErrorDto> handleParameter(Exception e)
    // {
    // var error = new ErrorDto().code("UNEXPECTED_ERROR").description("An
    // internal error has occurred.");

    // log.error("handleParameter", e);
    // return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    // }

    // #endregion

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpHeaders headers, HttpStatus status, WebRequest request)
    {
        var error = new ErrorDto().code("UNEXPECTED_ERROR").description(e.getMessage());

        log.error("handleParameter", e);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

