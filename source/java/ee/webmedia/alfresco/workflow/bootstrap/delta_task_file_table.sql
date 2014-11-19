<<<<<<< HEAD
CREATE TABLE delta_task_file (
  task_file_id BIGSERIAL PRIMARY KEY, -- auto-incremented
  task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE,
  file_id text NOT NULL,
  UNIQUE(task_id, file_id) DEFERRABLE INITIALLY DEFERRED
=======
CREATE TABLE delta_task_file (
  task_file_id BIGSERIAL PRIMARY KEY, -- auto-incremented
  task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE,
  file_id text NOT NULL,
  UNIQUE(task_id, file_id) DEFERRABLE INITIALLY DEFERRED
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
);