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
