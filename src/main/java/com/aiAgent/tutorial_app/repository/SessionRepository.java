package com.aiAgent.tutorial_app.repository;

import com.aiAgent.tutorial_app.model.TutorialSession;
import com.aiAgent.tutorial_app.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SessionRepository {

    @Autowired
    private FileStorageService fileStorageService;

    // Кэш в памяти для быстрого доступа
    private final ConcurrentHashMap<String, TutorialSession> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Инициализируем папку для данных
        fileStorageService.init();
        // Загружаем все сохраненные сессии
        sessions.putAll(fileStorageService.loadAllSessions());
        System.out.println("Загружено сессий: " + sessions.size());
    }

    /**
     * Сохранить сессию (в память и в файл)
     */
    public void save(TutorialSession session) {
        sessions.put(session.getId(), session);
        fileStorageService.saveSession(session);
    }

    /**
     * Найти сессию по ID
     */
    public TutorialSession findById(String id) {
        // Сначала пробуем из кэша
        TutorialSession session = sessions.get(id);

        // Если нет в кэше, пробуем загрузить из файла
        if (session == null && fileStorageService.sessionExists(id)) {
            session = fileStorageService.loadSession(id);
            if (session != null) {
                sessions.put(id, session);
            }
        }

        return session;
    }

    /**
     * Проверить существование сессии
     */
    public boolean exists(String id) {
        return sessions.containsKey(id) || fileStorageService.sessionExists(id);
    }

    /**
     * Удалить сессию
     */
    public void delete(String id) {
        sessions.remove(id);
        fileStorageService.deleteSession(id);
    }

    /**
     * Получить все сессии
     */
    public ConcurrentHashMap<String, TutorialSession> getAllSessions() {
        return sessions;
    }
}