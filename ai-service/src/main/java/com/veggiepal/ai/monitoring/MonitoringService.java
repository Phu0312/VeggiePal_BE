package com.veggiepal.ai.monitoring;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class MonitoringService {
    private final UsageLogRepository repository;

    public MonitoringService(UsageLogRepository repository) { this.repository = repository; }

    public UsageLogPage search(String feature, String excludeFeature, String status, Long userId, LocalDateTime from,
            LocalDateTime to, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Specification<UsageLog> specification = (root, query, builder) -> builder.conjunction();
        if (feature != null && !feature.isBlank()) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("feature"), feature.trim().toUpperCase()));
        }
        String excluded = normalize(excludeFeature);
        if (excluded != null) {
            specification = specification.and((root, query, builder) ->
                    builder.notEqual(root.get("feature"), excluded));
        }
        if (status != null && !status.isBlank()) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status.trim().toUpperCase()));
        }
        if (userId != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("userId"), userId));
        if (from != null) specification = specification.and((root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("createdAt"), from));
        if (to != null) specification = specification.and((root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("createdAt"), to));
        Page<UsageLog> result = repository.findAll(specification,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new UsageLogPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    static String normalize(String feature) {
        return feature == null || feature.isBlank() ? null : feature.trim().toUpperCase();
    }

    public record UsageLogPage(java.util.List<UsageLog> items, int page, int size,
                               long totalElements, int totalPages) {}
}
