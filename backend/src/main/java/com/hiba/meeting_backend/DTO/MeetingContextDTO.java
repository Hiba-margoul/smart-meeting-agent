package com.hiba.meeting_backend.DTO;

import java.util.List;

public class MeetingContextDTO {
    private String meetingTitle;
    private String meetId;
    private String meetingDate;
    private String duration;

    // --- Contexte du Template ---
    private String templateName;
    private String templateDescription;

    // --- Instructions structurelles ---
    private List<SectionContextDTO> sections;

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }

    public String getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(String meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public String getMeetId() {
        return meetId;
    }

    public void setMeetId(String meetId) {
        this.meetId = meetId;
    }

    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    public List<SectionContextDTO> getSections() {
        return sections;
    }

    public void setSections(List<SectionContextDTO> sections) {
        this.sections = sections;
    }
}
