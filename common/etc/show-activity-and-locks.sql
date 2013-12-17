SELECT CURRENT_TIMESTAMP;

SELECT * FROM pg_catalog.pg_stat_activity;

SELECT bl.pid AS blocked_pid, a.usename AS blocked_user, 
         kl.pid AS blocking_pid, ka.usename AS blocking_user, a.current_query AS blocked_statement
  FROM pg_catalog.pg_locks bl
       JOIN pg_catalog.pg_stat_activity a
       ON bl.pid = a.procpid
       JOIN pg_catalog.pg_locks kl
            JOIN pg_catalog.pg_stat_activity ka
            ON kl.pid = ka.procpid
       ON bl.transactionid = kl.transactionid AND bl.pid != kl.pid
  WHERE NOT bl.granted;

select 
     pg_stat_activity.datname,pg_class.relname,pg_locks.transactionid, pg_locks.mode, pg_locks.granted,
     pg_stat_activity.usename,substr(pg_stat_activity.current_query,1,30), pg_stat_activity.query_start, 
     age(now(),pg_stat_activity.query_start) as "age", pg_stat_activity.procpid 
   from pg_stat_activity,pg_locks left 
     outer join pg_class on (pg_locks.relation = pg_class.oid)  
   where pg_locks.pid=pg_stat_activity.procpid order by query_start;

  select bl.pid as blocked_pid, a.usename as blocked_user,
        ka.current_query as blocking_statement, now() - ka.query_start as blocking_duration,
        kl.pid as blocking_pid, ka.usename as blocking_user, a.current_query as blocked_statement,
        now() - a.query_start as blocked_duration
 from pg_catalog.pg_locks bl
      join pg_catalog.pg_stat_activity a
      on bl.pid = a.procpid
      join pg_catalog.pg_locks kl
           join pg_catalog.pg_stat_activity ka
           on kl.pid = ka.procpid
      on bl.transactionid = kl.transactionid and bl.pid != kl.pid
 where not bl.granted;
