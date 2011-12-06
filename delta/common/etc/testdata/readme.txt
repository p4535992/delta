Testandmete genereerimine
-------------------------

1) Kopeerida kõik csv failid siit kaustast dir.root kausta
2) Kopeerida ADR failide kaust asukohta dir.root/contentstore/testfiles
   Seega peaksid ADR failid asuma
      dir.root/contentstore/testfiles/1/1234
      dir.root/contentstore/testfiles/1/5678
   jne.
3) Importida dok.liigid failist jmeter/documentTypes.xml
4) Importida klassifikaatorid failist jmeter/classificators.xml
5) Lucene indekseerimine lülitada välja parema kiiruse saavutamiseks, ja pärast
   teha ühekorraga järgi (nii nagu SIM 1.10 -> 2.5 juhendis kirjeldatud juuni
   2011).
6) Administraator -> Node'ide sirvija -> Käivita testandmete genereerimine
   Progressi saab jälgida logist.
7) Andmete genereerimise käivitamisel määratakse employeeRegReceiveUsersPeriod
   parameetri väärtuseks 500000, et loodud kasutajaid sünkimisel ei
   kustutataks. (TODO struktuuriüksuste sünkimine lülitada välja, et loodud
   struktuuriüksusi sünkimisel ei kustutataks.
8) Kasutajate loomise lõpus kirjutatakse kõik eksisteerivad kasutajanimed faili
   dir.root/users.csv
   Seda faili läheb vaja koormustestide sisendina.

Alusandmete hankimine ADR'ist
-----------------------------

1) Failis testdata_from_adr.sql muuta failide asukohad sobivaks
2) ADR rakenduse andmebaasis käivitada testdata_from_adr.sql
