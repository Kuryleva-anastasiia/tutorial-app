package com.aiAgent.tutorial_app.service;

import com.aiAgent.tutorial_app.model.TutorialSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileStorageService {

    @Value("${app.data.dir}")
    private String dataDir;

    private final ObjectMapper objectMapper;

    public FileStorageService() {
        this.objectMapper = new ObjectMapper();
        // Регистрируем модуль для работы с LocalDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Инициализация папки для хранения данных
     */
    public void init() {
        try {
            Path path = Paths.get(dataDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Создана папка для данных: " + dataDir);
            }
        } catch (IOException e) {
            System.err.println("Ошибка создания папки для данных: " + e.getMessage());
        }
    }

    /**
     * Сохранить сессию в файл
     */
    public void saveSession(TutorialSession session) {
        try {
            String fileName = session.getId() + ".json";
            Path filePath = Paths.get(dataDir, fileName);
            objectMapper.writeValue(filePath.toFile(), session);
            System.out.println("Сохранена сессия: " + session.getId());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения сессии " + session.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Загрузить сессию из файла
     */
    public TutorialSession loadSession(String sessionId) {
        try {
            String fileName = sessionId + ".json";
            Path filePath = Paths.get(dataDir, fileName);
            if (Files.exists(filePath)) {
                return objectMapper.readValue(filePath.toFile(), TutorialSession.class);
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки сессии " + sessionId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Загрузить все сохраненные сессии
     */
    public ConcurrentHashMap<String, TutorialSession> loadAllSessions() {
        ConcurrentHashMap<String, TutorialSession> sessions = new ConcurrentHashMap<>();

        try {
            Path path = Paths.get(dataDir);
            if (!Files.exists(path)) {
                return sessions;
            }

            File dir = path.toFile();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    try {
                        TutorialSession session = objectMapper.readValue(file, TutorialSession.class);
                        sessions.put(session.getId(), session);
                        System.out.println("Загружена сессия: " + session.getId());
                    } catch (IOException e) {
                        System.err.println("Ошибка загрузки файла " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки всех сессий: " + e.getMessage());
        }

        return sessions;
    }

    /**
     * Удалить сессию из файла
     */
    public void deleteSession(String sessionId) {
        try {
            String fileName = sessionId + ".json";
            Path filePath = Paths.get(dataDir, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("Удалена сессия: " + sessionId);
            }
        } catch (IOException e) {
            System.err.println("Ошибка удаления сессии " + sessionId + ": " + e.getMessage());
        }
    }

    /**
     * Проверить, существует ли сессия
     */
    public boolean sessionExists(String sessionId) {
        String fileName = sessionId + ".json";
        Path filePath = Paths.get(dataDir, fileName);
        return Files.exists(filePath);
    }
}