package com.hiba.meeting_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "meet")
public class Meet {
    @Id
    private String id;
    private String title;
    // C'est le nom unique utilisé par LiveKit pour identifier la room
    private String liveKitRoomName; // (ex: "daily-scrum-8845")
    private String hostId;
    private MeetingStatus status; // PLANNED, ACTIVE, FINISHED
    private Date createdAt;
    private Date startedAt;
    private Date endedAt;
    private List<String> invitedUserIds;
    private boolean reportGenerated = false;
    private String reportId;
    private  String reportTemplateId;

    public String getReportTemplateId() {
        return reportTemplateId;
    }

    public void setReportTemplateId(String reportTemplateId) {
        this.reportTemplateId = reportTemplateId;
    }

    public enum MeetingStatus {
        PLANNED, ACTIVE, FINISHED, CANCELLED
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLiveKitRoomName() {
        return liveKitRoomName;
    }

    public void setLiveKitRoomName(String liveKitRoomName) {
        this.liveKitRoomName = liveKitRoomName;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingStatus status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }

    public List<String> getInvitedUserIds() {
        return invitedUserIds;
    }

    public void setInvitedUserIds(List<String> invitedUserIds) {
        this.invitedUserIds = invitedUserIds;
    }

    public boolean isReportGenerated() {
        return reportGenerated;
    }

    public void setReportGenerated(boolean reportGenerated) {
        this.reportGenerated = reportGenerated;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
}
