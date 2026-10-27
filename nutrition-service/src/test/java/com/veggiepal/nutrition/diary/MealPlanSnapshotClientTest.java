package com.veggiepal.nutrition.diary;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import com.veggiepal.nutrition.exception.*;
import com.veggiepal.nutrition.diary.DiaryDtos.MealType;

class MealPlanSnapshotClientTest {
    HttpServer server;
    String url;
    AtomicReference<String> authorization=new AtomicReference<>();
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        url="http://127.0.0.1:"+server.getAddress().getPort();server.start();
    }
    @AfterEach void cleanup() { server.stop(0); }
    void endpoint(String path,int status,String body) {
        server.createContext(path,e -> {
            authorization.set(e.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(status,bytes.length);
            try(var out=e.getResponseBody()) { out.write(bytes); }
        });
    }
    @Test void forwardsOwnerTokenAndReadsMealNutritionFromPlanSnapshot() {
        endpoint("/ai/meal-plans/10",200,"""
            {"code":1000,"result":{"id":10,"plan":{"days":[{"day":"MONDAY","meals":[
             {"mealType":"LUNCH","recipeId":5,"recipeTitle":"Tofu","servingMultiplier":2,
              "nutrition":{"calories":600,"protein":40,"carbohydrate":80,"fat":20}}]}]}}}
            """);
        var client=new MealPlanSnapshotClient(url,url);
        var result=client.fetch("owner-token",10L,LocalDate.of(2026,10,26),MealType.LUNCH);
        assertThat(authorization.get()).isEqualTo("Bearer owner-token");
        assertThat(result.recipeId()).isEqualTo(5);assertThat(result.title()).isEqualTo("Tofu");
        assertThat(result.plannedServings()).isEqualByComparingTo("2");
        assertThat(result.nutrition().calories()).isEqualByComparingTo("600");
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints={403,404})
    void someoneElsesPlanCannotBeConfirmed(int status) {
        endpoint("/ai/meal-plans/10",status,"{}");
        assertThatThrownBy(() -> new MealPlanSnapshotClient(url,url).fetch("other-token",10L,LocalDate.of(2026,10,26),MealType.LUNCH))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.MEAL_PLAN_MEAL_NOT_FOUND);
    }
    @Test void malformedSnapshotIsUnavailableInsteadOfZeroNutrition() {
        endpoint("/ai/meal-plans/10",200,"""
            {"code":1000,"result":{"plan":{"days":[{"day":"MONDAY","meals":[
            {"mealType":"LUNCH","recipeId":5,"recipeTitle":"Tofu","nutrition":{"calories":600}}]}]}}}
            """);
        assertThatThrownBy(() -> new MealPlanSnapshotClient(url,url).fetch("token",10L,LocalDate.of(2026,10,26),MealType.LUNCH))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.DIARY_SOURCE_UNAVAILABLE);
    }
}
