#!/bin/bash
<<<<<<< HEAD
# Author: Alar Kvell
=======
>>>>>>> develop-5.1
# Usage ./delta-requests.sh alfresco.log [alfresco2.log [...]]
# Outputs CSV with the following columns (separated by semicolon), suitable for importing into Excel or a database:
#  1) kasutaja HTTP päringu aeg kokku (ms)
#  2) servlet_path
#  3) action
#  4) action_listener
#  5) outcome
#  6) viewid
#  7) DB päringute arv
#  8) DB päringute aeg kokku (ms)
#  9) transaktsioonide retry arv
# 10) transaktsioonide retry aeg kokku (ms)
# 12) lucene päringute arv
# 13) lucene päringute aeg kokku (ms)
# 14) MSO päringute arv
# 15) MSO päringute aeg kokku (ms)
# Tunnused 2-6 kokku unikaalselt identifitseerivad päringu; kui soovida ridu summeerida, siis tuleks grupeerida nende 5 tunnuse alusel korraga
#  (a la GROUP BY servlet_path, action, action_listener, outcome, viewid).

grep -F -h REQUEST_END "$@"|sed -e 's/.*REQUEST_END,//' -e 's/,[^|]*|[^|]*|SERVLET_PATH,/;/' -e 's/|[^|]*|[^|]*|[^|]*|[^|]*|[^|]*|[^|]*|/;/' -e 's/ACTION_LISTENER,[0-9]*,//' -e 's/|/;/' -e 's/ACTION\(,[0-9]*\)\?,//' -e 's/|/;/' -e 's/OUTCOME,[0-9]*,//' -e 's/|/;/' -e 's/VIEWID,//' -e 's/|[^|]*|[^|]*|[^|]*|/;/' -e 's/|/;/' -e 's/|[^|]*|[^|]*|[^|]*|[^|]*|[^|]*|[^|]*|[^|]*|[^|]*|/;/' -e 's/|[^|]*|/;/' -e 's/\([^;]*;[^;]*;[^;]*;[^;]*;[^;]*;[^;]*\);;\(.*\)/\1;0,0;\2/' -e 's/\([^;]*;[^;]*;[^;]*;[^;]*;[^;]*;[^;]*;[^;]*\);;\(.*\)/\1;0,0;\2/' -e 's/\([^;]*;[^;]*;[^;]*;[^;]*;[^;]*;[^;]*;[^;]*;[^;]*\);;\(.*\)/\1;0,0;\2/' -e 's/\(.*\);$/\1;0,0/' -e 's/DB,//'  -e 's/TX_RETRY,//' -e 's/IDX_QUERY,//' -e 's/SRV_MSO,//' -e 's/,/;/g'
