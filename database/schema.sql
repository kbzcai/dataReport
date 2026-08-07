CREATE DATABASE IF NOT EXISTS data_reporting DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE data_reporting;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码摘要',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, role),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS report_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT NULL,
  columns_json TEXT NOT NULL COMMENT '模板字段定义，仅包含表头字段',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_report_template_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报模板';

CREATE TABLE IF NOT EXISTS report_template_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  columns_json TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_template_version_no (template_id, version_no),
  KEY idx_template_version_template (template_id, version_no),
  CONSTRAINT fk_template_version_template FOREIGN KEY (template_id) REFERENCES report_template (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板字段版本快照';

CREATE TABLE IF NOT EXISTS report_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  template_id BIGINT NOT NULL,
  template_version_id BIGINT DEFAULT NULL,
  frequency VARCHAR(20) NOT NULL,
  period_label VARCHAR(64) DEFAULT NULL,
    start_at DATETIME DEFAULT NULL,
    deadline DATETIME DEFAULT NULL,
    allow_late TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  description VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_task_template_status (template_id, status),
  CONSTRAINT fk_task_template FOREIGN KEY (template_id) REFERENCES report_template (id),
  CONSTRAINT fk_task_template_version FOREIGN KEY (template_version_id) REFERENCES report_template_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报任务';

CREATE TABLE IF NOT EXISTS report_task_assignee (
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (task_id, user_id),
  CONSTRAINT fk_task_assignee_task FOREIGN KEY (task_id) REFERENCES report_task (id) ON DELETE CASCADE,
  CONSTRAINT fk_task_assignee_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务填报人员';

CREATE TABLE IF NOT EXISTS report_task_detail (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  template_version_id BIGINT DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  submitted_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_detail_reporter (task_id, reporter_id),
  KEY idx_task_detail_task_status (task_id, status),
  KEY idx_task_detail_reporter (reporter_id, status),
  CONSTRAINT fk_task_detail_task FOREIGN KEY (task_id) REFERENCES report_task (id) ON DELETE CASCADE,
  CONSTRAINT fk_task_detail_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user (id),
  CONSTRAINT fk_task_detail_version FOREIGN KEY (template_version_id) REFERENCES report_template_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人任务明细';

CREATE TABLE IF NOT EXISTS report_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_id BIGINT NOT NULL,
  template_version_id BIGINT DEFAULT NULL,
  task_id BIGINT DEFAULT NULL,
  reporter_id BIGINT NOT NULL,
  data_json TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  review_comment VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_record_template_reporter (template_id, reporter_id),
  KEY idx_record_updated_at (updated_at),
  CONSTRAINT fk_record_template FOREIGN KEY (template_id) REFERENCES report_template (id),
  CONSTRAINT fk_record_template_version FOREIGN KEY (template_version_id) REFERENCES report_template_version (id),
  CONSTRAINT fk_record_task FOREIGN KEY (task_id) REFERENCES report_task (id),
  CONSTRAINT fk_record_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报记录';

CREATE TABLE IF NOT EXISTS report_change_request (
  id BIGINT NOT NULL AUTO_INCREMENT,
  report_id BIGINT NOT NULL,
  requester_id BIGINT NOT NULL,
  proposed_data_json TEXT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  base_updated_at DATETIME NOT NULL,
  status ENUM('APPROVED','CANCELLED','PENDING','REJECTED') NOT NULL DEFAULT 'PENDING',
  reviewer_id BIGINT DEFAULT NULL,
  review_comment VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_change_request_status (status),
  KEY idx_change_request_report (report_id),
  CONSTRAINT fk_change_request_report FOREIGN KEY (report_id) REFERENCES report_record (id) ON DELETE CASCADE,
  CONSTRAINT fk_change_request_requester FOREIGN KEY (requester_id) REFERENCES sys_user (id),
  CONSTRAINT fk_change_request_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报修改申请';

CREATE TABLE IF NOT EXISTS report_record_value (
    id BIGINT NOT NULL AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    template_id BIGINT DEFAULT NULL,
    field_key VARCHAR(64) NOT NULL,
    unique_marker VARCHAR(1) DEFAULT NULL,
    value_hash VARCHAR(64) DEFAULT NULL,
  value_text VARCHAR(2000) DEFAULT NULL,
  value_number DECIMAL(30,8) DEFAULT NULL,
  value_date DATE DEFAULT NULL,
  PRIMARY KEY (id),
    UNIQUE KEY uk_record_value_field (record_id, field_key),
    UNIQUE KEY uk_template_field_unique_value (template_id, field_key, unique_marker, value_hash),
  KEY idx_value_field_text (field_key),
  KEY idx_value_field_number (field_key, value_number),
  KEY idx_value_record (record_id),
  CONSTRAINT fk_value_record FOREIGN KEY (record_id) REFERENCES report_record (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态字段检索值';

CREATE TABLE IF NOT EXISTS report_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  original_file_name VARCHAR(255) NOT NULL,
  creator_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
  imported_rows INT NOT NULL DEFAULT 0,
  failed_rows INT NOT NULL DEFAULT 0,
  summary VARCHAR(1000) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_import_batch_creator (creator_id, created_at),
  KEY idx_import_batch_status (status),
  CONSTRAINT fk_import_batch_creator FOREIGN KEY (creator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报导入批次';

CREATE TABLE IF NOT EXISTS report_import_error (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  sheet_name VARCHAR(128) DEFAULT NULL,
  excel_row_number INT DEFAULT NULL,
  message VARCHAR(1000) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_import_error_batch (batch_id),
  CONSTRAINT fk_import_error_batch FOREIGN KEY (batch_id) REFERENCES report_import_batch (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入错误行';

CREATE TABLE IF NOT EXISTS report_change_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  report_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  actor_name VARCHAR(64) NOT NULL,
  action VARCHAR(32) NOT NULL,
  before_data_json TEXT DEFAULT NULL,
  after_data_json TEXT DEFAULT NULL,
  reason VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_change_log_record (report_id, created_at),
  KEY idx_change_log_actor (actor_id, created_at),
  CONSTRAINT fk_change_log_actor FOREIGN KEY (actor_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报操作日志';

-- 可选的角色字典，实际用户角色存储在 sys_user_role.role 中。
CREATE TABLE IF NOT EXISTS sys_role (
  role_code VARCHAR(32) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  PRIMARY KEY (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色字典';

INSERT INTO sys_role (role_code, role_name) VALUES
  ('ADMIN', '系统管理员'),
  ('MAINTAINER', '模板管理员'),
  ('LEADER', '数据领导'),
  ('REPORTER', '填报人员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO report_template (code, name, description, columns_json, enabled)
SELECT 'monthly_operation', '月度经营填报', '月度经营情况填报模板',
       JSON_ARRAY(
         JSON_OBJECT('key', 'report_month', 'label', '填报月份', 'type', 'month', 'required', true),
         JSON_OBJECT('key', 'organization_name', 'label', '单位名称', 'type', 'text', 'required', true),
         JSON_OBJECT('key', 'operating_revenue', 'label', '营业收入（元）', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'operating_cost', 'label', '营业成本（元）', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'total_profit', 'label', '利润总额（元）', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'employee_count', 'label', '从业人数', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'remark', 'label', '备注', 'type', 'textarea', 'required', false)
       ), 1
WHERE NOT EXISTS (SELECT 1 FROM report_template WHERE code = 'monthly_operation');

INSERT INTO report_template (code, name, description, columns_json, enabled)
SELECT 'annual_operation', '年度经营填报', '年度经营情况填报模板',
       JSON_ARRAY(
         JSON_OBJECT('key', 'report_year', 'label', '填报年度', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'organization_name', 'label', '单位名称', 'type', 'text', 'required', true),
         JSON_OBJECT('key', 'annual_revenue', 'label', '年度营业收入（元）', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'annual_cost', 'label', '年度营业成本（元）', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'annual_profit', 'label', '年度利润总额（元）', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'employee_count', 'label', '年末从业人数', 'type', 'number', 'required', true),
         JSON_OBJECT('key', 'remark', 'label', '备注', 'type', 'textarea', 'required', false)
       ), 1
WHERE NOT EXISTS (SELECT 1 FROM report_template WHERE code = 'annual_operation');

INSERT INTO report_template_version (template_id, version_no, columns_json, status)
SELECT t.id, 1, t.columns_json, 'ACTIVE'
FROM report_template t
WHERE t.code IN ('monthly_operation', 'annual_operation')
  AND NOT EXISTS (
    SELECT 1 FROM report_template_version v
    WHERE v.template_id = t.id
  );
