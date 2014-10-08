-- NB! Kui antud skripti kasutatakse hilisema kui 3.6.30 versiooi obfuskeerimiseks, ei pruugi see skript katta kõiki vajalikke andmeid. 
-- Näiteks uue õiguste süsteemi ülekandmist (alates versioon 5.0) see skript ei toeta, samuti võib olla muudatusi tööülesannete andmete osas.

-- Skripti kasutamine eeldab, et andmebaasis on olemas shcema nimega original, mis sisaldab algseid (obfuskeerimata) andmeid 
-- ning schema nimega public, mis sisaldab original schemaga sarnast struktuuri, kuid on tühi.
-- Seda võib luua järgmiselt:
-- 1) teha algsest muutmata kujul andmebaasist dump ainult struktuuriga 
--	(pg_dump.exe --host localhost --port 5432 --username "postgres" --no-password -s --format plain --verbose --file schema_without_data.sql dhs)
-- 2) andmebaasis nimetada public schema ümber original schemaks
--	(ALTER SCHEMA public RENAME TO original)
-- 3) Luua uus tühi schema nimega public
--	(CREATE SCHEMA public)
-- 3) importida uuesti punktis 1) eksporditud public schema struktuur. Veenduda, et public schema all olevad tabelid ei sisalda andmeid.
-- 4) Käivitada obfuskeerimise skript
-- 5) Kustutada baasist original schema




-- DROP TABLE tmp_isikud;

CREATE TABLE tmp_isikud
(
  isikukood character varying NOT NULL,
  eesnimi character varying NOT NULL,
  perekonnanimi character varying NOT NULL,
  row_num bigserial NOT NULL,
  amet character varying NOT NULL DEFAULT 'test-amet'::character varying,
  organisatsiooni_raja text[] DEFAULT ARRAY['Administratsioon'::text, 'Administratsioon, orkester'::text, 'Administratsioon, orkester, testosakond'::text]
);

CREATE TABLE tmp_eesnimed
(
  eesnimi character varying NOT NULL
);

CREATE TABLE tmp_perekonnanimed
(
  perekonnanimi character varying NOT NULL
);

INSERT INTO tmp_eesnimed (eesnimi) VALUES ('﻿Avo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hendrik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aado');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aadu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aadu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aadu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aarne');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Adolf');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Agnes');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ago-Endrik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Agu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ahti');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ahti');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aili');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aili');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aime');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aita');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Albert');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Albert');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksander');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aleksei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alfred');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alfred');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Algirdas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alice');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alissa');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Aljo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alla');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Allan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alma');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Alphonse');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andrei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andres');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Andrus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Angelina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anna');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anna');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anna-Maria');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anne');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anne');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anne');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anneli');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Annely');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Antoine de');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Antonín');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ants');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ants');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ants');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ants');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ants');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ants');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ants');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Anu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ao');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ardi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Are');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Arg');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Arg');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Arne');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Arne');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Artur');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Artur');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Artur');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Arvo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Asta');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Astrid');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Audrone');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('August');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('August');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('August');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('August');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('August von');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Avo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Benno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Bertolt');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Betti');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Bierute');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Boris');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Boris');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Bruno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Caëtano');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Camille');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Carl Robert');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Carlo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Charles');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Charles');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Dagmar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Dieter');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ede-Mall');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Edgar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Edgar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Edmund');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eduard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eduard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eduard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eduard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eduard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eero');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eero');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eevi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eike');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Einari');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elita');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ellen');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elli');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elmar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elmar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elmo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Els');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Elsa');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Emilie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Endel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ene');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Enn');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Enn');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Enn');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Enn');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Enno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Erik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Erik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Erika');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Erkki-Sven');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Erna-Johanna');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Erna-Vaike');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Erni');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ester');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ester');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eugen');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eugen Pavel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eugène');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eva');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eva');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Evald');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Evald');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Evald');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eve');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eve');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Eve');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Evi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Feliks');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Felix');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ferdinand');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('François Marie Arouet');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Friedebert');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Friedrich');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Friedrich Reinhold');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Galina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Garmen');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Georg');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Gerda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Gerda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Gert');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Gottfried');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Gunnar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Gunta');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Gustav');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hando');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hannes');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hans');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hans Christian');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hansen Anton');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hansi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Harald');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hardi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hariett');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Harri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Harri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Harry');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Harry');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Harry');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hedda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heikki');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heiko');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heinz');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helena');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helga');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helga');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heli');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Heljo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hella');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helle');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helle');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helma');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helmi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helmut');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Helvi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hendrik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hendrik jun');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Henno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Henrik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Henryk');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Henryk');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Herbert');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hermann');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Herta');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hiiet');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hiireke');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hilja');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hille');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hilli');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hugo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hugo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hugo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Hugo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Huko');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Igor');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Iir (pseud');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Iko');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ilmar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ilmar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ilme');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Imbi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Indrek');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ines');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Inge');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ingo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ingrid');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ingrid');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Inna');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Irene');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Irina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Irina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Irina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Irmgard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Isidor');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ivan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ivo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaagup');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaak');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaak');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaanus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jaanus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jakob');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jan Izydor');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Janno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jelena');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jevgeni');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jevgeni');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jindřich');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Joan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Joel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Joel Chandler');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Johann Sebastian');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Johann Wolfgang von');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Johannes');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Johannes');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jonathan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Josef');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Juhan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Juhan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Juhan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Juhan');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Juri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Juri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jutta');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Juura');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jürgo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jüri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jüri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jüri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jüri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Jüri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaarel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaarel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaarel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaarel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaarin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kadri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kai');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kalju');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kalju');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kalju');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kalju');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kalle');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kange');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karl');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karl');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karl');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karl Eduard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Karl Ernst');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kasesalu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kati');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Katrin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaupo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kaupo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kersti');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Koidula');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Konstantin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Konstantin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Konstantin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('kreeka lüürik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('kreeka lüürik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Krista');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Krista');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kristi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kuido');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kukepuu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kuldar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kustas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kustav-Agu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Kärt');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Külli');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('L. vt Tui');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Laine');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Laine');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Laura');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lauri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lea');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lea');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lea');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leelo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leenamari');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leenu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leida');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leida');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leida-Marie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lembit');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lembit');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lembo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lenora');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Leonhard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lewis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lidia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liisi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liivia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liivika');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Liivike');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lilian');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lilli Linda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Linda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Linda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Linda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Loreida');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ludmilla');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ludvig');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Luule');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Luule');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ly');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ly (Lydia)');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lydia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Lydia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Madis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Magnus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mai');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Maie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Maimu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Maks');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Male');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Malle');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Malle');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marcelijus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mare');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mare');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marge');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Margo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Margot');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Margot');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Margus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Margus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Margus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mari');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mari Ann');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Maria');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Maria');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Maria');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mari-Anne');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marika');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marika');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mario');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marju');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marko');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mart');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mart');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marta');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Marta-Marie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mati');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mati');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mati');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mati');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Matthias Johann');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mauri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Maurice');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Meelis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Meelis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Meelis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Meelis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Meeta');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Merle');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Meta');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mihhail');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mihkel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mihkel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mihkel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Miili');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Miina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mikko');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Milizia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Milvi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Muia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Murss');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Mägi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Natalie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Natalja');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Nigol');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Nikolai');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Nina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Oivi-Monika');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Olaf');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Olav');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Olev');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Olev');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Olga');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Olli');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Oscar Fingal O’Flahertie Wills');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Oskar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Oskar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Oskar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Oskar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ots');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Otto Ferdinand August Moritz');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Pablo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Paul');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Paul');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Paul');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Paul');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Paul');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Paul Otto Hermann');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Paul-Eerik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Pavel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Pedro August Pitka');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Peeter');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Peeter');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Peeter');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Peeter');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Peter');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Petras');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Pille');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Pille-Riin');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Pindam');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Pjotr');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Priit');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Priit');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Priit');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Priit');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Päären');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rabindranath');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raido');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raimo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raimond');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raimond');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raimund');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raivo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raivo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raivo (Raimond)');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Raivo E.');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rasmus Theodor');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rednar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Reeda');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Reet');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Reet');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Renate');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Riho');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Riho');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Riina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Riina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rimantas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rita');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rolf');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Romulus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rosita');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rudolf');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rudolf');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rudyard');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ruudu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Rünno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('S');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Salme');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Salme');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Salom');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Samuel');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Samuil');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sang');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Selma');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sergei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sergei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sergei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sergei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sergei');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('silmapaistvaim naislüürik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Silva');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Silvia Astrid');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sirppa');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sofia');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sven');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Sven');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Taavet');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Taavet (David');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Taavi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Taivo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Talvo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tamur');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tarmo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Teet');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tene');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tennessee');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Terje');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Terje');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Thomas Straussler');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tiina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tiina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tiina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tiina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tiit');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tiiu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tiiu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Toivo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Toivo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Toomas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Toomas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Toomas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tor Åge');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tove');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tõnis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tõnis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tõnis');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tõnson');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tõnu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tõnu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Tõnu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Uko');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Uno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Uno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Uno');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Urmas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Urmas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Urmas');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Urve');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Urvet');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vadim');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vaino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Valdek');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Valdo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Valentina');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Valeri');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Valfried');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Walt');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Valter');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vambola');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Veiko');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Veli');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vello');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vello');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vesca');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vidrik');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vidrik Rein');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Viiding');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Viiu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Viivi');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vija');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Viktor');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Villem');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('William');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Villu');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vitalijus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vladimir');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vladimir');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vladimir');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vladimir');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vladislav');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Voldemar-Siegfried');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Wolfgang Amadeus');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Votele');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Vova');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Väino');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ülev');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ülle Valve Oktavie');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ülo');
INSERT INTO tmp_eesnimed (eesnimi) VALUES ('Ülo');

INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('﻿Aaloe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aarma');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aasmäe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aavik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Adamson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Adamson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ader');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ader');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Adson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Agur');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ahas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aia');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aimla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ainula');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aisopos');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Alatalo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Alender');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Alev');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Allik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Allik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Alver');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Anakreon');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Andersen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Andresen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Andreste');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Andrianova');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Annus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ansomardi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Arbet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Arg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aro');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aru');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Arus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aule');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Aumere');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Avdjuško');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Babitševa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Bach');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Barabanštšikova');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Bažov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Bluzma');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Borde-Klein');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Brecht');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Brempel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Bringsværd');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Brook');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Büchner');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Carroll');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Chaplin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Collodi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Cvirka');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Cvirkiene');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Daudet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Demmeni');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Disney');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ditmann');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Donizetti');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Drda');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Driežis');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Druon');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Dvinjaninov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Dvořák');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Dürrenmatt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Eelmaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Eelmäe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Eensalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ehala');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ehasalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ehin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Einberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Einpalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Eisen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ekholm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Engelberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Erkina');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ernits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Esko');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Eskola');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Eskola');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Eskola');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Falk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Fedotov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Fiekova');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Fomitšev');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Gagarin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Gernet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Girdziauskaite');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Glazunova');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Goethe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Golditš');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Graps');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Greiffenhagen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Grimm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Gromov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Grünberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Gustavson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Gutman');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Haabjärv');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Haas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hakker');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Halla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hanstin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Harris');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Heinsalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Helbemäe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Helinurm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hermaküla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hermeliin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hiibus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hiir');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hindpere');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Holt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Holt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Hõimre');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Härma');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Iila');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ilves');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ionesco');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ird');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jakobson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jakobson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Janson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jansson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jessenin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Juhkum');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jurkowski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jõgi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jänes');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jänes');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Järs');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Järvet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Järvi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jürgo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Jürisson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kaddak');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kaktus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kaljumaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kaljumaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kalk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kall');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kallikorm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kalm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kalmet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kalmet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kalt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kampus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kangilaski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kangilaski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kangro');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kangro-Pool');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kangur');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kaplinski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kapp');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kappel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kapstas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karja');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karjus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karro');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karro');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karro');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karusoo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Karusoo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kasemets');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kasesalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kasesalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kask');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kask');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kask');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kass');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kaugver');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Keevallik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kelder');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kepp');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kerge');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kiivit');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kikerpuu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kilvet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kipling');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kitzberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kivi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kivikas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kivilo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kivirähk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kivirähk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kivirähk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kivistik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Klaus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Klink');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Koha');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kokk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kolár');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Komissarov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Komissarov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Konts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Koonen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Koorits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Koort');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Koppel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kordemets');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Korjus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Koroljov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Koržets');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kotkas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kotzebue');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kownacka');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kraam');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kraus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kreem');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kreem');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kreen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kreen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kreutzwald');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kress');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kristjan');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kross');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kruusenberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kuivjõgi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kukepuu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kukk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kukk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kull');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kuremaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kuulberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kuulpak');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kuus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kuusik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kõlar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kõrver');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kõrvits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kõrvits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kõrvits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kõrvits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Käo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Kütt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laabus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laasik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lagerlöf');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lahe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laht');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laidre');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laikmaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lattik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lauks');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lauk-Tamm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Laur');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lauri');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lauter');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lee');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Leetva');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Leies');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Leies');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lemba');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lemmik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lemmiste');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lensment');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Leomar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lepa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lepa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lepik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lepik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Leškin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Levin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Liblik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lieske');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Liigand');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Liigand');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Liivak');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Liives');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Link');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Linzbach');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lippasaar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lippur');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lomp');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lotman');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lott');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Luhavee');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Luik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lumet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lumiste');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lunge');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Luts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Luts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Luup');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lätte');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lätte');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Lüdig');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Maisaar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Maivel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Majakovski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Makedoonia kuningas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Malahhijeva');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Malík');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Manara');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mander');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mandri');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Maran');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Marandi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Maremäe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Markus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Maršak');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Martin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Martin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Martin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Martinaitis');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mazuras');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Matvere');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Meil');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Merilaas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mering');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mering');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Merits-Sepa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Merzin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mesikäpp');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mettus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mey (Mei)');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mihhalkov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mihkelson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mikkal');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mikluhho-Maklai');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mikutis');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mitt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mitt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Moor');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mozart');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Murss');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Murss-Põldar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Murutar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mustonen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mutsu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mutt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mäeots');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mägi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mägi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mägi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mägi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mänd');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Männik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Märska');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Mäser');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Müür');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Naissoo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Nebel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Neemre');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Neggo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Noris');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Neruda');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Nhari-Rudi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Niine');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Niit');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Niitvägi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Normet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Normet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Normet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Nuude');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Nõgisto');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Nõgisto');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Nüganen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Obraztsov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Oit');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ojakäär');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Orav');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Orgussaar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ostra-Oinas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ots');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ots');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Otsa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ott');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Paakspuu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Paaver');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pabut');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Paeorg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Paesüld');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pai');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Paiken');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Palaiologos');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Palm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Palmsaar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Panso');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Parve');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Paumann');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pedajas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pedriks');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Peedo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Peegel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Peil');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pennie');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Perrault');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pervik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Peterson-Särgava');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Peust');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pihlak');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Piirikivi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pikat');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pinna');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pirn');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pirnsalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Poller');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Poska');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pottisep');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Presjärv');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Prokofjev');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Promet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Purje');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Purje');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Purre');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Puudersell');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Puustusmaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Põldar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Põldma');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Põldmäe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Põldroos');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Põlla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Päiel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pärn');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pärt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Pärtel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Päts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Päts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Päts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Püssa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Püüman');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raadik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raid');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raid');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rand');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Randla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Randviir');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rannap');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('RatkeviÄ¨ius');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rattus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raud');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raud');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raud');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raudvee');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rauer');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Raus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rebane');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Reek');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Regi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Reiljan');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Reiman');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Reimann');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Reinla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Reinold');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rekand');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Renel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Renel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rink');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ripus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Roosa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Roosileht');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Roosileht');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rootare');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rosberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rost');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rotberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rummo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rummo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Runnel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ruubel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ruus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ruus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Rätsepp');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ryl');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Saar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Saar');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sahk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Saint-Exupery');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Saint-Saëns');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Saldre');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Salulaht');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sambla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sang');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sappho');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sats');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Saul');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sauter');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Savi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Savi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Seero');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sekk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Seliaru');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Seliaru');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Selirand');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Seljamaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Semjonova');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sepp');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Shakespeare');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sibul');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sihver');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Siirak');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Siirak');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sikkel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sild');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Silla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sink');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sivori-Asp');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Skomorowska');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Skupa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Smirnova');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Soodla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Soomre');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Soonik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Soonpää');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Soosalu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Spriit');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sztaundynger');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Stanislavski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Startšenko');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Stoppard');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Stravinski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sudaruškin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sudaruškin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Suits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Suits');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Suurallik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Swift');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sööt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Sööt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Süvalep');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Šapiro');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Šenhov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Šiškin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Špet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Štok');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Šubin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Švarts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Šarovtseva');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Žuhhovitskaja');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tabor');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tabor');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tagore');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tairov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tall');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Talvik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Talvik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tamberg');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tamm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tamm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tamm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tamm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tamm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tammert');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tammlaan');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tammsaare');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tammur');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tamre');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tarassov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tarkpea');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tarmo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tarto');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tauts');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Teetsov');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Terri');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tigane');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tihkan');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tiitus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tiks');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tinn');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tischler');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tohvelman');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tohver');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tolstoi');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tomingas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tomson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Toom');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tooming');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Toompere');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Toompere');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Toompere');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Toona');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Toots');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Topman(n)');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Torger');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tormis');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Trull');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Truu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tšaikovski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tubin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tuglas');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tui');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tumm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tungal');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tõnis');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tõnisson');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tätte');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Türnpu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Tüür');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Uder');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Uibo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Uibo');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ulla');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Under');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Ungvere');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Unt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Unt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Urbel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Urbel');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Urin');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Urvet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Urvet');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Uspenski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Uus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vaag');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vaarman');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vaga');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vaher');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vahing');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vahtrik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vaks');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vaks');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Valk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Valkonen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Valter');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vanhanen');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vapper');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Varango');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Varik');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vaus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Veerme');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Veetamm');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Veike');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vellerand');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Velt');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Veselý');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vetemaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vettus');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vihma');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Viiding');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Viilup');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Viisimaa');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vilba');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vilde');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Wilde');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Wilkowski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Williams');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vilms-Pool');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vilu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Vinter');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Visnap');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Visnapuu');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Volhovski');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Volmer');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Volodarskaja');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Voltaire');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Välbe');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Väljataga');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Värk');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Üksip');
INSERT INTO tmp_perekonnanimed (perekonnanimi) VALUES ('Üprus');

-- obfuskeeritud kasutajate loomine ajutisse tabelisse
with cte as (
select row_number() over() as row_num, eesnimi, perekonnanimi from (
	select eesnimi, perekonnanimi from
	(select eesnimi from tmp_eesnimed order by random() ) as eesnimed
	cross join (select perekonnanimi from tmp_perekonnanimed order by random()) as perekonnanimed
	order by random()) as nimed
)
insert into tmp_isikud (
select isikukood, eesnimi, perekonnanimi from (
select trim(to_char(10000000000 + row_number() OVER (), '99999999999')) as isikukood, 
	eesnimi, 
	perekonnanimi,
	nodes.*
 from (select alf_node.*, row_number() over() as row_num from original.alf_node  
	where type_qname_id in 
	(select alf_qname.id from original.alf_qname join original.alf_namespace ns on ns.id = alf_qname.ns_id 
	where local_name='person' and uri='http://www.alfresco.org/model/content/1.0')) as nodes
 left join cte on nodes.row_num = cte.row_num

	) as andmed
	);

