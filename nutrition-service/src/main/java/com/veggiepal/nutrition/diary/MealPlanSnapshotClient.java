package com.veggiepal.nutrition.diary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import com.veggiepal.nutrition.exception.*;
import static com.veggiepal.nutrition.diary.DiaryDtos.*;

@Component
public class MealPlanSnapshotClient {
    private final RestClient http;
    private final String aiUrl;
    private final String recipeUrl;
    public MealPlanSnapshotClient(@Value("${services.ai.base-url:http://localhost:8086}") String aiUrl,
            @Value("${services.recipe.base-url:http://localhost:8084}") String recipeUrl) {
        this.aiUrl=aiUrl; this.recipeUrl=recipeUrl;
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3)); factory.setReadTimeout(Duration.ofSeconds(10));
        this.http=RestClient.builder().requestFactory(factory).build();
    }
    // The original user JWT is forwarded: ai-service enforces ownership of the plan.
    public MealSnapshot fetch(String token, Long planId, LocalDate date, MealType mealType) {
        try {
            Map<?,?> plan=result(http.get().uri(aiUrl+"/ai/meal-plans/{id}",planId)
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Map.class));
            Map<?,?> body=map(plan.get("plan"));
            if (!(body.get("days") instanceof List<?> days)) throw unavailable();
            for (Object rawDay:days) {
                Map<?,?> day=map(rawDay);
                if (!date.getDayOfWeek().name().equals(day.get("day"))) continue;
                if (!(day.get("meals") instanceof List<?> meals)) throw unavailable();
                for (Object rawMeal:meals) {
                    Map<?,?> meal=map(rawMeal);
                    if (!mealType.name().equals(meal.get("mealType"))) continue;
                    Long recipeId=number(meal.get("recipeId")).longValueExact();
                    BigDecimal servings=meal.get("servingMultiplier")==null?BigDecimal.ONE:number(meal.get("servingMultiplier"));
                    if (servings.signum()<=0) throw unavailable();
                    Map<?,?> n=map(meal.get("nutrition"));
                    Nutrition nutrition=new Nutrition(number(n.get("calories")),number(n.get("protein")),
                            number(n.get("carbohydrate")),number(n.get("fat")));
                    String title=meal.get("recipeTitle") instanceof String s && !s.isBlank()?s:recipeTitle(recipeId,token);
                    return new MealSnapshot(recipeId,title,servings,nutrition);
                }
            }
            throw new AppException(ErrorCode.MEAL_PLAN_MEAL_NOT_FOUND);
        } catch (RestClientResponseException e) {
            if(e.getStatusCode().value()==401) throw new AppException(ErrorCode.UNAUTHENTICATED);
            if(e.getStatusCode().value()==403 || e.getStatusCode().value()==404)
                throw new AppException(ErrorCode.MEAL_PLAN_MEAL_NOT_FOUND);
            throw unavailable();
        } catch (RestClientException | IllegalArgumentException | ArithmeticException e) { throw unavailable(); }
    }
    private String recipeTitle(Long recipeId,String token) {
        try {
            Object title=result(http.get().uri(recipeUrl+"/recipes/{id}",recipeId)
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Map.class)).get("title");
            if (!(title instanceof String s) || s.isBlank() || s.length()>200) throw unavailable();
            return s;
        } catch (RestClientResponseException e) {
            // The nutrition in a saved plan remains usable after the original recipe is removed.
            if(e.getStatusCode().value()==404) return "Recipe #"+recipeId;
            throw e;
        }
    }
    private static Map<?,?> result(Map<?,?> envelope) {
        if(envelope==null || !(envelope.get("code") instanceof Number code) || code.intValue()!=1000) throw unavailable();
        return map(envelope.get("result"));
    }
    private static Map<?,?> map(Object value) { if(value instanceof Map<?,?> m) return m; throw unavailable(); }
    private static BigDecimal number(Object value) {
        if(!(value instanceof Number)) throw unavailable();
        BigDecimal n=new BigDecimal(value.toString()); if(n.signum()<0 || n.precision()>16) throw unavailable(); return n;
    }
    private static AppException unavailable() { return new AppException(ErrorCode.DIARY_SOURCE_UNAVAILABLE); }
}
