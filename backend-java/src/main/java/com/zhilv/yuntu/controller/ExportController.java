package com.zhilv.yuntu.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.zhilv.yuntu.dto.ItineraryResponse;
import com.zhilv.yuntu.service.TripService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/export")
public class ExportController {
    private final TripService tripService;

    public ExportController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/{tripId}/markdown")
    public ResponseEntity<byte[]> markdown(@PathVariable String tripId) {
        ItineraryResponse itinerary = tripService.get(tripId);
        if (itinerary == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = toMarkdown(itinerary).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(tripId + ".md").build().toString())
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(bytes);
    }

    @GetMapping("/{tripId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String tripId) {
        ItineraryResponse itinerary = tripService.get(tripId);
        if (itinerary == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph("Zhilv Yuntu Itinerary"));
            document.add(new Paragraph(itinerary.destination()));
            document.add(new Paragraph(itinerary.summary()));
            document.close();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(tripId + ".pdf").build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(outputStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String toMarkdown(ItineraryResponse itinerary) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(itinerary.destination()).append("旅行计划\n\n");
        builder.append(itinerary.summary()).append("\n\n");
        if (itinerary.days() != null) {
            for (ItineraryResponse.DayPlan day : itinerary.days()) {
                builder.append("## 第 ").append(day.dayIndex()).append(" 天：").append(day.theme()).append("\n\n");
                if (day.spots() != null) {
                    for (ItineraryResponse.Spot spot : day.spots()) {
                        builder.append("- ").append(spot.startTime()).append("-").append(spot.endTime()).append(" ").append(spot.name()).append("：").append(spot.description()).append("\n");
                    }
                }
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
