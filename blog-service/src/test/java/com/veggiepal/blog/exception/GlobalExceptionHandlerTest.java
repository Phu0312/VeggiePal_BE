package com.veggiepal.blog.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.veggiepal.blog.dto.response.ApiResponse;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

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
    void mapAttribute_replacesMinPlaceholder() {
        String result = ReflectionTestUtils.invokeMethod(
                globalExceptionHandler, "mapAttribute",
                "Content must be at least {min} characters",
                Map.of("min", 10));

        assertThat(result).isEqualTo("Content must be at least 10 characters");
    }

    @Test
    void mapAttribute_replacesMaxPlaceholder() {
        String result = ReflectionTestUtils.invokeMethod(
                globalExceptionHandler, "mapAttribute",
                "Title must be at most {max} characters",
                Map.of("max", 200));

        assertThat(result).isEqualTo("Title must be at most 200 characters");
    }

    @Test
    void mapAttribute_leavesMessageAloneWhenAttributeMissing() {
        String result = ReflectionTestUtils.invokeMethod(
                globalExceptionHandler, "mapAttribute",
                "Blog title is required",
                Map.of());

        assertThat(result).isEqualTo("Blog title is required");
    }

    @Test
    void handlingDataIntegrityViolation_returnsUncategorizedExceptionAndDoesNotLogUserData() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry '7-BLOG-12' for key 'uk_content_votes_user_target'");

        ResponseEntity<ApiResponse<?>> response =
                globalExceptionHandler.handlingDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(9999);

        List<String> loggedMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(loggedMessages).noneMatch(message -> message.contains("7-BLOG-12"));

        // getFormattedMessage() omits a trailing Throwable, so the message check alone
        // would still pass if the exception were logged. Assert no throwable is attached.
        assertThat(logAppender.list).allSatisfy(
                event -> assertThat(event.getThrowableProxy()).isNull());
    }
}
