package com.veggiepal.nutrition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.nutrition.configuration.JwtConfig;
import com.veggiepal.nutrition.configuration.SecurityConfig;
import com.veggiepal.nutrition.configuration.SecurityExceptionHandler;
import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.service.HealthRecordService;

@WebMvcTest(HealthRecordController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class HealthRecordControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HealthRecordService healthRecordService;

    static RequestPostProcessor currentUser() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    @Test
    void createRecord_valid_passesRequestForCurrentUser() throws Exception {
        when(healthRecordService.createRecord(eq(USER_ID), any(HealthRecordRequest.class)))
                .thenReturn(HealthRecordResponse.builder().id(1L).bmi(new BigDecimal("22.5")).build());

        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170, "weightKg": 65}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.bmi").value(22.5));

        verify(healthRecordService).createRecord(eq(USER_ID), any(HealthRecordRequest.class));
    }

    @Test
    void createRecord_heightTooSmall_returnsInvalidHeight() throws Exception {
        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 10, "weightKg": 65}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    void createRecord_heightWithTwoDecimals_returnsInvalidHeight() throws Exception {
        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170.55, "weightKg": 65}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    void createRecord_missingWeight_returnsWeightRequired() throws Exception {
        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    void createRecord_malformedJson_returnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heightCm\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018))
                .andExpect(jsonPath("$.message").value("Invalid request data"));
    }

    @Test
    void getRecords_defaultPaging() throws Exception {
        mockMvc.perform(get("/nutrition/me/health-records").with(currentUser()))
                .andExpect(status().isOk());

        verify(healthRecordService).getRecords(USER_ID, 0, 20);
    }

    @Test
    void getRecords_nonNumericPage_returnsInvalidRequest() throws Exception {
        mockMvc.perform(get("/nutrition/me/health-records").param("page", "abc").with(currentUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }

    @Test
    void getLatestRecord_returnsRecord() throws Exception {
        when(healthRecordService.getLatestRecord(USER_ID))
                .thenReturn(HealthRecordResponse.builder().id(3L).bmi(new BigDecimal("22.5")).build());

        mockMvc.perform(get("/nutrition/me/health-records/latest").with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(3));
    }

    @Test
    void getLatestRecord_none_returns404() throws Exception {
        when(healthRecordService.getLatestRecord(USER_ID))
                .thenThrow(new AppException(ErrorCode.HEALTH_RECORD_NOT_EXISTED));

        mockMvc.perform(get("/nutrition/me/health-records/latest").with(currentUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2005));
    }

    @Test
    void updateRecord_valid_passesIdAndCurrentUser() throws Exception {
        when(healthRecordService.updateRecord(eq(USER_ID), eq(5L), any(HealthRecordRequest.class)))
                .thenReturn(HealthRecordResponse.builder().id(5L).bmi(new BigDecimal("24.2")).build());

        mockMvc.perform(put("/nutrition/me/health-records/5").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170, "weightKg": 70}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(5));

        verify(healthRecordService).updateRecord(eq(USER_ID), eq(5L), any(HealthRecordRequest.class));
    }

    @Test
    void updateRecord_nonNumericId_returnsInvalidRequest() throws Exception {
        mockMvc.perform(put("/nutrition/me/health-records/abc").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170, "weightKg": 70}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }

    @Test
    void updateRecord_invalidBody_returnsValidationCode() throws Exception {
        mockMvc.perform(put("/nutrition/me/health-records/5").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 300, "weightKg": 70}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002));
    }
}
