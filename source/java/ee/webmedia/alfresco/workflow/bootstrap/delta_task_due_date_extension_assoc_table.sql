<<<<<<< HEAD
CREATE TABLE delta_task_due_date_extension_assoc (
  task_due_date_extension_assoc_id BIGSERIAL PRIMARY KEY, -- auto-increment
  task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE,
  extension_task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE UNIQUE
=======
CREATE TABLE delta_task_due_date_extension_assoc (
  task_due_date_extension_assoc_id BIGSERIAL PRIMARY KEY, -- auto-increment
  task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE,
  extension_task_id text REFERENCES delta_task(task_id) ON DELETE CASCADE UNIQUE
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
);