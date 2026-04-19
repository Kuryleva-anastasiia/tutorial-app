package com.aiAgent.tutorial_app.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class OcrService {

    @Value("${tesseract.data-path}")
    private String tessDataPath;

    /**
     * Распознать текст с изображения
     * @param imageFile загруженный файл изображения
     * @return распознанный текст
     */
    public String extractTextFromImage(MultipartFile imageFile) throws IOException, TesseractException {
        // Проверяем, что файл не пустой
        if (imageFile.isEmpty()) {
            throw new IOException("Файл изображения пуст");
        }

        // Проверяем тип файла
        String contentType = imageFile.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !imageFile.getOriginalFilename().matches(".*\\.(jpg|jpeg|png|bmp|gif)$"))) {
            throw new IOException("Неподдерживаемый формат изображения. Используйте JPG, PNG, BMP или GIF");
        }

        // Конвертируем MultipartFile в BufferedImage
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
        if (image == null) {
            throw new IOException("Не удалось прочитать изображение. Возможно, файл поврежден");
        }

        // Настраиваем Tesseract
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);     // Путь к папке с языковыми данными
        tesseract.setLanguage("rus+eng");        // Русский + английский языки
        tesseract.setPageSegMode(1);             // Автоматическое определение страницы
        tesseract.setOcrEngineMode(1);            // Использовать LSTM нейросеть

        // Выполняем OCR
        String result = tesseract.doOCR(image);

        // Очищаем результат от лишних пробелов и пустых строк
        result = result.trim().replaceAll("\\s+", " ");

        if (result.isEmpty()) {
            throw new IOException("Не удалось распознать текст на изображении. Попробуйте более четкую фотографию");
        }

        return result;
    }

    /**
     * Предварительная обработка изображения для улучшения распознавания (опционально)
     * Можно добавить поворот, масштабирование, повышение контрастности
     */
    public byte[] preprocessImage(MultipartFile imageFile, int targetWidth, int targetHeight) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

        // Масштабируем изображение
        java.awt.Image scaled = original.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        output.getGraphics().drawImage(scaled, 0, 0, null);

        // Конвертируем обратно в байты
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(output, "jpg", baos);
        return baos.toByteArray();
    }
}