-- crc32 funktsioon
CREATE OR REPLACE FUNCTION CRC32(VARCHAR) RETURNS BIGINT AS 
$BODY$
DECLARE
  src alias FOR $1;
  crc   BIGINT not null default x'ffffffff'::BIGINT;
  len   INTEGER not null default 0;
  i     INTEGER not null DEFAULT 1;
  crc_table BIGINT[] not null DEFAULT ARRAY[
  x'00000000'::BIGINT, x'77073096'::BIGINT, x'EE0E612C'::BIGINT, x'990951BA'::BIGINT,
  x'076DC419'::BIGINT, x'706AF48F'::BIGINT, x'E963A535'::BIGINT, x'9E6495A3'::BIGINT,
  x'0EDB8832'::BIGINT, x'79DCB8A4'::BIGINT, x'E0D5E91E'::BIGINT, x'97D2D988'::BIGINT,
  x'09B64C2B'::BIGINT, x'7EB17CBD'::BIGINT, x'E7B82D07'::BIGINT, x'90BF1D91'::BIGINT,
  x'1DB71064'::BIGINT, x'6AB020F2'::BIGINT, x'F3B97148'::BIGINT, x'84BE41DE'::BIGINT,
  x'1ADAD47D'::BIGINT, x'6DDDE4EB'::BIGINT, x'F4D4B551'::BIGINT, x'83D385C7'::BIGINT,
  x'136C9856'::BIGINT, x'646BA8C0'::BIGINT, x'FD62F97A'::BIGINT, x'8A65C9EC'::BIGINT,
  x'14015C4F'::BIGINT, x'63066CD9'::BIGINT, x'FA0F3D63'::BIGINT, x'8D080DF5'::BIGINT,
  x'3B6E20C8'::BIGINT, x'4C69105E'::BIGINT, x'D56041E4'::BIGINT, x'A2677172'::BIGINT,
  x'3C03E4D1'::BIGINT, x'4B04D447'::BIGINT, x'D20D85FD'::BIGINT, x'A50AB56B'::BIGINT,
  x'35B5A8FA'::BIGINT, x'42B2986C'::BIGINT, x'DBBBC9D6'::BIGINT, x'ACBCF940'::BIGINT,
  x'32D86CE3'::BIGINT, x'45DF5C75'::BIGINT, x'DCD60DCF'::BIGINT, x'ABD13D59'::BIGINT,
  x'26D930AC'::BIGINT, x'51DE003A'::BIGINT, x'C8D75180'::BIGINT, x'BFD06116'::BIGINT,
  x'21B4F4B5'::BIGINT, x'56B3C423'::BIGINT, x'CFBA9599'::BIGINT, x'B8BDA50F'::BIGINT,
  x'2802B89E'::BIGINT, x'5F058808'::BIGINT, x'C60CD9B2'::BIGINT, x'B10BE924'::BIGINT,
  x'2F6F7C87'::BIGINT, x'58684C11'::BIGINT, x'C1611DAB'::BIGINT, x'B6662D3D'::BIGINT,
  x'76DC4190'::BIGINT, x'01DB7106'::BIGINT, x'98D220BC'::BIGINT, x'EFD5102A'::BIGINT,
  x'71B18589'::BIGINT, x'06B6B51F'::BIGINT, x'9FBFE4A5'::BIGINT, x'E8B8D433'::BIGINT,
  x'7807C9A2'::BIGINT, x'0F00F934'::BIGINT, x'9609A88E'::BIGINT, x'E10E9818'::BIGINT,
  x'7F6A0DBB'::BIGINT, x'086D3D2D'::BIGINT, x'91646C97'::BIGINT, x'E6635C01'::BIGINT,
  x'6B6B51F4'::BIGINT, x'1C6C6162'::BIGINT, x'856530D8'::BIGINT, x'F262004E'::BIGINT,
  x'6C0695ED'::BIGINT, x'1B01A57B'::BIGINT, x'8208F4C1'::BIGINT, x'F50FC457'::BIGINT,
  x'65B0D9C6'::BIGINT, x'12B7E950'::BIGINT, x'8BBEB8EA'::BIGINT, x'FCB9887C'::BIGINT,
  x'62DD1DDF'::BIGINT, x'15DA2D49'::BIGINT, x'8CD37CF3'::BIGINT, x'FBD44C65'::BIGINT,
  x'4DB26158'::BIGINT, x'3AB551CE'::BIGINT, x'A3BC0074'::BIGINT, x'D4BB30E2'::BIGINT,
  x'4ADFA541'::BIGINT, x'3DD895D7'::BIGINT, x'A4D1C46D'::BIGINT, x'D3D6F4FB'::BIGINT,
  x'4369E96A'::BIGINT, x'346ED9FC'::BIGINT, x'AD678846'::BIGINT, x'DA60B8D0'::BIGINT,
  x'44042D73'::BIGINT, x'33031DE5'::BIGINT, x'AA0A4C5F'::BIGINT, x'DD0D7CC9'::BIGINT,
  x'5005713C'::BIGINT, x'270241AA'::BIGINT, x'BE0B1010'::BIGINT, x'C90C2086'::BIGINT,
  x'5768B525'::BIGINT, x'206F85B3'::BIGINT, x'B966D409'::BIGINT, x'CE61E49F'::BIGINT,
  x'5EDEF90E'::BIGINT, x'29D9C998'::BIGINT, x'B0D09822'::BIGINT, x'C7D7A8B4'::BIGINT,
  x'59B33D17'::BIGINT, x'2EB40D81'::BIGINT, x'B7BD5C3B'::BIGINT, x'C0BA6CAD'::BIGINT,
  x'EDB88320'::BIGINT, x'9ABFB3B6'::BIGINT, x'03B6E20C'::BIGINT, x'74B1D29A'::BIGINT,
  x'EAD54739'::BIGINT, x'9DD277AF'::BIGINT, x'04DB2615'::BIGINT, x'73DC1683'::BIGINT,
  x'E3630B12'::BIGINT, x'94643B84'::BIGINT, x'0D6D6A3E'::BIGINT, x'7A6A5AA8'::BIGINT,
  x'E40ECF0B'::BIGINT, x'9309FF9D'::BIGINT, x'0A00AE27'::BIGINT, x'7D079EB1'::BIGINT,
  x'F00F9344'::BIGINT, x'8708A3D2'::BIGINT, x'1E01F268'::BIGINT, x'6906C2FE'::BIGINT,
  x'F762575D'::BIGINT, x'806567CB'::BIGINT, x'196C3671'::BIGINT, x'6E6B06E7'::BIGINT,
  x'FED41B76'::BIGINT, x'89D32BE0'::BIGINT, x'10DA7A5A'::BIGINT, x'67DD4ACC'::BIGINT,
  x'F9B9DF6F'::BIGINT, x'8EBEEFF9'::BIGINT, x'17B7BE43'::BIGINT, x'60B08ED5'::BIGINT,
  x'D6D6A3E8'::BIGINT, x'A1D1937E'::BIGINT, x'38D8C2C4'::BIGINT, x'4FDFF252'::BIGINT,
  x'D1BB67F1'::BIGINT, x'A6BC5767'::BIGINT, x'3FB506DD'::BIGINT, x'48B2364B'::BIGINT,
  x'D80D2BDA'::BIGINT, x'AF0A1B4C'::BIGINT, x'36034AF6'::BIGINT, x'41047A60'::BIGINT,
  x'DF60EFC3'::BIGINT, x'A867DF55'::BIGINT, x'316E8EEF'::BIGINT, x'4669BE79'::BIGINT,
  x'CB61B38C'::BIGINT, x'BC66831A'::BIGINT, x'256FD2A0'::BIGINT, x'5268E236'::BIGINT,
  x'CC0C7795'::BIGINT, x'BB0B4703'::BIGINT, x'220216B9'::BIGINT, x'5505262F'::BIGINT,
  x'C5BA3BBE'::BIGINT, x'B2BD0B28'::BIGINT, x'2BB45A92'::BIGINT, x'5CB36A04'::BIGINT,
  x'C2D7FFA7'::BIGINT, x'B5D0CF31'::BIGINT, x'2CD99E8B'::BIGINT, x'5BDEAE1D'::BIGINT,
  x'9B64C2B0'::BIGINT, x'EC63F226'::BIGINT, x'756AA39C'::BIGINT, x'026D930A'::BIGINT,
  x'9C0906A9'::BIGINT, x'EB0E363F'::BIGINT, x'72076785'::BIGINT, x'05005713'::BIGINT,
  x'95BF4A82'::BIGINT, x'E2B87A14'::BIGINT, x'7BB12BAE'::BIGINT, x'0CB61B38'::BIGINT,
  x'92D28E9B'::BIGINT, x'E5D5BE0D'::BIGINT, x'7CDCEFB7'::BIGINT, x'0BDBDF21'::BIGINT,
  x'86D3D2D4'::BIGINT, x'F1D4E242'::BIGINT, x'68DDB3F8'::BIGINT, x'1FDA836E'::BIGINT,
  x'81BE16CD'::BIGINT, x'F6B9265B'::BIGINT, x'6FB077E1'::BIGINT, x'18B74777'::BIGINT,
  x'88085AE6'::BIGINT, x'FF0F6A70'::BIGINT, x'66063BCA'::BIGINT, x'11010B5C'::BIGINT,
  x'8F659EFF'::BIGINT, x'F862AE69'::BIGINT, x'616BFFD3'::BIGINT, x'166CCF45'::BIGINT,
  x'A00AE278'::BIGINT, x'D70DD2EE'::BIGINT, x'4E048354'::BIGINT, x'3903B3C2'::BIGINT,
  x'A7672661'::BIGINT, x'D06016F7'::BIGINT, x'4969474D'::BIGINT, x'3E6E77DB'::BIGINT,
  x'AED16A4A'::BIGINT, x'D9D65ADC'::BIGINT, x'40DF0B66'::BIGINT, x'37D83BF0'::BIGINT,
  x'A9BCAE53'::BIGINT, x'DEBB9EC5'::BIGINT, x'47B2CF7F'::BIGINT, x'30B5FFE9'::BIGINT,
  x'BDBDF21C'::BIGINT, x'CABAC28A'::BIGINT, x'53B39330'::BIGINT, x'24B4A3A6'::BIGINT,
  x'BAD03605'::BIGINT, x'CDD70693'::BIGINT, x'54DE5729'::BIGINT, x'23D967BF'::BIGINT,
  x'B3667A2E'::BIGINT, x'C4614AB8'::BIGINT, x'5D681B02'::BIGINT, x'2A6F2B94'::BIGINT, 
  x'B40BBE37'::BIGINT, x'C30C8EA1'::BIGINT, x'5A05DF1B'::BIGINT, x'2D02EF8D'::BIGINT
  ];
BEGIN
  len := CHAR_LENGTH(src);
  while i <= len loop
    crc := (crc >> 8) # crc_table[((crc & x'FF'::BIGINT) # ascii(substr(src, i , 1))) + 1];
    i := i + 1;
  END loop;
  return crc # x'FFFFFFFF'::BIGINT;
END
$BODY$
LANGUAGE plpgsql;

-- alf_namespace muutmata kujul
insert into public.alf_namespace (select * from original.alf_namespace);

-- alf_attributes muutmata kujul
insert into public.alf_attributes (select * from original.alf_attributes);

-- alf_global_attributes muutmata kujul
insert into public.alf_global_attributes (select * from original.alf_global_attributes);

-- alf_map_attribute_entries muutmata kujul
insert into public.alf_map_attribute_entries (select * from original.alf_map_attribute_entries);

-- alf_mimetype muutmata kujul
insert into public.alf_mimetype (select * from original.alf_mimetype);

-- alf_qname muutmata kujul
insert into public.alf_qname (select * from original.alf_qname);

-- alf_locale muutmata kujul
insert into public.alf_locale (select * from original.alf_locale);

-- alf_server muutmata kujul
insert into public.alf_server (select * from original.alf_server);

