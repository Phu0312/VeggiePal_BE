package com.veggiepal.recipe.dto.response;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class PageSerializationTest {
    @Test void serializesPageContract() {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var page = PageResponse.<String>builder().items(List.of("sample")).page(0).size(20)
                .totalElements(1).totalPages(1).build();
        var json = mapper.readTree(mapper.writeValueAsString(page));
        assertThat(json.get("items").get(0).asText()).isEqualTo("sample");
        assertThat(json.get("page").asInt()).isZero();
        assertThat(json.get("size").asInt()).isEqualTo(20);
        assertThat(json.has("first")).isFalse(); assertThat(json.has("last")).isFalse();
        assertThat(json.has("content")).isFalse();
    }
}
