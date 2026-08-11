package com.sjtb.reporting.controller;

import com.sjtb.reporting.dto.TemplateDtos;
import com.sjtb.reporting.service.ExcelService;
import com.sjtb.reporting.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController @RequestMapping("/api/templates")
public class TemplateController {
    private final TemplateService templates; private final ExcelService excel;
    public TemplateController(TemplateService templates, ExcelService excel) { this.templates = templates; this.excel = excel; }
    @GetMapping public List<TemplateDtos.Response> list() { return templates.list(); }
    @GetMapping("/{id:\\d+}") public TemplateDtos.Response get(@PathVariable Long id) { return templates.get(id); }
    @GetMapping("/{id:\\d+}/versions") public List<TemplateDtos.VersionResponse> versions(@PathVariable Long id) { return templates.versions(id); }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')") public TemplateDtos.Response create(@Valid @RequestBody TemplateDtos.Request request) { return templates.create(request); }
    @PutMapping("/{id:\\d+}") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')") public TemplateDtos.Response update(@PathVariable Long id, @Valid @RequestBody TemplateDtos.Request request) { return templates.update(id, request); }
    @DeleteMapping("/{id:\\d+}") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')") public void delete(@PathVariable Long id) { templates.delete(id); }
    @PostMapping("/import") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')") public TemplateDtos.Response importTemplate(@RequestParam String code, @RequestParam String name, @RequestParam(required = false) String description, @RequestParam("file") MultipartFile file) { return excel.importTemplate(code, name, description, file); }
    @PostMapping("/import-preview") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')") public com.sjtb.reporting.dto.TemplateImportDtos.PreviewResponse importPreview(@RequestParam("file") MultipartFile file) { return excel.previewTemplateImport(file); }
    @PostMapping("/import-confirm") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')") public List<TemplateDtos.Response> importConfirm(@RequestParam String names, @RequestParam("file") MultipartFile file) { return excel.importTemplates(names, file); }
    @PostMapping("/{id:\\d+}/file") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')") public TemplateDtos.Response replaceTemplateFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) { return excel.replaceTemplateColumns(id, file); }
    @GetMapping("/import-sample") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public ResponseEntity<byte[]> importSample() {
        String filename = ContentDisposition.attachment().filename("模板导入样例.xlsx", StandardCharsets.UTF_8).build().toString();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION, filename).body(excel.templateImportSample());
    }
    @GetMapping("/{id:\\d+}/download") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','LEADER','REPORTER')") public ResponseEntity<byte[]> downloadAlias(@PathVariable Long id) { return download(id); }
    @GetMapping("/{id:\\d+}/excel-template") @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','LEADER')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        TemplateDtos.Response template = templates.get(id);
        String filename = ContentDisposition.attachment().filename(template.name() + ".xlsx", StandardCharsets.UTF_8).build().toString();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION, filename).body(excel.blankTemplate(id));
    }
}