-- alf_transaction muutmata kujul
insert into public.alf_transaction (select * from original.alf_transaction);

-- alf_authority
-- 1) süsteemsed kasutajad ja grupid muutmata kujul.
insert into public.alf_authority (select * from original.alf_authority where authority is null 
	or authority in ('', 'guest', 'ROLE_OWNER', 'GROUP_ALFRESCO_ADMINISTRATORS', 'GROUP_ARCHIVISTS', 'GROUP_DOCUMENT_MANAGERS', 'GROUP_EVERYONE', 'GROUP_SUPERVISION'));
	
insert into public.alf_authority (select id, version,
	concat('GROUP_', case when position(',' in authority) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(authority, '(,)'), 1)) end, id ),
	crc32(concat('GROUP_', case when position(',' in authority) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(authority, '(,)'), 1)) end, id ))
	from original.alf_authority
	where authority like 'GROUP_%' and authority not in ('GROUP_ALFRESCO_ADMINISTRATORS', 'GROUP_ARCHIVISTS', 'GROUP_DOCUMENT_MANAGERS', 'GROUP_EVERYONE', 'GROUP_SUPERVISION'));
	
-- 2) ülejäänud kasutajad asendatakse suvaliste kasutajatega ajutisest tmp_isikud tabelist
-- See obfuskeerib ühtlasi ka õigused, kuna õiguste kirjed alf_access_control_entry tabelis viitavad sellele tabelile
insert into public.alf_authority (select id, version, 
	case when mock_authority is not null 
		then mock_authority
	else concat('mock_', orig_authorities.row_num::BIGINT::TEXT) end, 
	crc32(case when mock_authority is not null 
		then mock_authority
	else concat('mock_', orig_authorities.row_num::BIGINT::TEXT) end) from 
	(select row_number() over() as row_num, original.alf_authority.* from original.alf_authority 
		where not (authority is null or authority in ('', 'guest', 'ROLE_OWNER') or authority like 'GROUP_%')) as orig_authorities
	left join (select row_number() over () as row_num, isikukood as mock_authority from tmp_isikud) as isikud on isikud.row_num = orig_authorities.row_num
	where mock_authority is not null
);

-- õiguste tabelid
insert into public.alf_permission (select * from original.alf_permission);
insert into public.alf_acl_change_set (select * from original.alf_acl_change_set);
insert into public.alf_ace_context (select * from original.alf_ace_context);
insert into public.alf_access_control_list (select * from original.alf_access_control_list);

alter table public.alf_acl_member drop constraint fk_alf_aclm_ace;

