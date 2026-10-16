package com.veggiepal.ai.groq;

import java.util.List;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.groq.dto.GroqResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GroqClient {
    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private final RestClient restClient;
    private final GroqProperties properties;

    public GroqClient(@Qualifier("groqRestClient") RestClient restClient, GroqProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public GroqResult complete(String model, List<GroqRequest.Message> messages, double temperature, int maxTokens) {
        if (properties.key() == null || properties.key().isBlank()) {
            log.error("GROQ_API_KEY is not configured or blank. Please check your .env configuration.");
            throw new AppException(ErrorCode.GROQ_NOT_CONFIGURED);
        }
        var request = new GroqRequest(model, messages, temperature, maxTokens);
        for (int attempt = 0; ; attempt++) {
            try {
                GroqResponse response = restClient.post().uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.key())
                        .body(request).retrieve().body(GroqResponse.class);
                if (response == null || response.choices() == null || response.choices().isEmpty()
                        || response.choices().getFirst().message() == null) {
                    log.error("Groq returned empty or malformed completion structure for model: {}", model);
                    throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
                }
                var usage = response.usage();
                return new GroqResult(response.choices().getFirst().message().content(),
                        usage == null ? 0 : usage.prompt_tokens(), usage == null ? 0 : usage.completion_tokens());
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                logSafeHttpResponseError("complete", model, attempt, status, exception.getResponseBodyAsString());
                boolean retryable = status == 429 || exception instanceof HttpServerErrorException;
                if (!retryable || attempt >= properties.maxRetries()) {
                    throw new AppException(ErrorCode.GROQ_UNAVAILABLE, exception);
                }
                backoff(attempt);
            } catch (RestClientException exception) {
                log.warn("Groq client communication or parsing error on attempt {}/{} for operation 'complete': {}",
                        attempt + 1, properties.maxRetries() + 1, exception.getMessage());
                if (attempt >= properties.maxRetries()) {
                    throw new AppException(ErrorCode.GROQ_UNAVAILABLE, exception);
                }
                backoff(attempt);
            }
        }
    }

    public String transcribe(java.nio.file.Path audioFile, String model) {
        if (properties.key() == null || properties.key().isBlank()) {
            log.error("GROQ_API_KEY is not configured or blank. Please check your .env configuration.");
            throw new AppException(ErrorCode.GROQ_NOT_CONFIGURED);
        }
        for (int attempt = 0; ; attempt++) {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("file", new FileSystemResource(audioFile));
            body.add("model", model);
            body.add("response_format", "json");
            try {
                com.fasterxml.jackson.databind.JsonNode response = restClient.post().uri("/audio/transcriptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.key())
                        .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve()
                        .body(com.fasterxml.jackson.databind.JsonNode.class);
                if (response == null || response.path("text").asText().isBlank()) {
                    log.error("Groq returned empty transcription text for model: {}", model);
                    throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
                }
                return response.path("text").asText();
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                logSafeHttpResponseError("transcribe", model, attempt, status, exception.getResponseBodyAsString());
                boolean retryable = status == 429 || exception instanceof HttpServerErrorException;
                if (!retryable || attempt >= properties.maxRetries()) {
                    throw new AppException(ErrorCode.GROQ_UNAVAILABLE, exception);
                }
                backoff(attempt);
            } catch (RestClientException exception) {
                log.warn("Groq client communication or parsing error on attempt {}/{} for operation 'transcribe': {}",
                        attempt + 1, properties.maxRetries() + 1, exception.getMessage());
                if (attempt >= properties.maxRetries()) {
                    throw new AppException(ErrorCode.GROQ_UNAVAILABLE, exception);
                }
                backoff(attempt);
            }
        }
    }

    private void logSafeHttpResponseError(String operation, String model, int attempt, int status, String rawBody) {
        String sanitizedBody = sanitizeBody(rawBody);
        if (status == 401 || status == 403) {
            log.error("Groq authentication failure (HTTP {}) for operation '{}'. Check if GROQ_API_KEY is valid and active.", status, operation);
        } else if (status == 429) {
            log.warn("Groq rate limit exceeded (HTTP 429) on attempt {}/{} for operation '{}'. Backing off...", attempt + 1, properties.maxRetries() + 1, operation);
        } else if (status == 400 || status == 404 || status == 422) {
            log.error("Groq client error (HTTP {}) for operation '{}', model '{}'. Response: {}", status, operation, model, sanitizedBody);
        } else if (status >= 500) {
            log.warn("Groq server error (HTTP {}) on attempt {}/{} for operation '{}'. Response: {}", status, attempt + 1, properties.maxRetries() + 1, operation, sanitizedBody);
        } else {
            log.error("Groq unexpected HTTP status {} for operation '{}'. Response: {}", status, operation, sanitizedBody);
        }
    }

    private String sanitizeBody(String body) {
        if (body == null || body.isBlank()) return "<empty>";
        String cleaned = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (cleaned.length() > 300) {
            return cleaned.substring(0, 300) + "...";
        }
        return cleaned;
    }

    private void backoff(int attempt) {
        try { Thread.sleep(250L * (1L << attempt)); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.GROQ_UNAVAILABLE, exception);
        }
    }
    public record GroqResult(String content, int promptTokens, int completionTokens) {}
}

