package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.ReportChangeLog;
import com.sjtb.reporting.domain.ReportRecord;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.repository.ReportChangeLogRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReportAuditService {
    private final ReportChangeLogRepository logs;
    private final CurrentUserService current;
    public ReportAuditService(ReportChangeLogRepository logs, CurrentUserService current) { this.logs = logs; this.current = current; }
    public void record(ReportRecord record, String action, String before, String after, String reason) {
        User actor = current.current(); ReportChangeLog log = new ReportChangeLog(); log.setReportId(record.getId()); log.setTemplateId(record.getTemplate().getId()); log.setActorId(actor.getId()); log.setActorName(actor.getUsername()); log.setAction(action); log.setBeforeDataJson(before); log.setAfterDataJson(after); log.setReason(reason); logs.save(log);
    }
    public List<ReportChangeLog> list(ReportRecord record) { User user = current.current(); if (!user.getRoles().contains(com.sjtb.reporting.domain.Role.ADMIN) && !user.getRoles().contains(com.sjtb.reporting.domain.Role.LEADER) && !record.getReporter().getId().equals(user.getId())) throw new com.sjtb.reporting.exception.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "You cannot view this report log"); return logs.findByReportIdOrderByCreatedAtDesc(record.getId()); }
}
