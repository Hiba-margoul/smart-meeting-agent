package com.hiba.meeting_backend.controller;

import com.hiba.meeting_backend.DTO.ChoixTemplateDTO;
import com.hiba.meeting_backend.Repository.ReportTemplateRepository;
import com.hiba.meeting_backend.model.Meet;
import com.hiba.meeting_backend.model.ReportTemplate;
import com.hiba.meeting_backend.service.MeetService;
import com.hiba.meeting_backend.service.ReportTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200",
        allowCredentials = "true")
@RequestMapping("/reportTemplate")
public class ReportTemplateController {
     private final ReportTemplateService reportTemplateService;
     private  final MeetService meetService;

    public ReportTemplateController(ReportTemplateService reportTemplateService, MeetService meetService) {
        this.reportTemplateService = reportTemplateService;
        this.meetService = meetService;
    }
    @GetMapping("/templates")
    public ResponseEntity<List<ReportTemplate>> getAllTemplates() {
        return ResponseEntity.ok(reportTemplateService.getAllTemplates());
    }

    @PostMapping("/create_template")
    public ResponseEntity<ReportTemplate> createTemplate(
            @RequestBody ReportTemplate template) {
        return ResponseEntity.ok(
                reportTemplateService.save(template)
        );
    }

    @PostMapping("/choix_template")
    public ResponseEntity<Meet> choixTemplates(@RequestBody ChoixTemplateDTO request) {

        String templateId = request.getTemplateId();
        String meetId = request.getMeetId();
        System.out.println("----------------meetId"+ meetId);
        System.out.println("--------------templateId"+ templateId);

        Meet meet = meetService.getMeetById(meetId);
        meet.setReportTemplateId(templateId);
        meetService.save(meet);

        ReportTemplate template = reportTemplateService.getReportTemplateById(templateId);

        if (template.getMeetIds() == null) {
            template.setMeetIds(new ArrayList<>());
        }


        if (!template.getMeetIds().contains(meetId)) {
            template.getMeetIds().add(meetId);
        }

        reportTemplateService.save(template);

        return ResponseEntity.ok(meet);
    }



}
