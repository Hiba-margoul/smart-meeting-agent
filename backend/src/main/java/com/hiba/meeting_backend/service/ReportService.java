package com.hiba.meeting_backend.service;


import com.hiba.meeting_backend.Repository.ReportRepository;

import com.hiba.meeting_backend.model.Report;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }


    public Report saveOrUpdateReport(Report report) {
        return reportRepository.save(report); // save() fait create ou update automatiquement
    }

    // récupérer un rapport par meetId
    public Report getReportByMeet(String meetId) {
        return reportRepository.findByMeetId(meetId)
                .orElseThrow(() -> new RuntimeException("Report not found for meetId: " + meetId));
    }
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

}
