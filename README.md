# README #

Dokumendihaldussüsteem DELTA.



### Versioonid ###
------------------------------------------------------------------------------------------
DELTA version: 5.9.3
------------------------------------------------------------------------------------------
•	DELTA-1548 Menüü "Dokumendid", "Terviktöövood", "Asjatoimikud" veergude muudatus: veerus "Töövoo seis" tööülesande täitja nime järel kuvada ka TÜ tähtaeg + kuvada pealkiri pikemalt
•	DELTA-1541 Veerus "Töövoo seis" kuvada staatuseks "Peatatud" kui töövoog peatub
•	DELTA-1633
•	DELTA-1554
•	DELTA-1279 Digidoc Client konteineri sees oleva faili avamise logimine
•	DELTA-1356_fix
•	DELTA-1356_fix
•	DELTA-1515
•	DELTA-1516

* Correcting javadoc.
* Adding adit param: x-tee.adit.infosystem=TEST|DELTA
* Updating bcpkix-jdk15on-1.45.jar ==> 1.58

•	PERH arendus

P.S. alfresco-gobal.properties failis PERH puhul (teiste ldap sünk omadusega) on vaja lisada:
oracle.db.name=
oracle.db.username=
oracle.db.password=
oracle.db.host=

oracle.db.port=
oracle.db.driver=oracle.jdbc.OracleDriver

oracle.db.url=jdbc:oracle:thin:@${oracle.db.host}:${oracle.db.port}:${oracle.db.name}

orgstruct.fromdatabase=true

------------------------------------------------------------------------------------------
DELTA version: 5.9.0
------------------------------------------------------------------------------------------
corrected branch

------------------------------------------------------------------------------------------
DELTA version: 5.2.3.184.1
------------------------------------------------------------------------------------------

* CDOC crypting fix: Added support for new ID-Card sertificate - KeyUsage change.

------------------------------------------------------------------------------------------
DELTA version: 5.2.3.184
------------------------------------------------------------------------------------------
* DELTA-1472 Asutusele saadetud dokumendid jäävad osaliselt DVK-sse rippuma ja neid ei laeta DELTA-sse.
