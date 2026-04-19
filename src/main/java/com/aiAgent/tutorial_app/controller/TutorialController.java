package com.aiAgent.tutorial_app.controller;

import com.aiAgent.tutorial_app.model.Step;
import com.aiAgent.tutorial_app.model.TutorialSession;
import com.aiAgent.tutorial_app.repository.SessionRepository;
import com.aiAgent.tutorial_app.service.AiService;
import com.aiAgent.tutorial_app.service.OcrService;

import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.aiAgent.tutorial_app.service.PdfExportService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class TutorialController {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AiService aiService;

    @Autowired
    private PdfExportService pdfExportService;

    @Autowired
    private OcrService ocrService;
    /**
     * Загрузка и обработка инструкции (из ТЗ)
     */



    @PostMapping("/api/tutorial/upload-web")
    @ResponseBody
    public Map<String, Object> uploadTutorialWeb(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {

        Map<String, Object> response = new HashMap<>();

        try {
            String text;

            // Обработка в зависимости от типа файла
            if ("text".equals(type)) {
                // Текстовый файл
                text = new String(file.getBytes(), StandardCharsets.UTF_8);
                System.out.println("Загружен текстовый файл, длина: " + text.length());

            } else if ("image".equals(type)) {
                // Изображение - используем OCR
                System.out.println("Распознаем изображение: " + file.getOriginalFilename());
                try {
                    text = ocrService.extractTextFromImage(file);
                    System.out.println("Распознанный текст: " + text.substring(0, Math.min(200, text.length())));

                    if (text.isEmpty()) {
                        response.put("success", false);
                        response.put("error", "Не удалось распознать текст на изображении. Попробуйте более четкую фотографию.");
                        return response;
                    }
                } catch (TesseractException e) {
                    System.err.println("OCR ошибка: " + e.getMessage());
                    response.put("success", false);
                    response.put("error", "Ошибка распознавания: " + e.getMessage());
                    return response;
                }
            } else {
                response.put("success", false);
                response.put("error", "Неизвестный тип файла: " + type);
                return response;
            }

            if (text == null || text.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Файл пуст или не содержит текста");
                return response;
            }

            // Разбиваем на шаги через AI
            List<String> stepDescriptions = aiService.splitIntoSeps(text);

            if (stepDescriptions.isEmpty()) {
                response.put("success", false);
                response.put("error", "Не удалось разбить инструкцию на шаги");
                return response;
            }

            // Создаем сессию
            String sessionId = UUID.randomUUID().toString();
            TutorialSession session = new TutorialSession(sessionId, text);

            for (int i = 0; i < stepDescriptions.size(); i++) {
                session.getSteps().add(new Step(i + 1, stepDescriptions.get(i), false));
            }

            sessionRepository.save(session);

            response.put("success", true);
            response.put("sessionId", sessionId);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * Страница с чек-листом для конкретной сессии
     */
    @GetMapping("/tutorial/{sessionId}")
    public String showTutorial(@PathVariable String sessionId, Model model) {
        System.out.println("DEBUG: Looking for session with ID: " + sessionId);

        TutorialSession session = sessionRepository.findById(sessionId);

        if (session == null) {
            System.out.println("DEBUG: Session not found!");
            model.addAttribute("error", "Сессия не найдена");
            return "index";
        }

        System.out.println("DEBUG: Session found, steps count: " + session.getSteps().size());
        System.out.println("DEBUG: Session ID in object: " + session.getId());

        // ВАЖНО: Добавляем сессию в модель с именем "session"
        model.addAttribute("session", session);

        // Для отладки - проверим что шаги точно есть
        for (Step step : session.getSteps()) {
            System.out.println("Step " + step.getNumber() + ": " + step.getDescription());
        }

        return "tutorial";
    }

    /**
     * Отметить шаг как выполненный (из ТЗ)
     */
    @PostMapping("/api/tutorial/session/{sessionId}/step/{stepNumber}/complete")
    @ResponseBody
    public String markStepComplete(
            @PathVariable String sessionId,
            @PathVariable int stepNumber) {

        TutorialSession session = sessionRepository.findById(sessionId);
        if (session == null) {
            return "error: session not found";
        }

        for (Step step : session.getSteps()) {
            if (step.getNumber() == stepNumber) {
                // Переключаем состояние (true -> false, false -> true)
                step.setCompleted(!step.isCompleted());
                break;
            }
        }
        session.setLastUpdated(java.time.LocalDateTime.now());
        sessionRepository.save(session);

        return "ok";
    }

    /**
     * Получить следующий шаг или совет (из ТЗ)
     */
    @GetMapping("/api/tutorial/session/{sessionId}/next-step")
    @ResponseBody
    public String getNextStepOrAdvice(
            @PathVariable String sessionId,
            @RequestParam(required = false) String problem) {

        TutorialSession session = sessionRepository.findById(sessionId);
        if (session == null) {
            return "Сессия не найдена";
        }

        // Находим первый невыполненный шаг
        Step currentStep = session.getSteps().stream()
                .filter(s -> !s.isCompleted())
                .findFirst()
                .orElse(null);

        if (currentStep == null) {
            return "Поздравляю! Вы выполнили все шаги! 🎉";
        }

        // Если есть проблема - запрашиваем совет
        if (problem != null && !problem.trim().isEmpty()) {
            return aiService.getAdvice(currentStep.getDescription(), problem);
        }

        // Иначе возвращаем следующий шаг
        return "Следующий шаг: " + currentStep.getDescription();
    }

    /**
     * Прогресс выполнения (для графика)
     */
    @GetMapping("/api/tutorial/session/{sessionId}/progress")
    @ResponseBody
    public double getProgress(@PathVariable String sessionId) {
        TutorialSession session = sessionRepository.findById(sessionId);
        if (session == null) {
            return 0;
        }
        return session.getProgress();
    }

    @GetMapping("/api/tutorial/session/{sessionId}/steps")
    @ResponseBody
    public List<Step> getSteps(@PathVariable String sessionId) {
        TutorialSession session = sessionRepository.findById(sessionId);
        return session != null ? session.getSteps() : List.of();
    }

    @PostMapping("/api/tutorial/session/{sessionId}/step/{stepNumber}/toggle")
    @ResponseBody
    public String toggleStep(
            @PathVariable String sessionId,
            @PathVariable int stepNumber,
            @RequestBody Map<String, Boolean> request) {

        TutorialSession session = sessionRepository.findById(sessionId);
        if (session == null) {
            return "error: session not found";
        }

        Boolean completed = request.get("completed");

        for (Step step : session.getSteps()) {
            if (step.getNumber() == stepNumber) {
                step.setCompleted(completed != null && completed);
                break;
            }
        }

        session.setLastUpdated(java.time.LocalDateTime.now());
        sessionRepository.save(session);

        return "ok";
    }

    /**
     * Экспорт чек-листа в PDF (из ТЗ)
     */
    @GetMapping("/api/tutorial/session/{sessionId}/export/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> exportToPdf(@PathVariable String sessionId) {
        try {
            TutorialSession session = sessionRepository.findById(sessionId);

            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = pdfExportService.exportToPdf(session);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "checklist-" + sessionId + ".pdf");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}