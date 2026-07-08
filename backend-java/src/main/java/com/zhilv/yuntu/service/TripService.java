package com.zhilv.yuntu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhilv.yuntu.dto.ItineraryResponse;
import com.zhilv.yuntu.dto.TripRequest;
import com.zhilv.yuntu.llm.DashScopeLlmClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
                只返回一个合法 JSON 对象，不要返回 Markdown，不要返回代码块，不要解释。
                必须严格遵守类型：days/spots/meals/transport/tips/source_notes 都必须是数组；hotel 和 budget_breakdown 必须是对象；金额必须是数字。
                如果没有地图 API 数据，address/image_url/poi_id 可以为 null，latitude/longitude/distance_km/estimated_minutes 可以为 null。
                根字段必须包含：trip_id, destination, summary, days, estimated_budget, budget_breakdown, tips, source_notes, token_usage。
                每个 day 必须包含：day_index, date, theme, spots, meals, hotel, transport, notes。
                每个 spot 必须包含：name, start_time, end_time, description, estimated_cost, location, image_url, address, latitude, longitude, poi_id。
                每个 meal 必须包含：name, meal_type, estimated_cost, notes。
                hotel 必须包含：name, level, estimated_cost, location, address, latitude, longitude。
                每个 transport 必须包含：mode, from_place, to_place, estimated_cost, duration, distance_km, estimated_minutes。
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

                请严格按这个 JSON 形状返回，注意 spots 一定是数组，不是数字：
                {
                  "trip_id": "trip_目的地_开始日期",
                  "destination": "目的地",
                  "summary": "行程概览",
                  "days": [
                    {
                      "day_index": 1,
                      "date": "2026-07-09",
                      "theme": "主题",
                      "spots": [
                        {
                          "name": "景点名",
                          "start_time": "10:00",
                          "end_time": "12:00",
                          "description": "景点描述",
                          "estimated_cost": 0,
                          "location": "城市",
                          "image_url": null,
                          "address": null,
                          "latitude": null,
                          "longitude": null,
                          "poi_id": null
                        }
                      ],
                      "meals": [{"name":"餐饮建议","meal_type":"午餐","estimated_cost":0,"notes":"说明"}],
                      "hotel": {"name":"酒店建议","level":"舒适型","estimated_cost":0,"location":"城市","address":null,"latitude":null,"longitude":null},
                      "transport": [{"mode":"打车","from_place":"出发点","to_place":"目的地","estimated_cost":0,"duration":"30 分钟","distance_km":null,"estimated_minutes":null}],
                      "notes": ["备注"]
                    }
                  ],
                  "estimated_budget": 0,
                  "budget_breakdown": {"transport":0,"hotel":0,"meals":0,"tickets":0,"other":0,"total":0},
                  "tips": ["提示"],
                  "source_notes": ["Java Spring Boot LLM planner"],
                  "token_usage": {"rewrite_prompt_tokens":0,"rewrite_completion_tokens":0,"embedding_prompt_tokens":0,"embedding_completion_tokens":0,"planner_prompt_tokens":0,"planner_completion_tokens":0,"rerank_prompt_tokens":0,"rerank_completion_tokens":0}
                }
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
            JsonNode normalized = normalizeJson(objectMapper.readTree(json), request);
            ItineraryResponse response = objectMapper.treeToValue(normalized, ItineraryResponse.class);
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

    private JsonNode normalizeJson(JsonNode root, TripRequest request) {
        ObjectNode object = root.isObject() ? (ObjectNode) root : objectMapper.createObjectNode();
        putIfMissing(object, "trip_id", "trip_" + request.destination() + "_" + request.startDate());
        putIfMissing(object, "destination", request.destination());
        putIfMissing(object, "summary", request.destination() + "旅行计划");
        if (!object.path("days").isArray()) {
            object.set("days", objectMapper.createArrayNode());
        }
        ArrayNode days = (ArrayNode) object.get("days");
        if (days.isEmpty()) {
            days.add(objectMapper.valueToTree(defaultDay(request, 1, request.startDate())));
        }
        for (int i = 0; i < days.size(); i++) {
            JsonNode dayNode = days.get(i);
            if (!dayNode.isObject()) {
                days.set(i, objectMapper.valueToTree(defaultDay(request, i + 1, request.startDate().plusDays(i))));
                continue;
            }
            ObjectNode day = (ObjectNode) dayNode;
            putIfMissing(day, "day_index", i + 1);
            putIfMissing(day, "date", request.startDate().plusDays(i).toString());
            putIfMissing(day, "theme", "轻松旅行");
            ensureArray(day, "spots", defaultSpot(request));
            ensureArray(day, "meals", defaultMeal(request));
            ensureObject(day, "hotel", defaultHotel(request));
            ensureArray(day, "transport", defaultTransport(request));
            ensureArray(day, "notes", "Java 后端已自动修正大模型返回结构");
        }
        ensureObject(object, "budget_breakdown", defaultBudget(request));
        putIfMissing(object, "estimated_budget", request.budget());
        ensureArray(object, "tips", "建议提前确认门票、交通和天气情况");
        ensureArray(object, "source_notes", "Java Spring Boot LLM planner");
        ensureObject(object, "token_usage", defaultTokenUsage());
        return object;
    }

    private void ensureArray(ObjectNode object, String field, Object defaultValue) {
        if (!object.path(field).isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            array.add(objectMapper.valueToTree(defaultValue));
            object.set(field, array);
        }
    }

    private void ensureObject(ObjectNode object, String field, Object defaultValue) {
        if (!object.path(field).isObject()) {
            object.set(field, objectMapper.valueToTree(defaultValue));
        }
    }

    private void putIfMissing(ObjectNode object, String field, String value) {
        if (!object.hasNonNull(field) || object.path(field).asText().isBlank()) {
            object.put(field, value);
        }
    }

    private void putIfMissing(ObjectNode object, String field, int value) {
        if (!object.hasNonNull(field) || !object.path(field).canConvertToInt()) {
            object.put(field, value);
        }
    }

    private void putIfMissing(ObjectNode object, String field, double value) {
        if (!object.hasNonNull(field) || !object.path(field).isNumber()) {
            object.put(field, value);
        }
    }

    private ItineraryResponse.DayPlan defaultDay(TripRequest request, int index, LocalDate date) {
        return new ItineraryResponse.DayPlan(
                index,
                date,
                "轻松旅行",
                List.of(defaultSpot(request)),
                List.of(defaultMeal(request)),
                defaultHotel(request),
                List.of(defaultTransport(request)),
                List.of("Java 后端已自动补全默认行程结构")
        );
    }

    private ItineraryResponse.Spot defaultSpot(TripRequest request) {
        return new ItineraryResponse.Spot(request.destination() + "核心景点", "10:00", "12:00", "结合你的偏好安排的核心景点。", 0, request.destination(), null, request.destination(), null, null, null);
    }

    private ItineraryResponse.Meal defaultMeal(TripRequest request) {
        return new ItineraryResponse.Meal("当地特色餐", "午餐", 0, safeList(request.dietaryPreferences()));
    }

    private ItineraryResponse.Hotel defaultHotel(TripRequest request) {
        return new ItineraryResponse.Hotel(request.destination() + " " + safeText(request.hotelLevel()) + "住宿", safeText(request.hotelLevel()), 0, request.destination(), null, null, null);
    }

    private ItineraryResponse.Transport defaultTransport(TripRequest request) {
        return new ItineraryResponse.Transport("公共交通/打车", request.destination() + " 出发点", request.destination() + "核心景点", 0, "30 分钟", null, null);
    }

    private ItineraryResponse.BudgetBreakdown defaultBudget(TripRequest request) {
        return new ItineraryResponse.BudgetBreakdown(0, 0, 0, 0, request.budget(), request.budget());
    }

    private ItineraryResponse.TokenUsage defaultTokenUsage() {
        return ItineraryResponse.TokenUsage.empty();
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
                request.destination(),
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
