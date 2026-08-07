package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_template")
public class ReportTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String code;
    @Column(nullable = false, length = 128) private String name;
    @Column(length = 500) private String description;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String columnsJson;
    @Column(nullable = false) private boolean enabled = true;
    @Column(nullable = false, length = 20) private String status = "PUBLISHED";
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getColumnsJson() { return columnsJson; } public void setColumnsJson(String columnsJson) { this.columnsJson = columnsJson; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
}
