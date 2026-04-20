package com.aiAgent.tutorial_app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorialSession {
    private String id;
    private String originalText;
    private List<Step> steps = new ArrayList<>();
    private int currentStep;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private double progress;

    // Конструктор для быстрого создания
    public TutorialSession(String id, String originalText) {
        this.id = id;
        this.originalText = originalText;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.currentStep = 0;
        this.steps = new ArrayList<>();
    }

    // Метод для расчета прогресса
    public double getProgress() {
        if (steps == null || steps.isEmpty()) return 0;
        long completedCount = steps.stream().filter(Step::isCompleted).count();
        currentStep = (int)completedCount;
        return (double) completedCount / steps.size() * 100;
    }
}