<<<<<<< HEAD
Lähteandmed ja testi seadistamine
---------------------------------
1) Lähteandmed on seadistatavad csv failide ning testiplaani muutujate abil (User Defined Variables)
2) csv failid asuvad delta.jmx suhtes csvFiles kataloogis ning on järgnevad:
  a) fieldvalues.csv - sisaldab väärtusi tekstiväljade tarvis, mida kasutatakse tekstiväljade täitmiseks
  b) uploadPaths.csv - sisaldab failinimesid koos radadega, mida kasutatakse failide üleslaadimiseks
  c) users.csv - sisaldab Deltas olevate sama asutuse kasutajate kohta igal real komaga eraldatult kasutajanime ja parooli, mida kasutatakse CAS'is autentimisel
  d) usersfirstlastnames.csv - sisaldab Deltase olevate sama asutuse kasutajate ees- ja/või perenimesid, mida kasutatakse kasutajate otsingul Deltas
3) Testiplaani muutujad on järgmised:
  a) protocol - http/https
  b) host - serveri domeeninimi või IP aadress
  c) port - 80/443/vms
  d) recipientName - testi käigus välja saadetavate kirjade adressaadi nimi
       (määrab ainult dokumendi välja saatmise adressaadi; süsteemsed e-maili teavitused lähevad kasutajate e-maili aadressidele tavalisel viisil)
  e) recipientEmail - testi käigus välja saadetavate kirjade adressaadi e-mail
       (määrab ainult dokumendi välja saatmise adressaadi; süsteemsed e-maili teavitused lähevad kasutajate e-maili aadressidele tavalisel viisil)
  f) organizationUsersGroup - kasutajagrupp, milles sisalduvad kõik sama asutuse kasutajad; tavaliselt sama, mis parameetri taskOwnerStructUnit väärtus
  g) phoneNumber - mobiili ID'ga allkirjastamise number;
      kui kasutusel on DigiDocService testteenus (kui Delta konf.failis jdigidoc.test=true), siis selle jaoks sobilik automaattestimise number on +37200007
  h) cas_protocol - CAS teenus, http/https
  i) cas_host - CAS teenuse serveri domeeninimi või IP aadress
  j) cas_port - CAS teenuse port, 80/443/vms
  k) cas_path - CAS teenuse URI path, näiteks /cas/login
4) jMeter keskkonnas - "Main Thread Group":
  a) Number of Threads - paralleelsete kasutajate arv (soovituslik väärtus: 10-60)
  b) Ramp-Up - järgmise kasutaja süsteemi sisenemise ooteaeg sekundites (soovituslik väärtus: 5)
  c) Loop count - mitu korda teostab kasutaja määratud töövoogu (soovituslik väärtus: 10-20)
5) Mitmesugust:
  a) Delta rakenduse URL moodustatakse testiplaani muutujatest ${protocol}://${host}:${port}/dhs ehk Delta rakenduse context path peab olema /dhs
  b) CAS rakenduse login URL moodustatakse testiplaani muutujatest ${cas_protocol}://${cas_host}:${cas_port}/${cas_path}

Testi jooksutamiseks
--------------------
1) Avada JMeter'is delta.jmx
2) Seadistada lähteandmed ja testiplaani muutujad vastavalt eelmisele peatükile
3) Run -> Clear All
4) Run -> Start
5) Jooksvaid päringuid saab jälgida View Results Tree vaates

Eeldused Deltas olevate andmete kohta
-------------------------------------
1) Dokumentide loetelus eksisteerib vähemalt üks avatud funktsioon
2) Igas eelmises punktis vastavas funktsioonis eksisteerib vähemalt üks avatud sari, kuhu saab luua kõiki aktiivseid dokumendiliiki dokumente
3) Igas eelmises punktis vastavas sarjas eksisteerib vähemalt üks avatud funktsioon
4) Asjatoimiku liikide all on üks aktiivne asjatoimiku liik, mille id on "generalCaseFile"
5) Eelseadistatud terviktöövoogude all on üks terviktöövoog, mille nimi on "Universaalne"
6) Mallide all eksisteerib vähemalt üks dokumendi mall
7) Mallide all eksisteerib vähemalt üks e-kirja mall, mille nime algus on "text"
8) Delta konf.failis on kasutusel JuM haldusala seaded (conf.* seadete kohta)
=======

