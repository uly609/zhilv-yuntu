package com.zhilv.yuntu.controller;

import com.zhilv.yuntu.dto.ItineraryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {
    @GetMapping("/forecast")
    public ItineraryResponse.WeatherForecastResponse forecast(@RequestParam String city) {
        return new ItineraryResponse.WeatherForecastResponse(
                city,
                List.of(
                        Map.of("date", LocalDate.now().toString(), "weather", "多云", "tip", "Java 后端占位天气，后续可接入高德天气 API"),
                        Map.of("date", LocalDate.now().plusDays(1).toString(), "weather", "晴", "tip", "适合城市步行与拍照"),
                        Map.of("date", LocalDate.now().plusDays(2).toString(), "weather", "阴", "tip", "建议随身携带雨具")
                )
        );
    }
}
