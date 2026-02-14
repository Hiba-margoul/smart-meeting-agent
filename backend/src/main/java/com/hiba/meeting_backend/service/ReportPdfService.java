package com.hiba.meeting_backend.service;

import com.hiba.meeting_backend.Repository.ReportRepository;
import com.hiba.meeting_backend.model.Report;
import com.hiba.meeting_backend.model.ReportSectionContent;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportPdfService {

    private final ReportRepository reportRepository;
    private static final Logger logger = LoggerFactory.getLogger(ReportPdfService.class);

    public ReportPdfService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public byte[] generatePdf(String meetId) {
        logger.info("Début génération PDF pour meetId: {}", meetId);

        Report report = reportRepository.findByMeetId(meetId)
                .orElseThrow(() -> new RuntimeException("Report not found for meetId: " + meetId));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String dateStr = (report.getCreatedAt() != null) ? sdf.format(report.getCreatedAt()) : "N/A";

        // --- 1. DESIGN CSS MODERNE ---
        StringBuilder html = new StringBuilder();
        html.append("<html><head>")
                .append("<meta charset='UTF-8'/>")
                .append("<style>")
                .append("@page { size: A4; margin: 20mm; }")
                .append("body { font-family: 'Helvetica', sans-serif; color: #333; line-height: 1.6; font-size: 12px; }")

                // Header
                .append(".header { text-align: center; margin-bottom: 40px; border-bottom: 2px solid #0056b3; padding-bottom: 20px; }")
                .append(".header h1 { color: #0056b3; font-size: 24px; margin: 0; text-transform: uppercase; letter-spacing: 1px; }")
                .append(".meta-info { color: #666; font-size: 11px; margin-top: 10px; }")

                // Sections
                .append(".section { margin-bottom: 25px; background-color: #f9f9f9; padding: 15px; border-left: 5px solid #007bff; border-radius: 4px; page-break-inside: avoid; }")
                .append(".section h2 { color: #0056b3; font-size: 16px; margin-top: 0; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-bottom: 10px; }")

                // Content styles
                .append(".content { text-align: justify; }")
                .append(".content strong { color: #000; font-weight: bold; }")
                .append(".content h3 { color: #444; font-size: 14px; margin-top: 10px; margin-bottom: 5px; }")
                .append(".content ul { margin: 5px 0 5px 20px; padding: 0; }")
                .append(".content li { margin-bottom: 3px; }")

                // --- TABLE STYLES (NEW) ---
                .append("table { width: 100%; border-collapse: collapse; margin: 15px 0; font-size: 11px; }")
                .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
                .append("th { background-color: #0056b3; color: white; font-weight: bold; text-transform: uppercase; }")
                .append("tr:nth-child(even) { background-color: #f2f2f2; }")
                .append("tr:hover { background-color: #ddd; }")

                // Footer
                .append(".footer { position: fixed; bottom: 0; left: 0; right: 0; text-align: center; font-size: 10px; color: #aaa; border-top: 1px solid #eee; padding-top: 10px; }")
                .append("</style>")
                .append("</head><body>");

        // --- 2. PDF CONTENT ---
        html.append("<div class='header'>");
        html.append("<h1>").append(escapeHtml(report.getTitle())).append("</h1>");
        html.append("<div class='meta-info'>");
        html.append("<span>DATE : ").append(dateStr).append("</span> | ");
        html.append("<span>REF : ").append(escapeHtml(report.getMeetId())).append("</span>");
        html.append("</div>");
        html.append("</div>");

        if (report.getSections() != null && !report.getSections().isEmpty()) {
            for (ReportSectionContent section : report.getSections()) {
                // DO NOT escape HTML here yet. We need raw text to detect Markdown tables.
                // We will escape cell content individually.
                String formattedContent = convertMarkdownToHtml(section.getContent());

                html.append("<div class='section'>")
                        .append("<h2>").append(escapeHtml(section.getTitle())).append("</h2>")
                        .append("<div class='content'>").append(formattedContent).append("</div>")
                        .append("</div>");
            }
        } else {
            html.append("<p style='text-align:center; color:#999;'><i>Aucun contenu disponible.</i></p>");
        }

        html.append("<div class='footer'>Généré automatiquement par Smart Meeting Agent</div>");
        html.append("</body></html>");

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html.toString(), "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            logger.error("Erreur génération PDF", e);
            throw new RuntimeException("Erreur PDF: " + e.getMessage(), e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Converts Markdown to HTML, handling Tables specifically.
     */
    private String convertMarkdownToHtml(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder html = new StringBuilder();
        String[] lines = text.split("\n");
        boolean inTable = false;
        List<String> tableBuffer = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();

            // Detect table rows: must start and end with | or contain at least one |
            if (trimmed.startsWith("|") || (trimmed.contains("|") && trimmed.length() > 5)) {
                if (!inTable) {
                    inTable = true;
                }
                tableBuffer.add(trimmed);
            } else {
                // If we were in a table and now hit a non-table line, process the buffered table
                if (inTable) {
                    html.append(processTable(tableBuffer));
                    tableBuffer.clear();
                    inTable = false;
                }

                // Process standard Markdown lines
                html.append(processStandardLine(line));
            }
        }

        // Flush remaining table if text ends with a table
        if (inTable && !tableBuffer.isEmpty()) {
            html.append(processTable(tableBuffer));
        }

        return html.toString();
    }

    private String processStandardLine(String line) {
        if (line.trim().isEmpty()) return "";

        // Escape HTML for safety first
        String safeLine = escapeHtml(line);

        // Bold
        safeLine = safeLine.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        // Headers
        if (safeLine.startsWith("##")) {
            return "<h3>" + safeLine.substring(safeLine.lastIndexOf("#") + 1).trim() + "</h3>";
        }

        // Lists
        if (safeLine.trim().startsWith("- ")) {
            return "<li>" + safeLine.trim().substring(2) + "</li>";
        }

        // Standard paragraph line
        return "<p>" + safeLine + "</p>";
    }

    private String processTable(List<String> tableLines) {
        StringBuilder tableHtml = new StringBuilder();
        tableHtml.append("<table>");

        boolean headerProcessed = false;

        for (String row : tableLines) {
            // Skip separator lines like |---|---| used in markdown tables
            if (row.contains("---")) continue;

            // Remove leading/trailing pipes if present (common in markdown tables)
            String cleanRow = row.trim();
            if (cleanRow.startsWith("|")) {
                cleanRow = cleanRow.substring(1);
            }
            if (cleanRow.endsWith("|")) {
                cleanRow = cleanRow.substring(0, cleanRow.length() - 1);
            }

            String[] cells = cleanRow.split("\\|");

            // OPEN THE ROW
            tableHtml.append("<tr>");

            for (String cell : cells) {
                String cellContent = escapeHtml(cell.trim()); // Escape content for safety

                if (!headerProcessed) {
                    tableHtml.append("<th>").append(cellContent).append("</th>");
                } else {
                    tableHtml.append("<td>").append(cellContent).append("</td>");
                }
            }

            // CLOSE THE ROW (This was likely the missing part or conditional logic error)
            tableHtml.append("</tr>");

            // After the first iteration, we assume headers are done
            headerProcessed = true;
        }

        tableHtml.append("</table>");
        return tableHtml.toString();
    }
}