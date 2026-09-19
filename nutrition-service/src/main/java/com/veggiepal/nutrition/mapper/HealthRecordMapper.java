package com.veggiepal.nutrition.mapper;

import org.mapstruct.Mapper;

import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.entity.HealthRecord;

@Mapper(componentModel = "spring")
public interface HealthRecordMapper {

    HealthRecordResponse toHealthRecordResponse(HealthRecord healthRecord);
}
