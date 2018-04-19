CREATE TABLE delta_task_due_date_extension_assoc (
  task_due_date_extension_assoc_id BIGSERIAL PRIMARY KEY, -- auto-increment
  task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE,
  extension_task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE UNIQUE
);