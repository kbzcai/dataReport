package com.sjtb.reporting.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "report_import_error", indexes = @Index(name = "idx_import_error_batch", columnList = "batch_id"))
public class ReportImportError {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "batch_id") private ReportImportBatch batch;
    @Column(length = 128) private String sheetName;
    @Column(name = "excel_row_number") private Integer rowNumber;
    @Column(nullable = false, length = 1000) private String message;
    public Long getId() { return id; }
    public ReportImportBatch getBatch() { return batch; }
    public void setBatch(ReportImportBatch value) { batch = value; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String value) { sheetName = value; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer value) { rowNumber = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
}
