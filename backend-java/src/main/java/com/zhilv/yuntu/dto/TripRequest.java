package com.zhilv.yuntu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record TripRequest(
        @NotBlank String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Min(1) int travelers,
        @Min(0) double budget,
        List<String> preferences,
        String pace,
        List<String> dietaryPreferences,
        String hotelLevel,
        String specialNotes
) {
}
