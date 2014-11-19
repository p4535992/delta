<<<<<<< HEAD
CREATE TABLE delta_task_due_date_history (
  task_due_date_history_id BIGSERIAL PRIMARY KEY, -- auto-incremented
  task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE,
  previous_date TIMESTAMP NOT NULL,
  change_reason text
=======
CREATE TABLE delta_task_due_date_history (
  task_due_date_history_id BIGSERIAL PRIMARY KEY, -- auto-incremented
  task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE,
  previous_date TIMESTAMP NOT NULL,
  change_reason text
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
);