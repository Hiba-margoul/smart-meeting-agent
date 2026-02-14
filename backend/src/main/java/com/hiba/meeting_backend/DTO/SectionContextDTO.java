package com.hiba.meeting_backend.DTO;

public class SectionContextDTO {
    private String code;      // Ex: "INTRO"
    private String title;     // Ex: "Introduction"
    private String guidance;  // Ex: "Résume les points clés..."

    // Constructeurs, Getters et Setters
    public SectionContextDTO(String code, String title, String guidance) {
        this.code = code;
        this.title = title;
        this.guidance = guidance;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGuidance() { return guidance; }
    public void setGuidance(String guidance) { this.guidance = guidance; }
}
