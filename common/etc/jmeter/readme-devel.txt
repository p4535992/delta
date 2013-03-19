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
_mittekohustuslikke_ väljasid, kui oli koormustesti koostamisel, aga eeldusel
et need väljad mida koormustest väärtustab on sobivat andmetüüpi! Ja eeldusel
et kohustuslikud väljad on samad! Muidu tuleb lihtsalt vigu ja mõned testid ei tööta.

Eeldan, et toimib ka vastupidine, et requestis võib  olla rohkem välju,
selliseid mida serveri pool olemas ei ole ja requesti tegemisel neid
ignoreeritakse ja vigu ei tule.

Kui mingi päring ei tööta, siis kontrolli encode linnukest, sest see kodeerib ka
param name'i ära (+ -> %3A).
