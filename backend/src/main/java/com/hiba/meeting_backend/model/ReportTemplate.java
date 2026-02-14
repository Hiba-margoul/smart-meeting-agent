package com.hiba.meeting_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "reportTemplate")
public class ReportTemplate {
    @Id
    private String id;
    private String name;
    private String createdBy;
    private String description;

    private List<ReportSection> sections;
    private List<String> meetIds;

    public List<String> getMeetIds() {
        return meetIds;
    }

    public void setMeetIds(List<String> meetIds) {
        this.meetIds = meetIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ReportSection> getSections() {
        return sections;
    }

    public void setSections(List<ReportSection> sections) {
        this.sections = sections;
    }


}
