package com.sjtb.reporting.config;

import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.repository.ReportTemplateRepository;
import com.sjtb.reporting.repository.ReportTemplateVersionRepository;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapData {
    @Bean CommandLineRunner initializeSystem(UserRepository users, ReportTemplateRepository templates, ReportTemplateVersionRepository versions, PasswordEncoder encoder, ObjectMapper mapper,
                                        @Value("${app.bootstrap-admin.username}") String username,
                                        @Value("${app.bootstrap-admin.password}") String password) {
        return args -> {
            if (username != null && !username.isBlank() && password != null && !password.isBlank() && !users.existsByUsername(username)) {
                User admin = new User(); admin.setUsername(username); admin.setPassword(encoder.encode(password)); admin.setRoles(Set.of(Role.ADMIN)); users.save(admin);
            }
            ReportTemplate template = templates.findByCode("monthly_operation").orElseGet(ReportTemplate::new);
            template.setCode("monthly_operation");
            template.setName("月度经营填报");
            template.setDescription("月度经营情况填报模板");
            template.setEnabled(true);
            template.setColumnsJson(mapper.writeValueAsString(List.of(
                Map.of("key", "report_month", "label", "填报月份", "type", "month", "required", true),
                Map.of("key", "organization_name", "label", "单位名称", "type", "text", "required", true),
                Map.of("key", "operating_revenue", "label", "营业收入（元）", "type", "number", "required", true),
                Map.of("key", "operating_cost", "label", "营业成本（元）", "type", "number", "required", true),
                Map.of("key", "total_profit", "label", "利润总额（元）", "type", "number", "required", true),
                Map.of("key", "employee_count", "label", "从业人数", "type", "number", "required", true),
                Map.of("key", "remark", "label", "备注", "type", "textarea", "required", false)
            )));
            template = templates.save(template); ensureVersion(template, versions);

            ReportTemplate annual = templates.findByCode("annual_operation").orElseGet(ReportTemplate::new);
            annual.setCode("annual_operation");
            annual.setName("年度经营填报");
            annual.setDescription("年度经营情况填报模板");
            annual.setEnabled(true);
            annual.setColumnsJson(mapper.writeValueAsString(List.of(
                Map.of("key", "report_year", "label", "填报年度", "type", "number", "required", true),
                Map.of("key", "organization_name", "label", "单位名称", "type", "text", "required", true),
                Map.of("key", "annual_revenue", "label", "年度营业收入（元）", "type", "number", "required", true),
                Map.of("key", "annual_cost", "label", "年度营业成本（元）", "type", "number", "required", true),
                Map.of("key", "annual_profit", "label", "年度利润总额（元）", "type", "number", "required", true),
                Map.of("key", "employee_count", "label", "年末从业人数", "type", "number", "required", true),
                Map.of("key", "remark", "label", "备注", "type", "textarea", "required", false)
            )));
            annual = templates.save(annual); ensureVersion(annual, versions);
        };
    }

    private void ensureVersion(ReportTemplate template, ReportTemplateVersionRepository versions) {
        ReportTemplateVersion current = versions.findTopByTemplateIdOrderByVersionNoDesc(template.getId()).orElse(null);
        if (current == null || !template.getColumnsJson().equals(current.getColumnsJson())) {
            ReportTemplateVersion next = new ReportTemplateVersion(); next.setTemplate(template); next.setVersionNo(current == null ? 1 : current.getVersionNo() + 1); next.setColumnsJson(template.getColumnsJson()); next.setStatus("ACTIVE"); versions.save(next);
        }
    }
}
