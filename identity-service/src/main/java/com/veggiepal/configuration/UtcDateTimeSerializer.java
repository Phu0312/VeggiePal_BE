package com.veggiepal.configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Entities store LocalDateTime.now(), i.e. wall-clock time in the JVM zone (UTC on the server). Written bare, the FE
 * reads it as local time and shows it 7 hours off, so every LocalDateTime goes out as an instant with a Z.
 */
@JacksonComponent
public class UtcDateTimeSerializer extends ValueSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator generator, SerializationContext context) {
        generator.writeString(value.atZone(ZoneId.systemDefault()).toInstant().toString());
    }
}
