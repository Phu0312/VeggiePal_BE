package com.veggiepal.ai.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

// The mapper Spring MVC uses must pick the serializer up: a bare LocalDateTime is read by the FE as local time.
@SpringBootTest
class UtcDateTimeSerializerTest {
    @Autowired JsonMapper mapper;

    @Test
    void localDateTimeIsWrittenAsAnInstantInTheJvmZone() {
        LocalDateTime stored = LocalDateTime.of(2026, 10, 31, 13, 45, 27);
        String expected = stored.atZone(ZoneId.systemDefault()).toInstant().toString();

        assertEquals("\"" + expected + "\"", mapper.writeValueAsString(stored));
        assertEquals('Z', expected.charAt(expected.length() - 1));
    }
}
