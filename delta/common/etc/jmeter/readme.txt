
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
1) Kui jooksustati andmete genereerimise skripti, siis võtta dir.root/users.csv
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
