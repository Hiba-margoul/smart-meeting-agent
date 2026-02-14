package com.hiba.meeting_backend.model;

import java.util.List;

public class ReportSectionContent {
    private String code;
    private String title;
    private String content;
    private Integer order;
    private String aiConfidence;
    private List<String> aiReviewHints;


    public String getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(String aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public List<String> getAiReviewHints() {
        return aiReviewHints;
    }

    public void setAiReviewHints(List<String> aiReviewHints) {
        this.aiReviewHints = aiReviewHints;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

