CREATE TABLE delta_log_level (
  level text PRIMARY KEY
);

CREATE TABLE delta_log (
  log_entry_id text PRIMARY KEY,
  created_date_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  level text NOT NULL,
  creator_id text NOT NULL,
  creator_name text NOT NULL,
  computer_ip text NOT NULL,
  computer_name text,
  object_id text,
  object_name text,
  description text NOT NULL
);

INSERT INTO delta_log_level VALUES ('DOCUMENT'), ('WORKFLOW');

CREATE INDEX idx_delta_log_created ON delta_log (created_date_time);
CREATE INDEX idx_delta_log_creator_name ON delta_log (creator_name);
CREATE INDEX idx_delta_log_computer_ip ON delta_log (computer_ip);
CREATE INDEX idx_delta_log_computer_name ON delta_log (computer_name);
CREATE INDEX idx_delta_log_object_id ON delta_log (object_id);
CREATE INDEX idx_delta_log_object_name ON delta_log (object_name);
CREATE INDEX idx_delta_log_desc ON delta_log (description);
