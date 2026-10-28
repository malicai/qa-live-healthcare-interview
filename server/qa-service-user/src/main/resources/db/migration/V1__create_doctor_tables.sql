CREATE TABLE doctor (
    id            VARCHAR(32)  PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(64)  NOT NULL,
    name_en       VARCHAR(128) NOT NULL,
    title         VARCHAR(64)  NOT NULL,
    title_en      VARCHAR(128) NOT NULL,
    department    VARCHAR(64)  NOT NULL,
    department_en VARCHAR(128) NOT NULL,
    avatar        VARCHAR(512) NOT NULL,
    experience    VARCHAR(128) NOT NULL,
    experience_en VARCHAR(255) NOT NULL,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_doctor_is_active (is_active)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE doctor_specialty (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    doctor_id  VARCHAR(32) NOT NULL,
    name       VARCHAR(64) NOT NULL,
    name_en    VARCHAR(128) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_specialty_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
