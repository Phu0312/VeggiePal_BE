package com.veggiepal.payment.service;

import com.veggiepal.payment.dto.response.AdminPaymentDtos;
import com.veggiepal.payment.entity.PaymentOrder;
import com.veggiepal.payment.entity.PaymentStatus;
import com.veggiepal.payment.entity.PlanCode;
import com.veggiepal.payment.exception.AppException;
import com.veggiepal.payment.exception.ErrorCode;
import com.veggiepal.payment.repository.PaymentOrderRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPaymentService {
    public enum GroupBy { DAY, MONTH }

    private final PaymentOrderRepository orders;

    public AdminPaymentService(PaymentOrderRepository orders) { this.orders = orders; }

    @Transactional(readOnly = true)
    public AdminPaymentDtos.Summary summary(Instant from, Instant to, PlanCode planCode) {
        validateRange(from, to);
        List<PaymentOrder> transactions = orders.findAll(specification(null, planCode, from, to, "createdAt"));
        List<PaymentOrder> paid = verifiedPaid(from, to, planCode);
        Map<PaymentStatus, Long> distribution = emptyStatusCounts();
        transactions.forEach(order -> distribution.compute(order.getStatus(), (key, count) -> count + 1));
        long revenue = paid.stream().mapToLong(PaymentOrder::getAmount).sum();
        return new AdminPaymentDtos.Summary(revenue, transactions.size(), paid.size(),
                distribution.get(PaymentStatus.PENDING), distribution.get(PaymentStatus.CANCELLED),
                distribution.get(PaymentStatus.FAILED),
                paid.isEmpty() ? 0 : revenue / paid.size(), distribution);
    }

    @Transactional(readOnly = true)
    public AdminPaymentDtos.Statistics statistics(Instant from, Instant to, PlanCode planCode) {
        validateRange(from, to);
        List<PaymentOrder> transactions = orders.findAll(specification(null, planCode, from, to, "createdAt"),
                Sort.by(Sort.Direction.ASC, "createdAt"));
        List<AdminPaymentDtos.Distribution> statuses = new ArrayList<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            List<PaymentOrder> matching = transactions.stream().filter(order -> order.getStatus() == status).toList();
            statuses.add(new AdminPaymentDtos.Distribution(status.name(), matching.size(),
                    matching.stream().mapToLong(PaymentOrder::getAmount).sum()));
        }
        List<AdminPaymentDtos.Distribution> plans = new ArrayList<>();
        for (PlanCode code : PlanCode.values()) {
            List<PaymentOrder> matching = transactions.stream().filter(order -> order.getPlan().getCode() == code).toList();
            plans.add(new AdminPaymentDtos.Distribution(code.name(), matching.size(),
                    matching.stream().mapToLong(PaymentOrder::getAmount).sum()));
        }
        return new AdminPaymentDtos.Statistics(statuses, plans, trend(transactions, verifiedPaid(from, to, planCode), GroupBy.DAY));
    }

    @Transactional(readOnly = true)
    public AdminPaymentDtos.OrderPage findOrders(PaymentStatus status, PlanCode planCode,
                                                  Instant from, Instant to, int page, int size) {
        validateRange(from, to);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<PaymentOrder> result = orders.findAll(specification(status, planCode, from, to, "createdAt"),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new AdminPaymentDtos.OrderPage(result.getContent().stream().map(AdminPaymentService::order).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminPaymentDtos.Order getOrder(Long id) {
        return order(orders.findById(id).orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public AdminPaymentDtos.Revenue revenue(Instant from, Instant to, PlanCode planCode, GroupBy groupBy) {
        validateRange(from, to);
        GroupBy safeGroupBy = groupBy == null ? GroupBy.DAY : groupBy;
        List<PaymentOrder> paid = verifiedPaid(from, to, planCode);
        return new AdminPaymentDtos.Revenue(paid.stream().mapToLong(PaymentOrder::getAmount).sum(), paid.size(),
                safeGroupBy.name(), trend(List.of(), paid, safeGroupBy));
    }

    private List<PaymentOrder> verifiedPaid(Instant from, Instant to, PlanCode planCode) {
        return orders.findAll(specification(PaymentStatus.PAID, planCode, from, to, "paidAt"),
                        Sort.by(Sort.Direction.ASC, "paidAt")).stream()
                .filter(order -> order.getPaidAt() != null)
                .toList();
    }

    private static Specification<PaymentOrder> specification(PaymentStatus status, PlanCode planCode,
                                                               Instant from, Instant to, String dateField) {
        return (root, query, builder) -> {
            var conditions = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) conditions.add(builder.equal(root.get("status"), status));
            if (planCode != null) conditions.add(builder.equal(root.get("plan").get("code"), planCode));
            if (from != null) conditions.add(builder.greaterThanOrEqualTo(root.get(dateField), from));
            if (to != null) conditions.add(builder.lessThanOrEqualTo(root.get(dateField), to));
            return builder.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static List<AdminPaymentDtos.Point> trend(List<PaymentOrder> transactions,
                                                       List<PaymentOrder> paid, GroupBy groupBy) {
        DateTimeFormatter formatter = groupBy == GroupBy.MONTH
                ? DateTimeFormatter.ofPattern("yyyy-MM") : DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, long[]> values = new LinkedHashMap<>();
        transactions.forEach(order -> values.computeIfAbsent(period(order.getCreatedAt(), formatter), ignored -> new long[2])[0]++);
        paid.forEach(order -> {
            long[] value = values.computeIfAbsent(period(order.getPaidAt(), formatter), ignored -> new long[2]);
            if (transactions.isEmpty()) value[0]++;
            value[1] += order.getAmount();
        });
        return values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new AdminPaymentDtos.Point(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private static String period(Instant instant, DateTimeFormatter formatter) {
        return formatter.format(instant.atZone(ZoneOffset.UTC));
    }

    private static Map<PaymentStatus, Long> emptyStatusCounts() {
        Map<PaymentStatus, Long> result = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) result.put(status, 0L);
        return result;
    }

    private static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    private static AdminPaymentDtos.Order order(PaymentOrder order) {
        return new AdminPaymentDtos.Order(order.getId(), order.getOrderCode(), order.getUserId(),
                order.getPlan().getCode(), order.getAmount(), order.getStatus(), order.getPaymentLinkId(),
                order.getPaymentReference(), order.getCheckoutUrl(), order.getAccountName(),
                order.getAccountNumber(), order.getBin(), order.getDescription(), order.getCreatedAt(),
                order.getPaidAt(), order.getExpiredAt());
    }
}
