
CREATE TABLE delta_maais_session (
  username text PRIMARY KEY,
  created_date_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  expiration_date_time timestamp NOT NULL,
  url text NOT NULL
);
