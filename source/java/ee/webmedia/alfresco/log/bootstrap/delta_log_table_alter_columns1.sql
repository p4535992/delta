ALTER TABLE delta_log ADD COLUMN doc_name text; 
ALTER TABLE delta_log ADD COLUMN reg_number text; 
ALTER TABLE delta_log ADD COLUMN url text; 

CREATE INDEX idx_delta_log_doc_name ON delta_log (doc_name);
CREATE INDEX idx_delta_log_reg_nr ON delta_log (reg_number);
CREATE INDEX idx_delta_log_url ON delta_log (url);