package com.aiAgent.tutorial_app.service;

import com.aiAgent.tutorial_app.model.Step;
import com.aiAgent.tutorial_app.model.TutorialSession;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfExportService {

    public byte[] exportToPdf(TutorialSession session) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Загружаем шрифт из ресурсов
            InputStream fontStream = new ClassPathResource("fonts/arial.ttf").getInputStream();
            PDType0Font font = PDType0Font.load(document, fontStream);

            float margin = 50;
            float yPosition = page.getMediaBox().getHeight() - margin;

            // Заголовок
            contentStream.setFont(font, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Отчет о выполнении чек-листа");
            contentStream.endText();
            yPosition -= 30;

            // Информация
            contentStream.setFont(font, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Создан: " + formatDateTime(session.getCreatedAt()));
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Обновлен: " + formatDateTime(session.getLastUpdated()));
            contentStream.endText();
            yPosition -= 30;

            // Статистика
            int totalSteps = session.getSteps().size();
            long completedSteps = session.getSteps().stream().filter(Step::isCompleted).count();
            int percentComplete = totalSteps > 0 ? (int) ((double) completedSteps / totalSteps * 100) : 0;

            contentStream.setFont(font, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Выполнено: " + completedSteps + " из " + totalSteps + " шагов (" + percentComplete + "%)");
            contentStream.endText();
            yPosition -= 30;

            // Прогресс-бар
            int barWidth = 400;
            int filledWidth = (int) ((double) percentComplete / 100 * barWidth);

            // Фон прогресс-бара (светло-серый) - значения от 0 до 1
            contentStream.setStrokingColor(0.9f, 0.9f, 0.9f);
            contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
            contentStream.addRect(margin, yPosition - 10, barWidth, 15);
            contentStream.fill();

            // Заполнение прогресс-бара (зеленый) - значения от 0 до 1
            contentStream.setNonStrokingColor(0.3f, 0.7f, 0.3f);
            contentStream.addRect(margin, yPosition - 10, filledWidth, 15);
            contentStream.fill();

            // Текст процентов
            contentStream.setFont(font, 9);
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin + barWidth / 2 - 15, yPosition - 5);
            contentStream.showText(percentComplete + "%");
            contentStream.endText();

            yPosition -= 40;

            // Шаги
            contentStream.setFont(font, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Шаги:");
            contentStream.endText();
            yPosition -= 25;

            for (Step step : session.getSteps()) {
                if (yPosition < 50) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - margin;
                }

                String mark = step.isCompleted() ? "[ X ]" : "[   ]";
                contentStream.setFont(font, 11);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(mark + " " + step.getNumber() + ". " + step.getDescription());
                contentStream.endText();
                yPosition -= 20;
            }

            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) : "не указано";
    }
}