package com.veggiepal.payment.service;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class OrderCodeGenerator {
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());
    public long next() {
        return sequence.updateAndGet(previous -> Math.max(previous + 1, System.currentTimeMillis()));
    }
}
