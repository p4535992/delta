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
>>>>>>> develop-5.1
);