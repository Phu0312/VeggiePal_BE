package com.veggiepal.payment.payos;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.veggiepal.payment.exception.PayOsOperationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayOsSdkAdapterTest {
    @Test
    void verifyWebhook_emptyPayloadWrapsSdkRuntimeFailure() {
        PayOsSdkAdapter adapter = new PayOsSdkAdapter("test-client", "test-api-key", "test-checksum-key");

        assertThatThrownBy(() -> adapter.verifyWebhook(Map.of()))
                .isInstanceOf(PayOsOperationException.class);
    }
}
