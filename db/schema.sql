-- veggieBox schema (system MySQL, database `veggiebox`).
-- Multi-business, multi-worker delivery route manager.
-- NOTE: this file contains NO customer PII — only DDL. Customer names/phones
-- live in the DB rows (seeded separately, never committed).

SET NAMES utf8mb4;

-- A delivery operation (e.g. Silverman farm @ Midreshet Ben-Gurion, Tlalim, ...).
CREATE TABLE IF NOT EXISTS businesses (
  id                   INT AUTO_INCREMENT PRIMARY KEY,
  slug                 VARCHAR(64)  NOT NULL UNIQUE,
  name                 VARCHAR(255) NOT NULL,
  area                 VARCHAR(255) DEFAULT NULL,           -- "מדרשת בן גוריון"
  default_central_drop VARCHAR(255) DEFAULT NULL,           -- "נקודה קבועה אצל נופר"
  rate_per_delivery    DECIMAL(8,2) NOT NULL DEFAULT 10.00,
  currency             VARCHAR(8)   NOT NULL DEFAULT '₪',
  map_center_lat       DOUBLE       DEFAULT NULL,
  map_center_lon       DOUBLE       DEFAULT NULL,
  created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Deliverers. A business can have many workers (Yaniv @ Midresha, someone @ Tlalim).
CREATE TABLE IF NOT EXISTS workers (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  business_id INT          NOT NULL,
  name        VARCHAR(255) NOT NULL,
  phone       VARCHAR(32)  DEFAULT NULL,
  active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Recipients of the produce boxes.
CREATE TABLE IF NOT EXISTS customers (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  business_id        INT          NOT NULL,
  name               VARCHAR(255) NOT NULL,
  phone              VARCHAR(32)  DEFAULT NULL,
  address            VARCHAR(512) DEFAULT NULL,
  house_instructions TEXT         DEFAULT NULL,   -- "בית חדש על הכיכר עם פרגולות" / "יש כלבה בחצר"
  lat                DOUBLE       DEFAULT NULL,
  lon                DOUBLE       DEFAULT NULL,
  drop_preference    ENUM('central','beside') DEFAULT NULL, -- where to leave if not home (customer's choice)
  default_cartons    INT          NOT NULL DEFAULT 1,
  details_token      VARCHAR(40)  DEFAULT NULL UNIQUE,      -- self-service form link token
  details_filled_at  TIMESTAMP    NULL DEFAULT NULL,
  active             TINYINT(1)   NOT NULL DEFAULT 1,
  sort_hint          INT          DEFAULT NULL,             -- manual ordering hint
  created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A delivery run on a given day for a worker.
CREATE TABLE IF NOT EXISTS routes (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  business_id INT  NOT NULL,
  worker_id   INT  DEFAULT NULL,
  route_date  DATE NOT NULL,
  status      ENUM('planning','active','done') NOT NULL DEFAULT 'active',
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_route (business_id, worker_id, route_date),
  FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
  FOREIGN KEY (worker_id)   REFERENCES workers(id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ordered stops within a route. This is where per-day delivery state lives.
CREATE TABLE IF NOT EXISTS route_stops (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  route_id          INT NOT NULL,
  customer_id       INT NOT NULL,
  seq               INT NOT NULL DEFAULT 0,                  -- delivery order
  status            ENUM('pending','delivered','not_home','skipped') NOT NULL DEFAULT 'pending',
  cartons           INT NOT NULL DEFAULT 1,
  drop_used         ENUM('home','central','beside') DEFAULT NULL,  -- actual outcome
  media_path        VARCHAR(512) DEFAULT NULL,               -- photo/video when not home
  notes             TEXT DEFAULT NULL,
  on_my_way_sent_at TIMESTAMP NULL DEFAULT NULL,
  delivered_at      TIMESTAMP NULL DEFAULT NULL,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_stop (route_id, customer_id),
  KEY idx_route (route_id),
  FOREIGN KEY (route_id)    REFERENCES routes(id)    ON DELETE CASCADE,
  FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reusable WhatsApp message bodies. {name} / {worker} / {link} placeholders.
CREATE TABLE IF NOT EXISTS message_templates (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  business_id INT NOT NULL,
  key_name    VARCHAR(64) NOT NULL,   -- 'greeting_missing_details', 'on_my_way'
  body        TEXT NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_tmpl (business_id, key_name),
  FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
