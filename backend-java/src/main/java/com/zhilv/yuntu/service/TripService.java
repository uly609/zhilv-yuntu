package com.zhilv.yuntu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhilv.yuntu.dto.ItineraryResponse;
import com.zhilv.yuntu.dto.TripRequest;
import com.zhilv.yuntu.llm.DashScopeLlmClient;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TripService {
    private final DashScopeLlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final Map<String, ItineraryResponse> store = new ConcurrentHashMap<>();

    public TripService(DashScopeLlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public ItineraryResponse generate(TripRequest request) {
        String systemPrompt = """
                你是智旅云图的 Java Spring Boot 旅行规划 Agent。
                只返回合法 JSON，不要返回 Markdown，不要返回代码块。
                JSON 字段必须使用 snake_case，并且结构必须兼容：
                trip_id, destination, summary, days, estimated_budget, budget_breakdown, tips, source_notes, token_usage。
                days 中每天包含 day_index, date, theme, spots, meals, hotel, transport, notes。
                spots 包含 name, start_time, end_time, description, estimated_cost, location, image_url, address, latitude, longitude, poi_id。
                没有地图 API 数据时，address/latitude/longitude/poi_id 可以为 null。
                尽量根据用户预算、人数、饮食偏好和旅行节奏生成中文旅行计划。
                """;

        String userPrompt = """
                请生成一个结构化旅行计划。
                目的地：%s
                开始日期：%s
                结束日期：%s
                人数：%d
                总预算：%.2f
                偏好：%s
                节奏：%s
                饮食偏好：%s
                酒店等级：%s
                特殊备注：%s
                行程天数：%d
                """.formatted(
                request.destination(),
                request.startDate(),
                request.endDate(),
                request.travelers(),
                request.budget(),
                safeList(request.preferences()),
                safeText(request.pace()),
                safeList(request.dietaryPreferences()),
                safeText(request.hotelLevel()),
                safeText(request.specialNotes()),
                Math.max(1, ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1)
        );

        try {
            String json = stripCodeFence(llmClient.generateJson(systemPrompt, userPrompt));
            ItineraryResponse response = objectMapper.readValue(json, ItineraryResponse.class);
            if (response.tokenUsage() == null) {
                response = withEmptyTokenUsage(response);
            }
            return response;
        } catch (Exception e) {
            return fallback(request, e.getMessage());
        }
    }

    public ItineraryResponse edit(ItineraryResponse itinerary) {
        return itinerary;
    }

    public ItineraryResponse save(String tripId, ItineraryResponse itinerary) {
        store.put(tripId, itinerary);
        return itinerary;
    }

    public List<ItineraryResponse> list() {
        return new ArrayList<>(store.values());
    }

    public ItineraryResponse get(String tripId) {
        return store.get(tripId);
    }

    public void delete(String tripId) {
        store.remove(tripId);
    }

    private String safeList(List<String> values) {
        return values == null || values.isEmpty() ? "无" : String.join("、", values);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }

    private String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "").replaceFirst("^```", "");
            trimmed = trimmed.replaceFirst("```$", "").trim();
        }
        return trimmed;
    }

    private ItineraryResponse withEmptyTokenUsage(ItineraryResponse response) {
        return new ItineraryResponse(
                response.tripId(), response.destination(), response.summary(), response.days(),
                response.estimatedBudget(), response.budgetBreakdown(), response.tips(),
                response.sourceNotes(), ItineraryResponse.TokenUsage.empty()
        );
    }

    private ItineraryResponse fallback(TripRequest request, String reason) {
        double hotel = Math.max(200, request.budget() * 0.35);
        double meals = Math.max(150, request.budget() * 0.25);
        double transport = Math.max(100, request.budget() * 0.15);
        double tickets = Math.max(80, request.budget() * 0.10);
        double other = Math.max(100, request.budget() - hotel - meals - transport - tickets);
        double total = hotel + meals + transport + tickets + other;

        var spot = new ItineraryResponse.Spot(
                request.destination() + "核心景点",
                "10:00",
                "12:00",
                "Java Spring Boot 后端生成的兜底行程；大模型不可用时返回。",
                tickets,
                request.destination(),
                null,
                null,
                null,
                null,
                null
        );
        var meal = new ItineraryResponse.Meal("当地特色餐", "午餐", meals, safeText(request.dietaryPreferences() == null ? null : String.join("、", request.dietaryPreferences())));
        var hotelPlan = new ItineraryResponse.Hotel(request.destination() + " " + safeText(request.hotelLevel()) + "住宿", safeText(request.hotelLevel()), hotel, request.destination(), null, null, null);
        var transportPlan = new ItineraryResponse.Transport("公共交通/打车", request.destination() + " 出发点", request.destination() + "核心景点", transport, "30 分钟", null, null);
        var day = new ItineraryResponse.DayPlan(1, request.startDate(), "Java 后端兜底行程", List.of(spot), List.of(meal), hotelPlan, List.of(transportPlan), List.of("LLM 调用失败，原因：" + reason));

        return new ItineraryResponse(
                "trip_" + request.destination() + "_" + request.startDate(),
                request.destination(),
                "这是由 Spring Boot Java 后端返回的结构化旅行计划。",
                List.of(day),
                total,
                new ItineraryResponse.BudgetBreakdown(transport, hotel, meals, tickets, other, total),
                List.of("当前版本为 Java 后端第一版，后续可继续迁移 RAG、Rerank、高德地图补全。"),
                List.of("Java backend fallback response"),
                ItineraryResponse.TokenUsage.empty()
        );
    }
}
