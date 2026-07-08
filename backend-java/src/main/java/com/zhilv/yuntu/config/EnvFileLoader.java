package com.zhilv.yuntu.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnvFileLoader implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Path path : candidatePaths()) {
            if (Files.exists(path)) {
                readEnvFile(path, values);
            }
        }
        if (!values.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("localEnvFiles", values));
        }
    }

    private List<Path> candidatePaths() {
        return List.of(
                Path.of(".env"),
                Path.of("backend-java/.env"),
                Path.of("../backend/.env"),
                Path.of("backend/.env")
        );
    }

    private void readEnvFile(Path path, Map<String, Object> values) {
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int index = trimmed.indexOf('=');
                String key = trimmed.substring(0, index).trim();
                String value = trimmed.substring(index + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.putIfAbsent(key, value);
            }
        } catch (IOException ignored) {
            // Keep startup resilient. Missing or unreadable local env files should not stop the app.
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
