\o delta-stats.txt

SELECT CURRENT_TIMESTAMP;

\timing on
\echo 'Getting database stats'
\qecho 'BG writer stats'
SELECT * FROM pg_stat_bgwriter;
\qecho 'Database stats'
SELECT * FROM pg_stat_database WHERE datname = current_database();


\echo 'Calculating table stats'
\qecho 'Attribute distribution'
SELECT tablename, attname, null_frac, n_distinct, avg_width, correlation 
    FROM pg_stats
    WHERE tablename IN ('alf_node_properties', 'alf_node')
    ORDER BY tablename, attname;

\qecho 'Table stats'
SELECT st.relname AS "table",
        pg_size_pretty(pg_relation_size(st.relname::text)) AS tablesize,
        pg_size_pretty(pg_indexes_size(st.relname::text)) AS indexsize,
        n_live_tup, n_dead_tup, seq_scan, idx_scan,
        n_tup_ins, n_tup_upd, n_tup_del, n_tup_hot_upd,
        heap_blks_read + heap_blks_hit AS heap_accesses,
        ROUND(100. * heap_blks_hit / (heap_blks_read + heap_blks_hit), 4) AS heap_hitrate,
        idx_blks_read + idx_blks_hit AS idx_accesses,
        ROUND(100. * idx_blks_hit / (idx_blks_read + idx_blks_hit), 4) AS idx_hitrate
    FROM pg_stat_user_tables AS st
        INNER JOIN pg_statio_user_tables AS io USING (schemaname, relname)
    WHERE n_live_tup > 1000
    ORDER BY n_live_tup DESC;

\qecho 'Scan stats'
SELECT relname, seq_scan, idx_scan
    FROM pg_stat_user_tables
    ORDER BY idx_scan + seq_scan DESC
    LIMIT 40;

\echo 'Calculating node creation rate'
\qecho 'Node creation rate'
SELECT date_trunc('day', audit_created::timestamp) AS day, node_deleted, COUNT(*)
    FROM alf_node
    GROUP BY node_deleted, day
    ORDER BY node_deleted, day;

\echo 'Calculating transaction rate'
\qecho 'Transaction rate'
SELECT date_trunc('day', '1970-01-01'::timestamp + commit_time_ms::real / 1000 * '1 second'::interval) AS day,
        COUNT(*) 
    FROM alf_transaction
    GROUP BY day
    ORDER BY day;
