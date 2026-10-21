package com.veggiepal.payment.service;

import com.veggiepal.payment.dto.response.PlanResponse;
import com.veggiepal.payment.entity.SubscriptionPlan;
import com.veggiepal.payment.repository.SubscriptionPlanRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {
    private final SubscriptionPlanRepository repository;
    public PlanService(SubscriptionPlanRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<PlanResponse> getPlans() {
        return repository.findAllByActiveTrueOrderByPriceAsc().stream().map(PlanService::response).toList();
    }
    static PlanResponse response(SubscriptionPlan p) {
        return new PlanResponse(p.getId(), p.getCode(), p.getPrice(), p.getDurationDays(), p.getChatPerDay(),
                p.getMealPlansPerMonth(), p.getVideosPerMonth());
    }
}
