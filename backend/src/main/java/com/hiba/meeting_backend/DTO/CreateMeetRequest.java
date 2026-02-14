package com.hiba.meeting_backend.DTO;

import java.util.List;

public class CreateMeetRequest {
    String title;
    String meetId;
    private List<String> invitedUserIds;
    String reportTemplateId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMeetId() {
        return meetId;
    }

    public void setMeetId(String meetId) {
        this.meetId = meetId;
    }

    public List<String> getInvitedUserIds() {
        return invitedUserIds;
    }

    public void setInvitedUserIds(List<String> invitedUserIds) {
        this.invitedUserIds = invitedUserIds;
    }

    public String getReportTemplateId() {
        return reportTemplateId;
    }

    public void setReportTemplateId(String reportTemplateId) {
        this.reportTemplateId = reportTemplateId;
    }
}
