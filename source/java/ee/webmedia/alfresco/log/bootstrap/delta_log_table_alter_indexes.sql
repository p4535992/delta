-- in some environments, indexes have been deleted manually, so use IF EXISTS condition 
DROP INDEX IF EXISTS idx_delta_log_creator_name;
DROP INDEX IF EXISTS idx_delta_log_desc;
-- recreating these index is future development (can be done used trigram index), currently they are not used
DROP INDEX IF EXISTS idx_delta_log_computer_ip;
DROP INDEX IF EXISTS idx_delta_log_computer_name;
DROP INDEX IF EXISTS idx_delta_log_object_name;

CREATE INDEX idx_delta_log_creator_name ON delta_log USING gin(to_tsvector('simple', creator_name));
CREATE INDEX idx_delta_log_desc ON delta_log USING gin(to_tsvector('simple', description));