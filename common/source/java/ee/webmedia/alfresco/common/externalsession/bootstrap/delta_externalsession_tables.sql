
CREATE TABLE delta_external_session (
  username text PRIMARY KEY,
  created_date_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  expiration_date_time timestamp NOT NULL,
  session_id text UNIQUE NOT NULL
);
