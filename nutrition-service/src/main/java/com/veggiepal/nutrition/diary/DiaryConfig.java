package com.veggiepal.nutrition.diary;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiaryConfig {
    public static final String TIMEZONE = "Asia/Ho_Chi_Minh";
    @Bean("diaryClock") Clock diaryClock() { return Clock.system(ZoneId.of(TIMEZONE)); }
}
