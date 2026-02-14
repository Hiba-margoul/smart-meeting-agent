package com.hiba.meeting_backend.controller;

import com.hiba.meeting_backend.Repository.MeetRepository;
import com.hiba.meeting_backend.Repository.ReportRepository;
import com.hiba.meeting_backend.model.Meet;
import com.hiba.meeting_backend.model.Report;
import com.hiba.meeting_backend.service.MeetService;
import com.hiba.meeting_backend.service.ReportPdfService;
import com.hiba.meeting_backend.service.ReportService;
import com.hiba.meeting_backend.service.SseNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200",
        allowCredentials = "true")
public class ReportController {

    private final ReportService reportService;
    private final MeetService meetService;
    private final MeetRepository meetRepository;
    private final ReportPdfService pdfService;
    private final ReportRepository reportRepository;
    private final SseNotificationService sseService;


    public ReportController(ReportService reportService, MeetService meetService, MeetRepository meetRepository, ReportPdfService pdfService, ReportRepository reportRepository,SseNotificationService sseService) {
        this.reportService = reportService;
        this.meetService = meetService;
        this.meetRepository = meetRepository;
        this.pdfService = pdfService;
        this.reportRepository = reportRepository;
        this.sseService = sseService;
    }
    // SSE endpoint


    // récupérer un rapport existant (pour remplir le formulaire)
    @GetMapping("/{meetId}")
    public ResponseEntity<Report> getReport(@PathVariable String meetId) {
        return ResponseEntity.ok(reportService.getReportByMeet(meetId));
    }

    @GetMapping("/{meetId}/pdf")
    public ResponseEntity<byte[]> getReportPdf(@PathVariable String meetId) {
        byte[] pdf = pdfService.generatePdf(meetId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/")
    public ResponseEntity<List<Report>> getAllReports() {
        return  ResponseEntity.ok(reportService.getAllReports());
    }


    // créer ou modifier un rapport
    @PostMapping("/save_or_update")
    public ResponseEntity<Report> saveOrUpdateReport(@RequestBody Report report) {
        // Logger instance
        Logger logger = LoggerFactory.getLogger(this.getClass());
        System.out.println("🔥🔥🔥 LE CONTROLLER EST ATTEINT ! 🔥🔥🔥");
        System.out.println("Titre reçu : " + report.getTitle());
        Optional<Report> existingReport = reportRepository.findByMeetId(report.getMeetId());

        if (existingReport.isPresent()) {
            report.setId(existingReport.get().getId());
            report.setCreatedAt(existingReport.get().getCreatedAt());
        }

        Report savedReport = reportService.saveOrUpdateReport(report);

         Meet meet = meetService.getMeetById(report.getMeetId());
         meet.setReportId(report.getId());
         meet.setReportGenerated(true);
         meet.setStatus(Meet.MeetingStatus.FINISHED);
         meetRepository.save(meet);
         Meet meetUpdated = meetRepository.findById(meet.getId()).orElse(null);
        sseService.notifyUpdate(meetUpdated);

        // Use logger instead of sysout
        logger.info("Report saved/updated with ID: {}", savedReport.getId());

        return ResponseEntity.ok(savedReport);
    }

}
