Testandmete genereerimine
-------------------------

1) Kopeerida kõik csv failid siit kaustast ${dir.root} kausta
2) Kopeerida ADR failide kaust asukohta ${dir.root}/contentstore/testfiles
   Seega peaksid ADR failid asuma
      ${dir.root}/contentstore/testfiles/1/1234
      ${dir.root}/contentstore/testfiles/1/5678
   jne.
3) Importida dok.liigid failist jmeter/documentTypes.xml
4) Importida klassifikaatorid failist jmeter/classificators.xml
5) Kui kasutajaid ja/või struktuuriüksusi genereeritakse (genereeritakse siis
   kui lahtrites kasutajate arv / struktuuriüksuste on suuremad arvud kui
   praegu eksisteerivaid objekte), siis peab arvestama sellega, et järgmisel
   sünkimisel kustutatakse genereeritud objektid! Ainus võimalus sünkimise ära
   hoidmiseks on konf.failis ${amr.service.url} muuta
   mittetöötava/mitteeksisteeriva URL'i peale.
6) Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks, ja pärast
   teha ühekorraga järgi (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni
   2011).
7) Administraator -> Node'ide sirvija -> Käivita testandmete genereerimine
   Progressi saab jälgida logist.
8) Kasutajate loomise lõpus kirjutatakse kõik eksisteerivad kasutajanimed faili
   ${dir.root}/users.csv
   Seda faili läheb vaja koormustestide sisendina.

Alusandmete hankimine ADR'ist
-----------------------------

1) Failis testdata_from_adr.sql muuta failide asukohad sobivaks
2) ADR rakenduse andmebaasis käivitada testdata_from_adr.sql
