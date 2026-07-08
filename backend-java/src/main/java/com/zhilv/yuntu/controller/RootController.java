package com.zhilv.yuntu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "name", "zhilv-yuntu-java",
                "backend", "Spring Boot",
                "status", "ok"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok");
    }
}
