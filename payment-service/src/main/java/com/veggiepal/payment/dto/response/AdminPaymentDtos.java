package com.veggiepal.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.veggiepal.payment.entity.PaymentStatus;
import com.veggiepal.payment.entity.PlanCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminPaymentDtos {
    private AdminPaymentDtos() {}

    public record Order(Long id, Long orderCode, Long userId, PlanCode planCode, long amount,
                        PaymentStatus status, String paymentLinkId, String paymentReference,
                        String checkoutUrl, String accountName, String accountNumber, String bin,
                        String description, Instant createdAt, Instant paidAt, Instant expiredAt) {
        @JsonProperty("orderId")
        public Long orderId() { return id; }
    }

    public record OrderPage(List<Order> items, int page, int size, long totalElements, int totalPages) {}

    public record Summary(long totalRevenue, long totalTransactions, long paidTransactions,
                          long pendingTransactions, long cancelledTransactions, long failedTransactions,
                          long averagePaidAmount, Map<PaymentStatus, Long> statusDistribution) {}

    public record Distribution(String key, long count, long amount) {}

    public record Point(String period, long transactionCount, long revenue) {}

    public record Statistics(List<Distribution> statusDistribution,
                             List<Distribution> planDistribution,
                             List<Point> transactionTrend) {}

    public record Revenue(long totalRevenue, long paidTransactions, String groupBy, List<Point> points) {}
}
