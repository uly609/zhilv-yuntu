package com.zhilv.yuntu.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ItineraryResponse(
        String tripId,
        String destination,
        String summary,
        List<DayPlan> days,
        double estimatedBudget,
        BudgetBreakdown budgetBreakdown,
        List<String> tips,
        List<String> sourceNotes,
        TokenUsage tokenUsage
) {
    public record DayPlan(
            int dayIndex,
            LocalDate date,
            String theme,
            List<Spot> spots,
            List<Meal> meals,
            Hotel hotel,
            List<Transport> transport,
            List<String> notes
    ) {
    }

    public record Spot(
            String name,
            String startTime,
            String endTime,
            String description,
            double estimatedCost,
            String location,
            String imageUrl,
            String address,
            Double latitude,
            Double longitude,
            String poiId
    ) {
    }

    public record Meal(
            String name,
            String mealType,
            double estimatedCost,
            String notes
    ) {
    }

    public record Hotel(
            String name,
            String level,
            double estimatedCost,
            String location,
            String address,
            Double latitude,
            Double longitude
    ) {
    }

    public record Transport(
            String mode,
            String fromPlace,
            String toPlace,
            double estimatedCost,
            String duration,
            Double distanceKm,
            Integer estimatedMinutes
    ) {
    }

    public record BudgetBreakdown(
            double transport,
            double hotel,
            double meals,
            double tickets,
            double other,
            double total
    ) {
    }

    public record TokenUsage(
            int rewritePromptTokens,
            int rewriteCompletionTokens,
            int embeddingPromptTokens,
            int embeddingCompletionTokens,
            int plannerPromptTokens,
            int plannerCompletionTokens,
            int rerankPromptTokens,
            int rerankCompletionTokens
    ) {
        public static TokenUsage empty() {
            return new TokenUsage(0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    public record TripSaveRequest(
            String tripId,
            ItineraryResponse itinerary,
            String userId
    ) {
    }

    public record TripSaveResponse(
            String tripId,
            String message
    ) {
    }

    public record TripListResponse(
            List<ItineraryResponse> trips,
            int total
    ) {
    }

    public record TripDetailResponse(
            ItineraryResponse itinerary
    ) {
    }

    public record WeatherForecastResponse(
            String city,
            List<Map<String, String>> forecasts
    ) {
    }
}