insert into public.alf_access_control_entry (select alf_access_control_entry.* from original.alf_access_control_entry join public.alf_authority authority on authority.id = alf_access_control_entry.authority_id);
insert into public.alf_acl_member (select aclm.* from original.alf_acl_member aclm join public.alf_access_control_entry ace on ace.id = aclm.ace_id);
alter table public.alf_acl_member add constraint fk_alf_aclm_ace FOREIGN KEY (ace_id)
      REFERENCES public.alf_access_control_entry (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

-- alf_node tabelist store root'ide ülekandmiseks vajalikud tegevused
alter table only public.alf_node drop constraint fk_alf_node_store;

insert into public.alf_node (select * from original.alf_node where type_qname_id in 
	(select alf_qname.id from alf_qname join alf_namespace ns on ns.id = alf_qname.ns_id where local_name='store_root' and uri='http://www.alfresco.org/model/system/1.0'));

insert into public.alf_store (select * from original.alf_store);

ALTER TABLE only public.alf_node
    ADD CONSTRAINT fk_alf_node_store FOREIGN KEY (store_id) REFERENCES alf_store(id);

-- alf_content_url muutmata kujul
insert into public.alf_content_url (select * from original.alf_content_url);

insert into public.alf_encoding (select * from original.alf_encoding);

-- alf_content_data muutmata kujul
insert into public.alf_content_data (select * from original.alf_content_data);

-- delta_log obfuskeerimine.
insert into public.delta_log (
	select log_entry_id, created_date_time, level, (case when creator_id = 'DHS' then 'DHS' else tmp_isikud.isikukood end), (case when creator_name = 'DHS' then 'DHS' else concat(tmp_isikud.eesnimi, ' ', tmp_isikud.perekonnanimi) end), '0.0.0.0', 
		rpad('computer', length(computer_name), ' computer'), object_id, object_name, 
		rpad('obfuskeeritud', length(description), ' obfuskeeritud obfusk ') 
	from (select *, random() as random from original.delta_log) as d_log
	join tmp_isikud on ceil(d_log.random * (select count(*) from tmp_isikud)) = tmp_isikud.row_num
);

-- delta_log_date muutmata kujul
insert into public.delta_log_date (select * from original.delta_log_date);

-- delta_log_level muutmata kujul
insert into public.delta_log_level (select * from original.delta_log_level);

-- delta_register muutmata kujul
insert into public.delta_register (select * from original.delta_register);

-- delta_task obfuskeerimine
insert into public.delta_task (
	select task_id, workflow_id, task_type, index_in_workflow, wfc_status, concat(isikud_creator.eesnimi, ' ', isikud_creator.perekonnanimi),
		wfc_started_date_time, wfc_stopped_date_time, isikud_owner.isikukood, concat(isikud_owner.eesnimi, ' ', isikud_owner.perekonnanimi),
		isikud_previous_owner.isikukood, 'obf@obf.obf', wfc_owner_group, wfc_outcome, wfc_document_type, wfc_completed_date_time, isikud_owner.amet,
		wfc_parallel_tasks, isikud_creator.isikukood, 'obf@obf.obf', rpad('töövoo resolutsioon', length(wfs_workflow_resolution), ' resolutsioon reso aga see on juba pikem reso'),
		wfs_completed_overdue, wfs_due_date, 
		  wfs_due_date_days, wfs_is_due_date_days_working_days,
		rpad('tööülesande kommentaar', length(wfs_comment), ' kommentaar'),
		rpad('1.1, 1.2', length(wfs_file_versions), ', 1.3, 1.4, 1.5'), rpad('asutus', length(wfs_institution_name), ' asutuse nimi'),
		rpad('asutuse kood ', length(wfs_institution_code), ' kood '), rpad('saatja asutuse kood ', length(wfs_creator_institution_code), ' kood'),
		(case when wfs_original_dvk_id is not null then -1 else null end),
		(case when wfs_sent_dvk_id is not null then -1 else null end),
		(case when wfs_recieved_dvk_id is not null then -1 else null end),
		wfs_send_status, wfs_send_date_time, rpad('resolutsioon', length(wfs_resolution), ' reso'),
		wfs_temp_outcome, wfs_active, wfs_send_order_assignment_completed_email, wfs_proposed_due_date,
		wfs_confirmed_due_date, has_due_date_history, is_searchable, isikud_owner.organisatsiooni_raja,
		store_id
	from (select *, (case when wfs_creator_id is not null then random() else null end) as random1, 
		(case when wfc_owner_id is not null then random() else null end) as random2, 
		(case when wfc_previous_owner_id is not null then random() else null end) as random3 from original.delta_task) as d_task
	left join tmp_isikud as isikud_creator on ceil(d_task.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_owner on ceil(d_task.random2 * (select count(*) from tmp_isikud)) = isikud_owner.row_num
	left join tmp_isikud as isikud_previous_owner on ceil(d_task.random3 * (select count(*) from tmp_isikud)) = isikud_previous_owner.row_num
);

-- delta_task_due_date_extension_assoc muutmata kujul
insert into public.delta_task_due_date_extension_assoc (select * from original.delta_task_due_date_extension_assoc);

-- delta_task_due_date_history obfuskeerimine
insert into public.delta_task_due_date_history (
	select task_due_date_history_id, task_id, previous_date, rpad('Põhjus', length(change_reason), ' pikem põhjendus') from original.delta_task_due_date_history
);

-- delta_task_file muutmata kujul
insert into public.delta_task_file (select * from original.delta_task_file);

-- alf_node tüübi alusel alf_node ja alf_node_properties ülekandmine
-- 1) Dokumentidel obfuskeeritakse kõik tekstilised andmed, arvestades allpool toodud erandjuhte.
--	Vastavalt SMIT sisendile ei ole vajalik kuupäevade ja numbrite obfuskeerimine.
--	Ei obfuskeerita süsteemseid väärtuseid:
--	{http://alfresco.webmedia.ee/model/document/admin/1.0}objectTypeId
--	{http://alfresco.webmedia.ee/model/document/admin/1.0}objectTypeVersionNr
--	{http://alfresco.webmedia.ee/model/document/common/1.0}searchableHasAllFinishedCompoundWorkflows
--	{http://alfresco.webmedia.ee/model/document/common/1.0}searchableHasStartedCompoundWorkflows
--	{http://alfresco.webmedia.ee/model/document/common/1.0}updateMetadataInFiles
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}function
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}series
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}volume
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}case
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}templateName
--	
--	Üldse ei kanta üle:
--	{http://alfresco.webmedia.ee/model/document/common/1.0}searchableSendInfoDateTime
--	{http://alfresco.webmedia.ee/model/document/common/1.0}searchableSendInfoRecipient
--	{http://alfresco.webmedia.ee/model/document/common/1.0}searchableSendInfoResolution
--	{http://alfresco.webmedia.ee/model/document/common/1.0}searchableSendMode
-- 	{http://alfresco.webmedia.ee/model/document/common/1.0}fileContents
--	{http://alfresco.webmedia.ee/model/document/common/1.0}fileNames
--
-- Dokumendi vastutaja andmed obfuskeeritakse suvaliste andmetega tmp_isikud tabelist
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}ownerEmail	
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}ownerId	
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}ownerJobTitle	
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}ownerName	
--	{http://alfresco.webmedia.ee/model/document/dynamic/1.0}ownerOrgStructUnit
--
-- Juurdepääsupiirang {http://alfresco.webmedia.ee/model/document/dynamic/1.0}accessRestriction
-- asendatakse juhusliku väärtusega hulgast 'Avalik', 'AK', 'Majasisene', 'Piiratud'

-- Serialiseeritud väärtused obfuskeeritakse vastavalt tüübile kas tekstiliseks või java ArrayList tüüpi konstandiks

-- alf_node tabelis obfuskeeritakse creator_id ja modifier_id väljad
insert into public.alf_node (
	select id, version, store_id, uuid, transaction_id, node_deleted, type_qname_id, acl_id,
	(case when audit_creator = 'System' then audit_creator else isikud_creator.isikukood end),
	audit_created,
	(case when audit_modifier = 'System' then audit_modifier else isikud_modifier.isikukood end),
	audit_modified, audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node where type_qname_id in 
			(select alf_qname.id from alf_qname join alf_namespace ns on ns.id = alf_qname.ns_id where local_name='document' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')) as node
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
);

insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(local_name = 'objectTypeId' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'objectTypeVersionNr' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'searchableHasAllFinishedCompoundWorkflows' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'searchableHasStartedCompoundWorkflows' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'updateMetadataInFiles' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'function' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'series' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'volume' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'case' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'templateName' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	when (local_name = 'ownerId' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		then isikud_owner.isikukood
	when (local_name = 'ownerName' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		then concat(isikud_owner.eesnimi, ' ', isikud_owner.perekonnanimi)
	when (local_name = 'ownerEmail' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		then 'obf@obf.obf'		
	when (local_name = 'ownerJobTitle' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		then isikud_owner.amet	
	when (local_name = 'accessRestriction' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		then (ARRAY['Avalik', 'AK', 'Majasisene', 'Piiratud'])[ceil(random()*4)]
	else rpad('obfuskeeritud', length(string_value), ' obfuskeeritud')
	end,
	case when serializable_value is null 
		then null
	when
		(local_name = 'objectTypeId' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'objectTypeVersionNr' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'searchableHasAllFinishedCompoundWorkflows' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'searchableHasStartedCompoundWorkflows' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'updateMetadataInFiles' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'function' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'series' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'volume' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'case' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (local_name = 'templateName' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		then serializable_value
	else
		case when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
		else 
		'\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
		end	
	end,
	qname_id, list_index, locale_id
	from 
	(select alf_node.*, random() as random from original.alf_node where type_qname_id in 
		(select alf_qname.id from original.alf_qname join original.alf_namespace ns on ns.id = original.alf_qname.ns_id where local_name='document' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')) as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud as isikud_owner on ceil(node.random * (select count(*) from tmp_isikud)) = isikud_owner.row_num
	where not  
		((local_name='searchableSendInfoDateTime' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name='searchableSendInfoRecipient' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name='searchableSendInfoResolution' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name='searchableSendMode' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name='fileContents' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name='fileNames' and uri='http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name='expiryDate' and uri='http://www.alfresco.org/model/content/1.0')
		or (local_name='lockOwner' and uri='http://www.alfresco.org/model/content/1.0')
		or (local_name='lockType' and uri='http://www.alfresco.org/model/content/1.0')
		or (local_name='lockedFileNodeRef' and uri='http://alfresco.webmedia.ee/model/file/1.0'))
);

-- 8) document tüüpi node'ide alamnode'idel (tuvastatakse nimeruumiga http://alfresco.webmedia.ee/model/document/child/1.0) obfuskeeritakse kõik tekstilised andmed suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where uri = 'http://alfresco.webmedia.ee/model/document/child/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_ns.uri = 'http://alfresco.webmedia.ee/model/document/child/1.0'
);

-- 2) content tüüpi node'id - üle kantakse need, mis asuvad dokumendi, töövoo või cm:folder tüüpi node'ide all (sisaldab Alfresco süsteemseid malle ja Delta malle)
-- Teisi faile üle ei kanta (nt prügikastis olevaid faile, mis ei asu dokumentide all), kuna on võimalik, et ajalooliselt on lisatud veel mingeid faile, mille sisu ja asukoha kohta pole kindlat infot.

-- ajutine tabel faili node'ide id-de jaoks
-- DROP TABLE tmp_content_node_ids;

CREATE TABLE tmp_content_node_ids
(
  id bigint not null unique,
  nimi_obfuskeerida boolean not null
);

insert into tmp_content_node_ids (
	select node.id, 
	case when (local_name = 'document' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'reportResult' and uri = 'http://alfresco.webmedia.ee/model/report/1.0')
		or (local_name = 'workflow' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then true
	else false end
	from (
		select * from original.alf_node where type_qname_id in 
			(select alf_qname.id from alf_qname join alf_namespace ns on ns.id = alf_qname.ns_id where local_name='content' and uri='http://www.alfresco.org/model/content/1.0')) as node
	join original.alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	join original.alf_node parent on parent.id = child_assoc.parent_node_id
	join original.alf_qname qname on qname.id = parent.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	where (local_name = 'folder' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'document' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'reportResult' and uri = 'http://alfresco.webmedia.ee/model/report/1.0')
		or (local_name = 'workflow' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
);

insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	join tmp_content_node_ids on tmp_content_node_ids.id = node.id
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (local_name = 'reportOutputType' and uri = 'http://alfresco.webmedia.ee/model/document-template/1.0')
		or (local_name = 'reportType' and uri = 'http://alfresco.webmedia.ee/model/document-template/1.0')
		or (local_name = 'templateType' and uri = 'http://alfresco.webmedia.ee/model/document-template/1.0')
		or (local_name = 'name' and uri = 'http://alfresco.webmedia.ee/model/document-template/1.0')
		or (local_name = 'title' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'source' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	when (local_name = 'lastName' and uri = 'http://alfresco.webmedia.ee/model/versions/1.0')
		then isikud_versioon.perekonnanimi
	when (local_name = 'firstName' and uri = 'http://alfresco.webmedia.ee/model/versions/1.0')
		then isikud_versioon.eesnimi		
	when (local_name = 'displayName' and uri = 'http://alfresco.webmedia.ee/model/file/1.0')
		or (local_name = 'name' and uri = 'http://www.alfresco.org/model/content/1.0')
		then
		case when tmp_content_node_ids.nimi_obfuskeerida  
			then
			-- use dot plus 5 characters after last dot as extension in case there is actually no extension and . is in the middel of sensitive data.
			-- 5 characters seems reasonably small amount of data that it cannot contain any amount of information that would make sense without context
			concat('fail_', props.row_num, (case when position('.' in string_value) > 0 
							then substring(string_value from (length(string_value) - position('.' in reverse(string_value)) + 1) for (length(string_value) - position('.' in reverse(string_value)) + 6)) 
							else '' end) )
		else string_value end
	else rpad('obfuskeeritud', length(string_value), ' obfusk')
	end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,	
	qname_id, list_index, locale_id
	from 
	(select alf_node.*, random() as random from original.alf_node ) as node
	join tmp_content_node_ids on tmp_content_node_ids.id = node.id
	join (select *, row_number() over () as row_num from original.alf_node_properties) props on props.node_id = node.id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud as isikud_versioon on ceil(node.random * (select count(*) from tmp_isikud)) = isikud_versioon.row_num
);

-- 3) Süsteemsed node tüübid, kantakse üle muutmata kujul, v.a. alf_node.creator ja alf_node.modifier väljad, mis obfuskeeritakse suvaliste isikukoodidega tmp_isikud tabelist
-- Tuleks üle vaadata käsitsi, et ei sisaldaks konfidentsiaalset infot.
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where (local_name = 'container' and uri = 'http://www.alfresco.org/model/system/1.0')
		or (local_name = 'descriptor' and uri = 'http://www.alfresco.org/model/system/1.0')
		or (local_name = 'cmobject' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'category_root' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'category' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'mlRoot' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'systemfolder' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'rule' and uri = 'http://www.alfresco.org/model/rule/1.0')
		or (local_name = 'compositeaction' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'actioncondition' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'actionparameter' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'action' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'sites' and uri = 'http://www.alfresco.org/model/site/1.0')
		or (local_name = 'sites' and uri = 'http://www.alfresco.org/model/site/1.0')
		or (local_name = 'zone' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'addressbook' and uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0')
		or (local_name = 'parameters' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
		or (local_name = 'classificators' and uri = 'http://alfresco.webmedia.ee/model/classificator/1.0')
		or (local_name = 'drafts' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'adrDeletedDocuments' and uri = 'http://alfresco.webmedia.ee/model/adr/1.0')
		or (local_name = 'authorityContainer' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'taskSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/task/search/1.0')
		or (local_name = 'documentReportFilters' and uri = 'http://alfresco.webmedia.ee/model/document/report/1.0')
		or (local_name = 'documentSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/document/search/1.0')
		or (local_name = 'taskReportFilters' and uri = 'http://alfresco.webmedia.ee/model/task/report/1.0')
		or (local_name = 'thesauri' and uri = 'http://alfresco.webmedia.ee/model/thesaurus/1.0')
		or (local_name = 'registers' and uri = 'http://alfresco.webmedia.ee/model/register/1.0')
		or (local_name = 'reportsQueue' and uri = 'http://alfresco.webmedia.ee/model/report/1.0')
		or (local_name = 'documentTypes' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'fieldDefinitions' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'fieldGroupDefinitions' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'functions' and uri = 'http://alfresco.webmedia.ee/model/functions/1.0')
		or (local_name = 'imapFolder' and uri = 'http://alfresco.webmedia.ee/model/imap/1.0')
		or (local_name = 'compoundWorkflowDefinitions' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		or (local_name = 'scanned' and uri = 'http://alfresco.webmedia.ee/model/scanned/1.0')
		or (local_name = 'orgstructs' and uri = 'http://alfresco.webmedia.ee/model/orgstructure/1.0')
		or (local_name = 'caseFileTypes' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'dimensions' and uri = 'http://alfresco.webmedia.ee/model/dimension/1.0')
		or (local_name = 'dimension' and uri = 'http://alfresco.webmedia.ee/model/dimension/1.0')
		or (local_name = 'configurations' and uri = 'http://www.alfresco.org/model/application/1.0')
		or (local_name = 'transactionDescParameters' and uri = 'http://alfresco.webmedia.ee/model/transactionDescParameter/1.0')
		or (local_name = 'transactionDescParameter' and uri = 'http://alfresco.webmedia.ee/model/transactionDescParameter/1.0')
		or (local_name = 'transactionTemplates' and uri = 'http://alfresco.webmedia.ee/model/transaction/1.0')
		or (local_name = 'genNotifications' and uri = 'http://alfresco.webmedia.ee/model/notification/1.0')
		or (local_name = 'separationLine' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'followupAssociation' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'fieldMapping' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'orgstruct' and uri = 'http://alfresco.webmedia.ee/model/orgstructure/1.0')
		or (local_name = 'substitutes' and uri = 'http://alfresco.webmedia.ee/model/substitute/1.0')
		or (local_name = 'privilegeActionsQueue' and uri = 'http://alfresco.webmedia.ee/model/privilege/1.0')
		or (local_name = 'archivalsQueue' and uri = 'http://alfresco.webmedia.ee/model/archivals/1.0')
		or (local_name = 'volumeReportFilters' and uri = 'http://alfresco.webmedia.ee/model/volume/report/1.0')
		or (local_name = 'cwSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/cw/search/1.0')
		or (local_name = 'volumeSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/volume/search/1.0')
		or (local_name = 'independentCompoundWorkflows' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		or (local_name = 'linkedReviewTasks' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
		or (local_name = 'intParameter' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
		or (local_name = 'stringParameter' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
		or (local_name = 'doubleParameter' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
	
);

insert into public.alf_node_properties (
	select props.* from original.alf_node node
	join original.alf_node_properties props on node.id = props.node_id
	join original.alf_qname qname on node.type_qname_id = qname.id
	join original.alf_namespace namespace on qname.ns_id = namespace.id
	where (local_name = 'container' and uri = 'http://www.alfresco.org/model/system/1.0')
		or (local_name = 'descriptor' and uri = 'http://www.alfresco.org/model/system/1.0')
		or (local_name = 'cmobject' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'category_root' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'category' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'mlRoot' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'systemfolder' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'rule' and uri = 'http://www.alfresco.org/model/rule/1.0')
		or (local_name = 'compositeaction' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'actioncondition' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'actionparameter' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'action' and uri = 'http://www.alfresco.org/model/action/1.0')
		or (local_name = 'sites' and uri = 'http://www.alfresco.org/model/site/1.0')
		or (local_name = 'sites' and uri = 'http://www.alfresco.org/model/site/1.0')
		or (local_name = 'zone' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'addressbook' and uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0')
		or (local_name = 'parameters' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
		or (local_name = 'classificators' and uri = 'http://alfresco.webmedia.ee/model/classificator/1.0')
		or (local_name = 'drafts' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'adrDeletedDocuments' and uri = 'http://alfresco.webmedia.ee/model/adr/1.0')
		or (local_name = 'authorityContainer' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'taskSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/task/search/1.0')
		or (local_name = 'documentReportFilters' and uri = 'http://alfresco.webmedia.ee/model/document/report/1.0')
		or (local_name = 'documentSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/document/search/1.0')
		or (local_name = 'taskReportFilters' and uri = 'http://alfresco.webmedia.ee/model/task/report/1.0')
		or (local_name = 'thesauri' and uri = 'http://alfresco.webmedia.ee/model/thesaurus/1.0')
		or (local_name = 'registers' and uri = 'http://alfresco.webmedia.ee/model/register/1.0')
		or (local_name = 'reportsQueue' and uri = 'http://alfresco.webmedia.ee/model/report/1.0')
		or (local_name = 'documentTypes' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'documentTypes' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'fieldDefinitions' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'fieldGroupDefinitions' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'functions' and uri = 'http://alfresco.webmedia.ee/model/functions/1.0')
		or (local_name = 'imapFolder' and uri = 'http://alfresco.webmedia.ee/model/imap/1.0')
		or (local_name = 'compoundWorkflowDefinitions' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		or (local_name = 'scanned' and uri = 'http://alfresco.webmedia.ee/model/scanned/1.0')
		or (local_name = 'orgstructs' and uri = 'http://alfresco.webmedia.ee/model/orgstructure/1.0')	
		or (local_name = 'caseFileTypes' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'dimension' and uri = 'http://alfresco.webmedia.ee/model/dimension/1.0')
		or (local_name = 'configurations' and uri = 'http://www.alfresco.org/model/application/1.0')
		or (local_name = 'transactionDescParameters' and uri = 'http://alfresco.webmedia.ee/model/transactionDescParameter/1.0')
		or (local_name = 'transactionDescParameter' and uri = 'http://alfresco.webmedia.ee/model/transactionDescParameter/1.0')
		or (local_name = 'transactionTemplates' and uri = 'http://alfresco.webmedia.ee/model/transaction/1.0')
		or (local_name = 'genNotifications' and uri = 'http://alfresco.webmedia.ee/model/notification/1.0')
		or (local_name = 'separationLine' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'followupAssociation' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'fieldMapping' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (local_name = 'orgstruct' and uri = 'http://alfresco.webmedia.ee/model/orgstructure/1.0')
		or (local_name = 'substitutes' and uri = 'http://alfresco.webmedia.ee/model/substitute/1.0')
		or (local_name = 'privilegeActionsQueue' and uri = 'http://alfresco.webmedia.ee/model/privilege/1.0')
		or (local_name = 'archivalsQueue' and uri = 'http://alfresco.webmedia.ee/model/archivals/1.0')
		or (local_name = 'volumeReportFilters' and uri = 'http://alfresco.webmedia.ee/model/volume/report/1.0')
		or (local_name = 'cwSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/cw/search/1.0')
		or (local_name = 'volumeSearchFilters' and uri = 'http://alfresco.webmedia.ee/model/volume/search/1.0')
		or (local_name = 'independentCompoundWorkflows' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		or (local_name = 'linkedReviewTasks' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
		or (local_name = 'reportResult' and uri = 'http://alfresco.webmedia.ee/model/report/1.0')
		or (local_name = 'intParameter' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
		or (local_name = 'stringParameter' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
		or (local_name = 'doubleParameter' and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0')
		
);

-- 4) folder tüüpi node'id - nendel, mis asuvad userHomes all, obfuskeeritakse isikukoodid;
-- ülejäänud kantakse muutmata kujul üle
CREATE TABLE tmp_userhome_child_node_ids
(
  id bigint not null unique
);

insert into tmp_userhome_child_node_ids (
	select distinct node.id from original.alf_node node
	join alf_qname qname on node.type_qname_id = qname.id
	join alf_namespace ns on ns.id = qname.ns_id
	join alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	join alf_node parent on parent.id = child_assoc.parent_node_id
	join alf_child_assoc grand_child_assoc on grand_child_assoc.child_node_id = parent.id
	join alf_node grand_parent on grand_parent.id = grand_child_assoc.parent_node_id
	where local_name = 'folder' and uri = 'http://www.alfresco.org/model/content/1.0' and grand_child_assoc.qname_localname = 'userHomes'
);

insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'folder' and uri = 'http://www.alfresco.org/model/content/1.0'
);

insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when qname.local_name = 'name' and ns.uri = 'http://www.alfresco.org/model/content/1.0' and node.id in (select * from tmp_userhome_child_node_ids)
		then tmp_isikud.isikukood
	else string_value end,
	serializable_value,
	qname_id, list_index, locale_id
	from 
	(select original.alf_node.*, random() as random from original.alf_node ) as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud on ceil(node.random * (select count(*) from tmp_isikud)) = tmp_isikud.row_num
	where type_qname.local_name = 'folder' and type_ns.uri = 'http://www.alfresco.org/model/content/1.0'
	order by node_id
);

-- 5) person tüüpi node'idel obfuskeeritakse owner, userName, firstName, lastName, jobtitle väärtused suvalise isiku väärtustega tabelist tmp_isikud; 
-- ei obfuskeerita shortcuts väärtuseid (kasutaja lemmikud)
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliselt.
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'person' and uri = 'http://www.alfresco.org/model/content/1.0'
);

insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (qname.local_name = 'owner' or qname.local_name = 'userName') and ns.uri = 'http://www.alfresco.org/model/content/1.0'
		then case when isikud.isikukood is not null then isikud.isikukood else concat('ik_', node.row_num) end
	when qname.local_name = 'firstName' and ns.uri = 'http://www.alfresco.org/model/content/1.0'
		then case when isikud.eesnimi is not null then isikud.eesnimi else concat('Eesnimi_', node.row_num) end
	when qname.local_name = 'lastName' and ns.uri = 'http://www.alfresco.org/model/content/1.0'
		then case when isikud.perekonnanimi is not null then isikud.perekonnanimi else concat('Perekonnanimi_', node.row_num) end
	when qname.local_name = 'jobtitle' and ns.uri = 'http://www.alfresco.org/model/content/1.0'
		then case when isikud.amet is not null then isikud.amet else concat('amet_', node.row_num) end
	when (qname.local_name = 'shortcuts' and ns.uri = 'http://alfresco.webmedia.ee/model/menu/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	(select alf_node.*, row_number() over () as row_num from original.alf_node 
		join original.alf_qname type_qname on type_qname.id =alf_node.type_qname_id
		join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
		where type_qname.local_name = 'person' and type_ns.uri = 'http://www.alfresco.org/model/content/1.0') as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud isikud on node.row_num = isikud.row_num
);

-- 6) classificator tüüpi node'idel ei obfuskeerita järgmisi välju: addRemoveValues, alfabeticOrder,
-- deleteEnabled, name. Kõik ülejäänud väljad obfuskeeritakse (eeldatavasti on vaja obfuskeerida ainult üks "description" väli)
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'classificator' and uri = 'http://alfresco.webmedia.ee/model/classificator/1.0'
);

insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when ((qname.local_name = 'addRemoveValues' 
			or qname.local_name = 'alfabeticOrder'
			or qname.local_name = 'deleteEnabled'
			or qname.local_name = 'name') 
		and ns.uri = 'http://alfresco.webmedia.ee/model/classificator/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'classificator' and type_ns.uri = 'http://alfresco.webmedia.ee/model/classificator/1.0'
);

-- 7) classificatorValue tüüpi node'idel, millel readOnly = true, ei obfuskeerita classificatorDescription ja valueName väljade väärtuseid.
-- Ülejäänud tekstilised väärtused obfuskeeritakse. Ülejäänud classificatorValue node'idel obfuskeeritakse kõik tekstilised andmed.


insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'classificatorValue' and uri = 'http://alfresco.webmedia.ee/model/classificator/1.0'
);

insert into public.alf_node_properties (
	select props.node_id, actual_type_n, persisted_type_n, props.boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when ((qname.local_name = 'classificatorDescription' 
			or qname.local_name = 'valueName') 
		and ns.uri = 'http://alfresco.webmedia.ee/model/classificator/1.0'
		and readonly_prop.boolean_value = true)
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join (select node_id, boolean_value from alf_node_properties where qname_id in (select alf_qname.id from alf_qname join alf_namespace ns on ns.id = alf_qname.ns_id where local_name = 'readOnly' and uri = 'http://alfresco.webmedia.ee/model/classificator/1.0')) readonly_prop on node.id = readonly_prop.node_id
	where type_qname.local_name = 'classificatorValue' and type_ns.uri = 'http://alfresco.webmedia.ee/model/classificator/1.0'
);

-- 8) fieldDefinition ja field tüüpi node'idel ei obfuskeerita süsteemselt vajalikke välju, ülejäänud tekstilised väljad obfuskeeritakse 
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name in ('fieldDefinition', 'field') and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (qname.local_name in ('fieldType', 
			'removableFromSystematicDocType',
			'changeableIf',
			'order',
			'isParameterInDocSearch',
			'changeableIfChangeable',
			'inapplicableForDoc',
			'defaultUserLoggedIn',
			'relatedIncomingDecElement',
			'inapplicableForVol',
			'systematicComment',
			'mandatoryForDoc',
			'onlyInGroup',
			'relatedOutgoingDecElement',
			'parameterOrderInDocSearch',
			'originalFieldId',
			'isFixedParameterInVolSearch',
			'classificatorDefaultValue',
			'fieldId',
			'classificator',
			'mandatoryForVol',
			'mandatoryChangeable',
			'docTypes',
			'isFixedParameterInDocSearch',
			'mappingRestriction',
			'systematic',
			'comboboxNotRelatedToClassificator',
			'volTypes',
			'defaultDateSysdate',
			'isParameterInVolSearch',
			'removableFromSystematicFieldGroup',
			'parameterOrderInVolSearch',
			'mandatory',
			'defaultSelected') 
		and ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name in ('fieldDefinition', 'field') and type_ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);

-- 7) fieldGroup tüüpi node'idel, millel systematic = true, ei obfuskeerita name välja väärtust.
-- Ülejäänud tekstilised väärtused obfuskeeritakse. Ülejäänud fieldGroup node'idel obfuskeeritakse kõik tekstilised andmed.
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'fieldGroup' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);

insert into public.alf_node_properties (
	select props.node_id, actual_type_n, persisted_type_n, props.boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when ((qname.local_name = 'name' 
			or qname.local_name = 'valueName') 
		and ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
		and systematic_prop.boolean_value = true)
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join (select node_id, boolean_value from original.alf_node_properties where qname_id in (select alf_qname.id from original.alf_qname join original.alf_namespace ns on ns.id = alf_qname.ns_id where local_name = 'systematic' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')) systematic_prop on node.id = systematic_prop.node_id
	where type_qname.local_name = 'fieldGroup' and type_ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);

-- 7) documentType tüüpi node'idel ei obfuskeerita välja id, systematicComment, case, function, volume, series
-- Node'idel, millel systematic = true, ei obfuskeerita name välja väärtust.
-- Ülejäänud tekstilised väärtused obfuskeeritakse. 
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'documentType' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);

insert into public.alf_node_properties (
	select props.node_id, actual_type_n, persisted_type_n, props.boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when ((qname.local_name in ('id', 'systematicComment' )
		and ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		or
		(qname.local_name in ('case', 'function', 'volume', 'series') 
		and ns.uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0'))
		or (systematic_prop.boolean_value = true and qname.local_name = 'name' and ns.uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join (select node_id, boolean_value from original.alf_node_properties where qname_id in (select alf_qname.id from original.alf_qname join original.alf_namespace ns on ns.id = alf_qname.ns_id where local_name = 'systematic' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')) systematic_prop on node.id = systematic_prop.node_id
	where type_qname.local_name = 'documentType' and type_ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);

-- 8) documentTypeVersion tüüpi node'idel obfuskeeritakse loojaga seotud väljad suvaliste andmetega tabelist tmp_isikud,
-- name välja väärtust ei obfuskeerita
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'documentTypeVersion' and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (qname.local_name = 'name' and ns.uri = 'http://www.alfresco.org/model/content/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	when qname.local_name = 'creatorId' and ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
		then tmp_isikud.isikukood
	when qname.local_name = 'creatorName' and ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
		then concat(tmp_isikud.eesnimi, ' ', tmp_isikud.perekonnanimi)
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	(select alf_node.*, random() as random from original.alf_node ) as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud on ceil(node.random * (select count(*) from tmp_isikud)) = tmp_isikud.row_num
	where type_qname.local_name = 'documentTypeVersion' and type_ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'
);


-- 8) substitute tüüpi node'idel obfuskeeritakse substituteName ja substituteId väljad suvaliste andmetega tabelist tmp_isikud,
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'substitute' and uri = 'http://alfresco.webmedia.ee/model/substitute/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	when qname.local_name = 'substituteId' and ns.uri = 'http://alfresco.webmedia.ee/model/substitute/1.0'
		then tmp_isikud.isikukood
	when qname.local_name = 'substituteName' and ns.uri = 'http://alfresco.webmedia.ee/model/substitute/1.0'
		then concat(tmp_isikud.eesnimi, ' ', tmp_isikud.perekonnanimi)
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	(select alf_node.*, random() as random from original.alf_node ) as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud on ceil(node.random * (select count(*) from tmp_isikud)) = tmp_isikud.row_num
	where type_qname.local_name = 'substitute' and type_ns.uri = 'http://alfresco.webmedia.ee/model/substitute/1.0'
);

-- 8) privPerson tüüpi node'idel obfuskeeritakse kõik tekstilised andmed suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'privPerson' and uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'privPerson' and type_ns.uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'
);

-- 8) organization tüüpi node'idel ei obfuskeerita name välja väärtust (sisaldab node'i uuid väärtust), 
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'organization' and uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(qname.local_name = 'name' 
		and ns.uri = 'http://www.alfresco.org/model/content/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'organization' and type_ns.uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'
);

-- 8) register tüüpi node'idel obfuskeeritakse kõik tekstilised väärtused
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'register' and uri = 'http://alfresco.webmedia.ee/model/register/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'register' and type_ns.uri = 'http://alfresco.webmedia.ee/model/register/1.0'
);

-- 8) function tüüpi node'idel ei obfuskeerita type,status, mark väljade väärtuseid, 
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'function' and uri = 'http://alfresco.webmedia.ee/model/functions/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(qname.local_name in ('type', 'status', 'mark' )
		and ns.uri = 'http://alfresco.webmedia.ee/model/functions/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'function' and type_ns.uri = 'http://alfresco.webmedia.ee/model/functions/1.0'
);

-- 8) series tüüpi node'idel ei obfuskeerita type,docNumberPattern, volType, status, seriesIdentifier, docType väljade väärtuseid, 
-- accessRestricion väli obfuskeeritakse suvaliste väärtustega hulgast 'Avalik', 'AK', 'Majasisene', 'Piiratud'
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'series' and uri = 'http://alfresco.webmedia.ee/model/series/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(qname.local_name in ('type', 'docNumberPattern', 'volType', 'status', 'seriesIdentifier', 'docType'  )
		and ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	when (qname.local_name = 'accessRestriction' and ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0')
		then (ARRAY['Avalik', 'AK', 'Majasisene', 'Piiratud'])[ceil(random()*4)]		
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'series' and type_ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0'
);

-- 8) volume tüüpi node'idel ei obfuskeerita 'volumeMark', 'series', 'function', 'status', 'volumeType' väljade väärtuseid, 
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'volume' and uri = 'http://alfresco.webmedia.ee/model/volume/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(qname.local_name in ('volumeMark', 'series', 'function', 'status', 'volumeType'  )
		and ns.uri = 'http://alfresco.webmedia.ee/model/volume/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'volume' and type_ns.uri = 'http://alfresco.webmedia.ee/model/volume/1.0'
);

-- 8) case tüüpi node'idel ei obfuskeerita 'volume', 'series', 'function', 'status' väljade väärtuseid, 
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'case' and uri = 'http://alfresco.webmedia.ee/model/case/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		((qname.local_name in ('volume', 'series', 'function' )
		and ns.uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')
		or
		(qname.local_name = 'status'
		and ns.uri = 'http://alfresco.webmedia.ee/model/case/1.0'))
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'case' and type_ns.uri = 'http://alfresco.webmedia.ee/model/case/1.0'
);

-- 8) compoundWorkflow ja compundWorkflowDefinition tüüpi node'idel ei obfuskeerita type, status, documentsToSign, caseFileTypes ja documentTypes väljade väärtuseid, 
-- vastutaja, eelmise vastutaja ja looja andmed obfuskeeritakse suvaliste isikute andmetega tabelist tmp_isikud,
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name in ('compoundWorkflow', 'compoundWorkflowDefinition') and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(qname.local_name in ('type', 'status', 'documentsToSign', 'caseFileTypes', 'documentTypes'  )
		and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	when (qname.local_name = 'ownerId' and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then isikud_owner.isikukood		
	when (qname.local_name = 'ownerName' and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then concat(isikud_owner.eesnimi, ' ', isikud_owner.perekonnanimi)
	when (qname.local_name = 'ownerJobTitle' and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then isikud_owner.amet
	when (qname.local_name = 'userId' and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then isikud_creator.isikukood
	when (qname.local_name = 'creatorName' and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then concat(isikud_creator.eesnimi, ' ', isikud_creator.perekonnanimi)
	when (qname.local_name = 'previousOwnerId' and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then isikud_previous_owner.isikukood			
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	(select alf_node.*, random() as random1, random() as random2, random() as random3 from original.alf_node ) as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud isikud_owner on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_owner.row_num
	left join tmp_isikud isikud_creator on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud isikud_previous_owner on ceil(node.random3 * (select count(*) from tmp_isikud)) = isikud_previous_owner.row_num
	where type_qname.local_name in ('compoundWorkflow', 'compoundWorkflowDefinition') and type_ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0'
);

-- 8) 'confirmationWorkflow', 'informationWorkflow', 'assignmentWorkflow', 'signatureWorkflow', 'reviewWorkflow', 'opinionWorkflow',
-- 'docRegistrationWorkflow', 'externalReviewWorkflow', 'orderAssignmentWorkflow', 'dueDateExtensionWorkflow' tüüpi node'idel ei status välja väärtust, 
-- looja andmed obfuskeeritakse suvalise isiku andmetega tabelist tmp_isikud,
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name in ('confirmationWorkflow', 'informationWorkflow', 'assignmentWorkflow', 'signatureWorkflow', 'reviewWorkflow', 'opinionWorkflow',
	'docRegistrationWorkflow', 'externalReviewWorkflow', 'orderAssignmentWorkflow', 'dueDateExtensionWorkflow') and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(qname.local_name in ('status' )
		and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	when (qname.local_name = 'creatorName' and ns.uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		then concat(isikud_creator.eesnimi, ' ', isikud_creator.perekonnanimi)
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	(select alf_node.*, random() as random from original.alf_node ) as node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	left join tmp_isikud isikud_creator on ceil(node.random * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	where type_qname.local_name in ('confirmationWorkflow', 'informationWorkflow', 'assignmentWorkflow', 'signatureWorkflow', 'reviewWorkflow', 'opinionWorkflow',
	'docRegistrationWorkflow', 'externalReviewWorkflow', 'orderAssignmentWorkflow', 'dueDateExtensionWorkflow') and type_ns.uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0'
);

-- 8) contactGroup tüüpi node'idel obfuskeeritakse kõik tekstilised väärtused
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'contactGroup' and uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'contactGroup' and type_ns.uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'
);

-- 8) filter tüüpi node'idel (dok. aruannete, otsingute, tööülesannete otsingute salvestatud filtrid) obfuskeeritakse kõik tekstilised väärtused
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'filter' and uri in ('http://alfresco.webmedia.ee/model/document/report/1.0', 'http://alfresco.webmedia.ee/model/document/search/1.0',
	'http://alfresco.webmedia.ee/model/task/search/1.0')
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'filter' and type_ns.uri in ('http://alfresco.webmedia.ee/model/document/report/1.0', 'http://alfresco.webmedia.ee/model/document/search/1.0',
	'http://alfresco.webmedia.ee/model/task/search/1.0')
);

-- 8) sendInfo tüüpi node'idel ei obfuskeerita 'sendStatus', 'sendMode' väljade väärtuseid, 
-- ülejäänud tekstilised andmed obfuskeeritakse suvaliste väärtustega  
insert into public.alf_node (
	select node.id, node.version, node.store_id, node.uuid, node.transaction_id, node.node_deleted, node.type_qname_id, node.acl_id,
	(case when node.audit_creator = 'System' then node.audit_creator else isikud_creator.isikukood end),
	node.audit_created,
	(case when node.audit_modifier = 'System' then node.audit_modifier else isikud_modifier.isikukood end),
	node.audit_modified, node.audit_accessed from (
		select *, 
		(case when audit_creator is not null then random() else null end) as random1,
		(case when audit_modifier is not null then random() else null end) as random2 from original.alf_node ) as node
	join original.alf_qname qname on qname.id = node.type_qname_id
	join original.alf_namespace ns on ns.id = qname.ns_id
	left join tmp_isikud as isikud_creator on ceil(node.random1 * (select count(*) from tmp_isikud)) = isikud_creator.row_num
	left join tmp_isikud as isikud_modifier on ceil(node.random2 * (select count(*) from tmp_isikud)) = isikud_modifier.row_num
	where local_name = 'sendInfo' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0'
);


insert into public.alf_node_properties (
	select node_id, actual_type_n, persisted_type_n, boolean_value, 
	case when serializable_value is not null and persisted_type_n = 9
		then octet_length(serializable_value)
	else long_value end, 
	float_value, double_value, 
	case when 
		(qname.local_name in ('sendStatus', 'sendMode' )
		and ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (actual_type_n = 7 and persisted_type_n = 6)
		then string_value
	else rpad('obfuskeeritud', length(string_value), ' obfusk') end,
	case when serializable_value is null 
		then null
	when actual_type_n = 9 and persisted_type_n = 9
		then '\254\355\000\005sr\000\023java.util.ArrayListx\201\322\035\231\307a\235\003\000\001I\000\004sizexp\000\000\000\000w\004\000\000\000\000x'::bytea
		-- TODO: this should be function of original value's length
	else '\254\355\000\005t\0041Eesti Linnade Liit, Eesti Maaomavalitsuste Liit, Maakondlikud omavalitsusliidud, Presidendi Kantselei , Riigikogu juhatus , Riigikontroll, \303\225iguskantsleri Kantselei , Arengufond, Eesti Koost\303\266\303\266 Kogu , Eesti K\303\274laliikumine Kodukant , Eesti Mittetulundus\303\274hingute ja Sihtasutuste Liit , Eesti Tuleviku-uuringute instituut , Eesti T\303\266\303\266andjate Keskliit , Eesti V\303\244ike- ja Keskmiste Ettev\303\265tjate Assotsiatsioon , SA Poliitikauuringute Keskus Praxis , MT\303\234 Polis , Tallinna Tehnika\303\274likooli Sotsiaalteaduskond , Tallinna \303\234likooli Riigiteaduste Instituut , Tartu \303\234likool Loodus- ja tehnoloogiateaduskonna Inimgeograafia ja regionaalplaneerimise \303\265ppetool , Tartu \303\234likooli \303\225igusteaduskond , Isamaa ja Res Publica Liit , Eesti Reformierakond, Keskerakond , Sotsiaaldemokraatlik erakond , Erakond Eestimaa Rohelised , Erakond Eesti Kristlikud Demokraadid , Eesti Konservatiivne Rahvaerakond , Eesti Arhitektide liit , Eesti Lastevanemate liit , Eesti Liikumispuudega Inimeste Liit , Eesti Omanike Keskliit , Eesti Patsientide Esindus\303\274hing , Eesti Pension\303\244ride \303\234henduste Liit'::bytea
	end,
	qname_id, list_index, locale_id
	from 
	original.alf_node node
	join original.alf_node_properties props on props.node_id = node.id
	join original.alf_qname type_qname on type_qname.id = node.type_qname_id
	join original.alf_namespace type_ns on type_ns.id = type_qname.ns_id
	join original.alf_qname qname on qname.id = props.qname_id
	join original.alf_namespace ns on qname.ns_id = ns.id
	where type_qname.local_name = 'sendInfo' and type_ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0'
);

-- alf_node_aspects kõik kirjed, millel eksisteerib node väärtus obfuskeeritud alf_node tabelis
insert into public.alf_node_aspects (
	select aspects.* from original.alf_node_aspects aspects
	join public.alf_node node on node.id = aspects.node_id
);

-- alf_node_assoc kõik kirjed, millel eksisteerib target_node_id ja source_node_id väärtus obfuskeeritud alf_node tabelis
insert into public.alf_node_assoc (
	select assoc.* from original.alf_node_assoc assoc
	join public.alf_node source_node on source_node.id = assoc.source_node_id
	join public.alf_node target_node on target_node.id = assoc.target_node_id
);

-- alf_child_assoc content node'ide seosed, kus on vaja täiendavalt obfuskeerida failinimed
insert into public.alf_child_assoc (
	select assoc.id, assoc.version, parent_node_id, assoc.type_qname_id, crc32(file_name), file_name, child_node_id,qname_ns_id, file_name, is_primary, assoc_index 
	from original.alf_child_assoc assoc
	join public.alf_node parent_node on parent_node.id = assoc.parent_node_id
	join public.alf_node child_node on child_node.id = assoc.child_node_id
	join (select id, concat('fail_', row_number() over (), '.bin' ) as file_name from tmp_content_node_ids where nimi_obfuskeerida = true) content_nodes on content_nodes.id = assoc.child_node_id
	
);

-- alf_child_assoc isikutega seotud node'ide seosed, kus on vaja täiendavalt obfuskeerida isikukoodid
insert into public.alf_child_assoc (
	select distinct assoc.id, assoc.version, parent_node_id, assoc.type_qname_id, crc32(string_value), string_value, child_node_id,qname_ns_id, string_value, is_primary, assoc_index 
	from original.alf_child_assoc assoc
	join public.alf_node parent_node on parent_node.id = assoc.parent_node_id
	join public.alf_node child_node on child_node.id = assoc.child_node_id
	join (select props.node_id as props_node_id, string_value from tmp_userhome_child_node_ids
		join public.alf_node_properties props on tmp_userhome_child_node_ids.id = props.node_id
		join alf_qname qname on qname.id = props.qname_id
		join alf_namespace ns on ns.id = qname.ns_id
		where local_name = 'name' and uri = 'http://www.alfresco.org/model/content/1.0') user_home_nodes on user_home_nodes.props_node_id = assoc.child_node_id
);

insert into public.alf_child_assoc (
	select distinct assoc.id, assoc.version, parent_node_id, assoc.type_qname_id, crc32(string_value), string_value, child_node.child_node_id,qname_ns_id, string_value, is_primary, assoc_index 
	from original.alf_child_assoc assoc
	join public.alf_node parent_node on parent_node.id = assoc.parent_node_id
	join (select node.id as child_node_id, * from public.alf_node node 
		join alf_qname qname on qname.id = node.type_qname_id
		join alf_namespace ns on ns.id = qname.ns_id
		where local_name = 'person' and uri = 'http://www.alfresco.org/model/content/1.0') as child_node on child_node.child_node_id = assoc.child_node_id
	join (select props.node_id as props_node_id, string_value from public.alf_node_properties props 
		join alf_qname qname on qname.id = props.qname_id
		join alf_namespace ns on ns.id = qname.ns_id
		where local_name = 'userName' and uri = 'http://www.alfresco.org/model/content/1.0') user_home_nodes on user_home_nodes.props_node_id = child_node.child_node_id
	where not exists (select id from public.alf_child_assoc public_assoc where public_assoc.id = assoc.id)
--and parent_node_id = 3145
	order by parent_node_id
);

-- alf_child_assoc ülejäänud seosed, millel on obfuskeeritud andmete hulgas olemas nii 
insert into public.alf_child_assoc (
	select assoc.* 
	from original.alf_child_assoc assoc
	join public.alf_node parent_node on parent_node.id = assoc.parent_node_id
	join public.alf_node child_node on child_node.id = assoc.child_node_id
	where not exists (select id from public.alf_child_assoc public_assoc where public_assoc.id = assoc.id)
);

-- süsteemsed tabelid muutmata kujul
insert into public.alf_applied_patch (select * from original.alf_applied_patch);


-- süsteemsed avm_xxx tabelid
insert into public.avm_aspects (select * from original.avm_aspects);

alter table avm_stores drop CONSTRAINT fk_avm_s_root;
insert into public.avm_stores (select * from original.avm_stores);
insert into public.avm_store_properties (select * from original.avm_store_properties);
insert into public.avm_nodes (select * from original.avm_nodes);
alter table avm_stores add CONSTRAINT fk_avm_s_root FOREIGN KEY (current_root_id)
      REFERENCES avm_nodes (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;
      
insert into public.avm_history_links (select * from original.avm_history_links);
insert into public.avm_merge_links (select * from original.avm_merge_links);
insert into public.avm_child_entries (select * from original.avm_child_entries);
insert into public.avm_node_properties (select * from original.avm_node_properties);
insert into public.avm_version_layered_node_entry (select * from original.avm_version_layered_node_entry);
insert into public.avm_version_roots (select * from original.avm_version_roots);

SELECT setval('hibernate_sequence', greatest((select max(id) + 1 from public.alf_node), (select max(id) + 1 from public.alf_transaction)));