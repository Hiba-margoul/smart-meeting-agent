package com.hiba.meeting_backend.DTO;

import java.util.List;

public class ReportTemplateDTO {
    private String id;
    private String name;
    private String description;
    private List<ReportSectionDTO> sections;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ReportSectionDTO> getSections() {
        return sections;
    }

    public void setSections(List<ReportSectionDTO> sections) {
        this.sections = sections;
    }
}
