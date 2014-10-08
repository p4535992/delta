CREATE TABLE delta_compound_workflow_comment (
  comment_id  BIGSERIAL PRIMARY KEY, -- auto-incremented
  compound_workflow_id text NOT NULL,
  created timestamp NOT NULL,
  creator_id text NOT NULL,
  creator_name text NOT NULL,
  comment_text text NOT NULL
);