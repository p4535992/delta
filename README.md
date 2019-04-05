# README #

Dokumendihaldussüsteem DELTA.

### Versioonid ###
------------------------------------------------------------------------------------------
DELTA version: 5.10.1 RC7
------------------------------------------------------------------------------------------
•	DELTA-1645 probleem asendaja lisamisega (updated)
•	DELTA-1365 Süsteemi rakenduse logisse lisada dokumendi pealkiri, viit ning link
•	DELTA-1652 Delta-AMR-i sünk on katki
•	Digisign-crypt separate service support.
	
	alfresco-gobal.properties file new params:
	
	digisign.crypt.service.url=https://digisign-crypt.smit.ee
	digisign.crypt.service.client.id=[digisign-crypt id name]
	digisign.crypt.service.client.secure=[digisign-crypt secure code]
	digisign.crypt.service.appname=[registred application username]
	digisign.crypt.service.apppass=[registred application password]
	digisign.crypt.service.active=true/false
	
------------------------------------------------------------------------------------------
DELTA version: 5.10.1 RC6
------------------------------------------------------------------------------------------
•	DELTA-1645 probleem asendaja lisamisega
•	Digisign date convert change
•	DELTA paigaldusjuhend update

------------------------------------------------------------------------------------------
DELTA version: 5.10.1 RC5
------------------------------------------------------------------------------------------
•	1001 mesting fix

------------------------------------------------------------------------------------------
DELTA version: 5.10.1 RC4 - hotfix
------------------------------------------------------------------------------------------
•	DELTA-1566 - Probleemid meilide lohistamisega

------------------------------------------------------------------------------------------
DELTA version: 5.10.1 RC3
------------------------------------------------------------------------------------------
•	rahmin-fix1
•	DELTA-1642 Saatmise plokis ei kuvata saatmisviisi "riigiportaal eesti.ee" puhul välja saadetavate failide nimesid
•	DELTA-1547 Süsteem peab kontrollima sissetuleva kirja täitmise tähtaega vastu loodava vastus-/järgikirja tööülesannete tähtaegu
•	DELTA-974 Teema välja otsing peab otsima ka teema peakirja seest ja teemade nimekirjadesse lisada otsingu väli
•	DELTA-768 Failid ja taustainfo failide plokis peab saama mitut faili korraga panna taustainfo failide alla ja vastupidi ning kustutada.

------------------------------------------------------------------------------------------
DELTA version: 5.10.1 RC2
------------------------------------------------------------------------------------------
•	DELTA-1430 fix - Iseseisvas terviktöövoos peab saama siduda dokumendi liiki konkreetsete töövoogudega

------------------------------------------------------------------------------------------
DELTA version: 5.10.1 RC1
------------------------------------------------------------------------------------------
•	DELTA-1001 - Kasutajat peab olema võimalik otsida ka tööülesande täitja andmeväljalt
•	DELTA-1430 - Iseseisvas terviktöövoos peab saama siduda dokumendi liiki konkreetsete töövoogudega
•	DELTA-1569 - Teatud tingimusel ei kuva menüüs "Dokumendi otsing" välja ikooni "Lisa otsetee"
•	DELTA-1625 - Toimikute elukäikude andmetes on märgistatud väli "Märgitud hävitamiseks"
•	DELTA-1637 - Teadmiseks TÜ edastamisel tähtaja lahtri täitmise kontroll ebakorrektne
•	DELTA-1639 - Päästeameti Deltas lühimenetluse otsuse vormil tekkib "Saada välja" nupu vajutusel süsteemi viga

------------------------------------------------------------------------------------------
DELTA version: 5.10.0 RELEASE - 14.02.2019
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
DELTA version: 5.10.0 RC2
------------------------------------------------------------------------------------------
Added fix:
•	DELTA-982 Luua uus süsteemne teavitus, mille saadab süsteem siis, kui töövoog peatub

------------------------------------------------------------------------------------------
DELTA version: 5.10.0 RC1
------------------------------------------------------------------------------------------
Added ADIT-adapter 1.0.0 support (https://bitbucket.org/smitdevel/adit-adapter/wiki/Home)
Deploys as jar.

Add alfresco-gobal.properties NEW params (if not exists):
x-tee.institution=[Reg.Nr]
adit.service.url=https://adit-adapter.develop:8090
adit.service.active=false|true

------------------------------------------------------------------------------------------
DELTA version: 5.9.3 RC2
------------------------------------------------------------------------------------------
•	PERH parandus, kontrollitakse kas kasutaja on olemas deltas või mitte

------------------------------------------------------------------------------------------
DELTA version: 5.9.3 RC1
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
