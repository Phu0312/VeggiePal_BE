package com.veggiepal.ai.configuration;

import com.veggiepal.ai.groq.GroqProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class RestClientConfig {
    @Bean
    com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Bean("groqRestClient")
    RestClient groqRestClient(GroqProperties properties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory)
                .messageConverters(items -> items.add(0, new MappingJackson2HttpMessageConverter(objectMapper))).build();
    }

    @Bean("groqTranscriptionRestClient")
    RestClient groqTranscriptionRestClient(GroqProperties properties,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${groq.api.transcription-read-timeout:10m}")
            java.time.Duration transcriptionReadTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(transcriptionReadTimeout);
        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory)
                .messageConverters(items -> items.add(0, new MappingJackson2HttpMessageConverter(objectMapper))).build();
    }

    @Bean("serviceRestClient")
    RestClient serviceRestClient(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(3));
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory)
                .messageConverters(items -> items.add(0, new MappingJackson2HttpMessageConverter(objectMapper))).build();
    }
}
