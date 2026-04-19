package com.aiAgent.tutorial_app.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final ChatClient chatClient;

    @Autowired
    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Разбивает текст инструкции на логические шаги
     */
    public List<String> splitIntoSeps(String instructionText) {
        // Временный fallback, если AI не работает
        // Просто разбиваем текст по точкам и переносам строк

        System.out.println("DEBUG: AI Service called with text: " + instructionText.substring(0, Math.min(100, instructionText.length())));

        try {
            String prompt = """
            Ты — помощник, который разбивает инструкции на понятные пошаговые действия.
            
            Правила:
            1. Каждый шаг должен начинаться с глагола в повелительном наклонении
            2. Шаги должны быть краткими (максимум 15 слов)
            3. Нумеруй шаги
            4. Не добавляй лишних комментариев
            5. Не придумывай шаги, которых нет в инструкции
            6. Если есть список продуктов или деталий с их количеством включай это в конец шага в скобках, иначе ничего не добавляй
            7. Не пропускай подготовительные шаги, если они есть
            
            Инструкция:
            """ + instructionText + """
            
            Ответь только шагами, каждый шаг с новой строки.
            Например:
            1. Добавить муку (200гр)
            2. Добавить воды (100мл)
            3. Хорошо размешать
            
            Второй пример:
            1. Прикрутить шурупы к дверце (2шт)
            2. Соеденить основание и дверцу
            """;

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            System.out.println("DEBUG: AI Response: " + response);

            // Парсим ответ
            return Arrays.stream(response.split("\\n"))
                    .filter(line -> line.matches("^\\d+\\..*"))
                    .map(line -> line.replaceFirst("^\\d+\\.\\s*", ""))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println("ERROR: AI failed, using fallback");
            e.printStackTrace();

            // FALLBACK: разбиваем по предложениям
            String[] sentences = instructionText.split("[.!?\\n]+");
            List<String> steps = new ArrayList<>();
            for (String sentence : sentences) {
                String step = sentence.trim();
                if (!step.isEmpty()) {
                    steps.add(step);
                }
            }
            return steps;
        }
    }

    /**
     * Генерирует совет при возникновении проблемы на определенном шаге
     */
    public String getAdvice(String stepDescription, String problemDescription) {
        String prompt = """
            Ты — дружелюбный помощник по сборке/готовке.
            
            Пользователь выполняет шаг: "%s"
            У него возникла проблема: "%s"
            
            Дай короткий, конкретный совет (2-3 предложения), как решить эту проблему.
            Будь полезным и понятным.
            """.formatted(stepDescription, problemDescription);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}