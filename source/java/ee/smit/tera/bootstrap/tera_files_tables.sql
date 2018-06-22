CREATE TABLE tera_files
(
  id bigint NOT NULL,
  created_date_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  filename text,
  filetype text,
  node_ref text NOT NULL,
  crypt text,
  asics boolean NOT NULL DEFAULT FALSE,
  checked boolean NOT NULL DEFAULT FALSE,
  status_info text,
  PRIMARY KEY (id)
);

CREATE INDEX idx_tera_files_filename ON tera_files (filename);
CREATE INDEX idx_tera_files_filetype ON tera_files (filetype);
CREATE INDEX idx_tera_files_node_ref ON tera_files (node_ref);
CREATE INDEX idx_tera_files_crypt ON tera_files (crypt);
CREATE INDEX idx_tera_files_asics ON tera_files (asics);
CREATE INDEX idx_tera_files_checked ON tera_files (checked);

CREATE SEQUENCE tera_files_seq;

ALTER TABLE ONLY tera_files ALTER COLUMN id SET DEFAULT nextval('tera_files_seq'::regclass);
