package com.veggiepal.payment.service;

import com.veggiepal.payment.entity.*;
import com.veggiepal.payment.repository.SubscriptionPlanRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanCatalogInitializer implements ApplicationRunner {
    private final SubscriptionPlanRepository repository;
    public PlanCatalogInitializer(SubscriptionPlanRepository repository) { this.repository = repository; }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        List<PlanSpec> specs = List.of(
                new PlanSpec(PlanCode.FREE, 0, 0, 5, 2, 0),
                new PlanSpec(PlanCode.PLUS, 29_000, 30, 20, 8, 3),
                new PlanSpec(PlanCode.PRO, 59_000, 30, 50, 20, 8));
        for (PlanSpec spec : specs) {
            SubscriptionPlan plan = repository.findByCode(spec.code()).orElseGet(SubscriptionPlan::new);
            plan.setCode(spec.code());
            plan.setPrice(spec.price());
            plan.setDurationDays(spec.durationDays());
            plan.setChatPerDay(spec.chat());
            plan.setMealPlansPerMonth(spec.meals());
            plan.setVideosPerMonth(spec.videos());
            plan.setActive(true);
            repository.save(plan);
        }
    }

    private record PlanSpec(PlanCode code, long price, int durationDays, int chat, int meals, int videos) {}
}
