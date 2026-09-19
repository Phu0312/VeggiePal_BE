package com.veggiepal.nutrition.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.veggiepal.nutrition.dto.response.ApiResponse;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    private ListAppender<ILoggingEvent> logAppender;
    private ch.qos.logback.classic.Logger handlerLogger;

    @BeforeEach
    void attachLogAppender() {
        handlerLogger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        handlerLogger.detachAppender(logAppender);
    }

    @Test
    void handlingDataIntegrityViolation_returnsUncategorizedExceptionAndDoesNotLogUserData() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry '7-4' for key 'uk_user_allergies_user_allergen'");

        ResponseEntity<ApiResponse<?>> response =
                globalExceptionHandler.handlingDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(9999);

        List<String> loggedMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(loggedMessages).noneMatch(message -> message.contains("7-4"));
    }
}
