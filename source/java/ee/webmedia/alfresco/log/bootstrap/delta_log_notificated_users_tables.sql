CREATE SEQUENCE DELTA_NOTIFICATION_LOG_ID START 1;

CREATE TYPE delta_notification_user_log_item AS (first_name varchar(128), last_name varchar(128), email varchar(256), id_code varchar(16));

CREATE TABLE IF NOT EXISTS DELTA_NOTIFICATION_GROUP_LOG(
id BIGSERIAL PRIMARY KEY,
notification_log_id BIGINT,
user_group_name VARCHAR(512),
notification_date_time timestamp,
user_group_hash VARCHAR(32)
);
ALTER SEQUENCE DELTA_NOTIFICATION_GROUP_LOG_ID_SEQ OWNED BY DELTA_NOTIFICATION_GROUP_LOG.ID;
CREATE INDEX idx_delta_ngl_log_id ON DELTA_NOTIFICATION_GROUP_LOG (notification_log_id);
CREATE INDEX idx_delta_ngl_group_hash ON DELTA_NOTIFICATION_GROUP_LOG (user_group_hash);

CREATE TABLE IF NOT EXISTS DELTA_NOTIFICATION_USER_GROUP_LOG(
notification_group_log_id BIGINT,
delta_notification_user_log_ids TEXT
);
CREATE INDEX idx_delta_nugl_log_id ON DELTA_NOTIFICATION_USER_GROUP_LOG (notification_group_log_id);

CREATE TABLE IF NOT EXISTS DELTA_NOTIFICATION_USER_LOG(
id BIGSERIAL,
first_name VARCHAR(128),
last_name VARCHAR(128),
email VARCHAR(256),
id_code VARCHAR(16)
);
ALTER SEQUENCE DELTA_NOTIFICATION_USER_LOG_ID_SEQ OWNED BY DELTA_NOTIFICATION_USER_LOG.ID;
CREATE INDEX idx_delta_nul_firstName ON DELTA_NOTIFICATION_USER_LOG (first_name);
CREATE INDEX idx_delta_nul_lastName ON DELTA_NOTIFICATION_USER_LOG (last_name);
CREATE INDEX idx_delta_nul_email ON DELTA_NOTIFICATION_USER_LOG (email);
CREATE INDEX idx_delta_nul_code ON DELTA_NOTIFICATION_USER_LOG (id_code);
CREATE INDEX idx_delta_nul_all ON DELTA_NOTIFICATION_USER_LOG (first_name, last_name, email,  id_code);
 