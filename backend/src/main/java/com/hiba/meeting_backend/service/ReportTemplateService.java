package com.hiba.meeting_backend.service;

import com.hiba.meeting_backend.Repository.ReportSectionRepository;
import com.hiba.meeting_backend.Repository.ReportTemplateRepository;
import com.hiba.meeting_backend.model.Meet;
import com.hiba.meeting_backend.model.ReportSection;
import com.hiba.meeting_backend.model.ReportTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ReportTemplateService {
    private final ReportTemplateRepository reportTemplateRepository;
    private final MeetService meetService;

    public ReportTemplateService(ReportTemplateRepository reportTemplateRepository, MeetService meetService) {
        this.reportTemplateRepository = reportTemplateRepository;
        this.meetService = meetService;
    }

    public ReportTemplate saveTemplate(ReportTemplate template) {

        // 1️⃣ Sécurité : trier les sections par ordre
        if (template.getSections() != null) {
            List<ReportSection> sortedSections = template.getSections()
                    .stream()
                    .sorted(Comparator.comparingInt(ReportSection::getOrder))
                    .toList();

            template.setSections(sortedSections);
        }
        return reportTemplateRepository.save(template);
}
    public List<ReportTemplate> getAllTemplates() {
        return reportTemplateRepository.findAll();
    }
    public ReportTemplate save(ReportTemplate template) {

        ReportTemplate savedTemplate = reportTemplateRepository.save(template);


        if (savedTemplate.getMeetIds() == null || savedTemplate.getMeetIds().isEmpty()) {
            return savedTemplate;
        }
        for (String meetId : savedTemplate.getMeetIds()) {

            Meet meet = meetService.getMeetById(meetId);

            if (meet != null) {
                meet.setReportTemplateId(savedTemplate.getId());
                meetService.save(meet);
            }
        }

        return savedTemplate;
    }


    public ReportTemplate getReportTemplateById(String id) {
        return reportTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template non trouvé"));
    }

}
