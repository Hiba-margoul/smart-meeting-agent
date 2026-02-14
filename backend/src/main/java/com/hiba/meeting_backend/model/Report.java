package com.hiba.meeting_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "report")
public class Report {

    @Id
    private String id;
    @Indexed(unique = true)
    private String meetId;
    private String templateId;
    private String title;
    private Date createdAt = new Date(); // date de création
    private List<ReportSectionContent> sections;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMeetId() {
        return meetId;
    }

    public void setMeetId(String meetId) {
        this.meetId = meetId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<ReportSectionContent> getSections() {
        return sections;
    }

    public void setSections(List<ReportSectionContent> sections) {
        this.sections = sections;
    }
}
