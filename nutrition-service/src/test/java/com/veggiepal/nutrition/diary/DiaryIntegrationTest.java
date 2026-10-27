package com.veggiepal.nutrition.diary;

import com.veggiepal.nutrition.configuration.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static com.veggiepal.nutrition.diary.DiaryDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:diary;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class DiaryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DiaryService service;
    @Autowired MealDiaryRepository meals;
    @Autowired WaterDiaryRepository water;
    @Autowired ActivityDiaryRepository activities;
    @MockitoBean MealPlanSnapshotClient plans;
    @MockitoBean(name="diaryClock") Clock clock;
    static final LocalDate DAY=LocalDate.of(2026,10,26);
    @BeforeEach void setup() {
        meals.deleteAll();water.deleteAll();activities.deleteAll();
        when(clock.instant()).thenReturn(Instant.parse("2026-10-27T18:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of(DiaryConfig.TIMEZONE));
    }
    private MealSnapshot snapshot() { return new MealSnapshot(5L,"Tofu",new BigDecimal("2"),
            new Nutrition(new BigDecimal("600"),new BigDecimal("40"),new BigDecimal("80"),new BigDecimal("20"))); }
    @Test void confirmationIsIdempotentSnapshotAndSummaryCountsOnlyConfirmedMeals() {
        when(plans.fetch("token",10L,DAY,MealType.LUNCH)).thenReturn(snapshot());
        var request=new ConfirmMealRequest(DAY,BigDecimal.ONE);
        assertThat(service.summary(7L,DAY).nutrition().calories()).isZero();
        var first=service.confirm(7L,"token",10L,MealType.LUNCH,request);
        var retry=service.confirm(7L,"token",10L,MealType.LUNCH,new ConfirmMealRequest(DAY,new BigDecimal("3")));
        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(retry.servings()).isEqualByComparingTo("1");
        assertThat(service.summary(7L,DAY).nutrition().calories()).isEqualByComparingTo("300");
        assertThat(service.summary(8L,DAY).meals()).isEmpty();
        verify(plans,times(1)).fetch("token",10L,DAY,MealType.LUNCH);
        service.cancel(8L,10L,MealType.LUNCH,DAY);
        assertThat(meals.count()).isEqualTo(1);
        service.cancel(7L,10L,MealType.LUNCH,DAY);service.cancel(7L,10L,MealType.LUNCH,DAY);
        assertThat(service.summary(7L,DAY).nutrition().calories()).isZero();
    }
    @Test void concurrentConfirmationsCreateOneDatabaseRow() throws Exception {
        when(plans.fetch("token",10L,DAY,MealType.LUNCH)).thenReturn(snapshot());
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(4)) {
            var calls=new ArrayList<Future<MealEntry>>();
            for(int i=0;i<4;i++) calls.add(executor.submit(() -> {start.await();return service.confirm(7L,"token",10L,MealType.LUNCH,new ConfirmMealRequest(DAY,null));}));
            start.countDown();var ids=new HashSet<Long>();
            for(var call:calls) ids.add(call.get(15,TimeUnit.SECONDS).id());
            assertThat(ids).hasSize(1);assertThat(meals.count()).isEqualTo(1);
        }
    }
    @Test void vietnamTimezoneAndWaterActivityTotalsDoNotChangeCalories() {
        var w=service.saveWater(7L,null,new WaterRequest(null,250));
        assertThat(w.date()).isEqualTo(LocalDate.of(2026,10,28));
        service.saveWater(7L,w.id(),new WaterRequest(null,500));
        var a=service.saveActivity(7L,null,new ActivityRequest(null,"Walking",30));
        service.saveActivity(7L,a.id(),new ActivityRequest(null,"Walking",45));
        var summary=service.summary(7L,null);
        assertThat(summary.totalWaterMl()).isEqualTo(500);
        assertThat(summary.totalActivityMinutes()).isEqualTo(45);
        assertThat(summary.nutrition().calories()).isZero();
        assertThatThrownBy(() -> service.saveWater(8L,w.id(),new WaterRequest(null,1))).isInstanceOf(com.veggiepal.nutrition.exception.AppException.class);
        assertThatThrownBy(() -> service.deleteWater(8L,w.id())).isInstanceOf(com.veggiepal.nutrition.exception.AppException.class);
        assertThatThrownBy(() -> service.saveActivity(8L,a.id(),new ActivityRequest(null,"Run",5))).isInstanceOf(com.veggiepal.nutrition.exception.AppException.class);
        assertThatThrownBy(() -> service.deleteActivity(8L,a.id())).isInstanceOf(com.veggiepal.nutrition.exception.AppException.class);
        service.deleteWater(7L,w.id());service.deleteActivity(7L,a.id());
        assertThat(service.summary(7L,null).totalWaterMl()).isZero();
        assertThat(service.summary(7L,null).totalActivityMinutes()).isZero();
    }
    @Test void httpValidatesBodiesAndRequiresAuthentication() throws Exception {
        mvc.perform(get("/nutrition/me/diary")).andExpect(status().isUnauthorized());
        for(String amount:List.of("null","0","-5")) {
            mvc.perform(post("/nutrition/me/diary/water").with(jwt().jwt(j -> j.claim("userId",7L)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"milliliters\":"+amount+"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2011));
        }
        mvc.perform(post("/nutrition/me/diary/activities").with(jwt().jwt(j -> j.claim("userId",7L)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"activityType\":\" \",\"minutes\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2012));
        mvc.perform(put("/nutrition/me/diary/meal-plans/10/meals/LUNCH").with(jwt().jwt(j -> j.claim("userId",7L)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"servings\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2010));
        verifyNoInteractions(plans);
    }
}
