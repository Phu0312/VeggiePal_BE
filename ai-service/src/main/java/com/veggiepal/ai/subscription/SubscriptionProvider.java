package com.veggiepal.ai.subscription;

public interface SubscriptionProvider {
    SubscriptionEntitlement getEffectiveSubscription(Long userId);
}