Deltasse lähteandmed
--------------------
Koormustesti jooksutamiseks sobivad lähteandmed tekivad andmete genereerimise
skriptist (ei ole veel olemas).

Praegu saab koormustesti jooksutada ka tühja repo peal või vigadega olemasoleva
repo peal - koormustest teeb mõned eeldused andmete osas:
* Et dok.liikidel on kindlad nimed ja kindlad väljad (nt. incomingLetter = Sissetulev kiri)

Kui on tühi repo, siis:
1) Importida dok.liigid failist documentTypes.xml
2) Importida klassifikaatorid failist classificators.xml
3) Luua vähemalt üks register, et saaks luua funktsioone, sarju, toimikuid
4) Luua funktsioonid, sarjad, toimikud, asjad nii et igat dok.liiki saaks salvestada
    vähemalt kahte erinevasse funktsiooni
    vähemalt kahte erinevasse sarja
    vähemalt kahte erinevasse toimikusse
    NB! Hetkel teha kõik toimikud ilma asjadeta! 
    (Võiks iga sarja juures lubada kõik kasutusel olevad dok.liigid)

Testi jooksutamiseks
--------------------
1) Kui jooksustati andmete genereerimise skripti, siis võtta ${dir.root}/users.csv
   fail ja asetada delta.jmx'iga samasse kausta
1) Avada JMeter'is delta.jmx
2) Vali "Delta load test" ja muuda protocol, host, port sobivaks
   Eeldus: Delta on nende parameetritega määratud serveris /dhs
   Eeldus: CAS on nende parameetritega määratud serveris /cas 
2) Run -> Clear All
3) Run -> Start
4) Jooksvaid päringuid saab jälgida View Results Tree vaates

Testide juurde lindistamiseks
-----------------------------
1) Select Workbench; File -> Merge -> select workbench-proxy-server.jmx
2) HTTP Proxy Server -> Port = määra selline et ei kattuks Delta Tomcat'i pordiga
5) HTTP Proxy Server -> Start
6) Brauseris seadista proxyks localhost ja punktis 2 määratud port
7) Brauseris mine Deltasse ja tegutse
8) Igat testjuhtumit peab alustama sellega, et klikkida logo peale!
9) Kustuta JMeteris ülearused päringud
10) Iga järelejäänud päringu juures tuleb kustutada javax.faces.ViewState parameeter
11) Muuda JMeteris päringuid - parametriseerida vajalikud väljad
12) Kindlasti lisada iga päringu kohta Assert, millegi uuel lehel sisalduva ja vanal lehel mittesisalduva kohta, et olla kindel et lehe vahetus toimus
      Add -> Assertions -> Response Assertion
        Patterns to Test -> Add -> kopeeri selle lehe HTML väljundist näiteks <title>...</title> osa;
        ja kui see pole unikaalne (nt. dok ekraani view -> edit mode vahetades sama title), siis lisaks või selle asemel muu osa!

Muud märkmed
------------
Näib et väljasid võib requestist puudu olla küll -- seega on lubatud et
koormustesti jooksutamisel on dok.liikidel natuke teistsugune komlekt
_mittekohustuslikke_ väljasid, kui oli koormustesti koostamisel (s.t. ei pea
importima documentTypes.xml), aga eeldusel et need väljad mida koormustest
väärtustab on sobivat andmetüüpi! Ja eeldusel et kohustuslikud väljad on samad!
Muidu tuleb lihtsalt vigu ja mõned testid ei tööta.

Eeldan, et toimib ka vastupidine, et requestis võib  olla rohkem välju,
selliseid mida serveri pool olemas ei ole ja requesti tegemisel neid
ignoreeritakse ja vigu ei tule.
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
