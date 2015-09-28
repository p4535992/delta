DROP INDEX if EXISTS idx_delta_log_created;
CREATE INDEX idx_delta_log_created ON delta_log (date(created_date_time));