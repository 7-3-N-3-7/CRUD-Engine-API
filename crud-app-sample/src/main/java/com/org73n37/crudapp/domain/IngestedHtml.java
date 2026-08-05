package com.org73n37.crudapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingested_html")
public class IngestedHtml {

    @Id
    @Column(name = "tracking_id", length = 36, nullable = false)
    private String trackingId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "html_content", columnDefinition = "TEXT", nullable = false)
    private String htmlContent;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    public IngestedHtml() {
    }

    public IngestedHtml(String trackingId, String userId, String htmlContent, String status) {
        this.trackingId = trackingId;
        this.userId = userId;
        this.htmlContent = htmlContent;
        this.status = status;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
