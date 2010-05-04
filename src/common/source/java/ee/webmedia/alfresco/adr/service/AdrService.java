package ee.webmedia.alfresco.adr.service;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.adr.Dokumendiliik;
import ee.webmedia.alfresco.adr.Dokument;
import ee.webmedia.alfresco.adr.DokumentDetailidega;
import ee.webmedia.alfresco.adr.Fail;

public interface AdrService {

    String BEAN_NAME = "AdrService";

    // ---------- Reaalajas osa -----------

    // tagastab nimekirja dokumentidest koos lühiinfoga
    // vastuste arvu piirangut ei ole
    // sisendparameetrites olev periood jääb reg.kuup järgi
    // kuupäevade vahemikus on nii algus- kui lõppkuupäeva kaasaarvatud (ehk aaaa-kk-ppT00:00:00 kuni aaaa-kk-ppT23:59:59)
    // kõik parameetrid on mittekohustuslikud ehk võib ära jätta
    //   implementatsioonis on ainult üks erikäitumine tehtud: kui ühtegi parameetrit antud pole, siis tagastatakse mitte midagi (mitte KÕIK dokumendid)
    // aga sellest hoolimata on võimalik väga lihtsalt saada KÕIK või väga suure hulga dokumente vastusena (sest vastuste arvu piirangut ei ole),
    //   näiteks kui panna väga lai kuupäeva vahemik
    // seepärast peab kasutajale piiranguid seadma väljakutsuv süsteem!
    List<Dokument> otsiDokumendid(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, String dokumendiLiik, String otsingusona);

    // tagastab ühe dokumendi detailinfo + nimekirja seotud dokumentidest lühiinfoga + nimekirja failidest (ilma faili sisudeta)
    // kõik parameetrid on kohustuslikud, otsitakse täpset vastet!
    DokumentDetailidega dokumentDetailidega(String viit, XMLGregorianCalendar registreerimiseAeg);

    // tagastab ühe faili info ja faili sisu
    // kõik parameetrid on kohustuslikud, otsitakse täpset vastet!
    Fail failSisuga(String viit, XMLGregorianCalendar registreerimiseAeg, String filename);

    // XXX TODO FIXME NB! kas kasutajaliideses dok.liigi mittenähtavaks muutmine peab mõjutama ka teisi päringuid?!?!
    List<Dokumendiliik> dokumendiliigid();

    // ---------- Perioodilise sünkimise osa -----------

    // tagastab nimekirja dokumentidest koos detailinfoga + nimekirja seotud dokumentidest (ainult viitadega) + nimekirja failidest (koos faili sisudega)
    // vastuste arvu piirangut ei ole
    // sisendparameetrites olev periood tähendab viimase muutmise aega
    // kuupäevade vahemikus on nii algus- kui lõppkuupäeva kaasaarvatud (ehk aaaa-kk-ppT00:00:00 kuni aaaa-kk-ppT23:59:59)
    // kõik parameetrid on kohustuslikud
    List<DokumentDetailidega> koikDokumendidLisatudMuudetud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev);

    // XXX kui dokumenti on muudetud pärast perioodiLoppKuupaev'a, siis ta ei ole näha siin vastuses, olenemata sellest et registreeriti ta näiteks selles vahemikus
    // Aga ei tohiks olla oluline, sest ADR võtab samal öösel eelmise kuupäeva kohta, ehk vahemik on ainult paar tundi ja keegi siis enam ei muuda


    // tagastab nimekirja dokumentidest (ainult viidad)
    // vastuste arvu piirangut ei ole
    // sisendparameetrites olev periood tähendab kustutamise aega
    // kuupäevade vahemikus on nii algus- kui lõppkuupäeva kaasaarvatud (ehk aaaa-kk-ppT00:00:00 kuni aaaa-kk-ppT23:59:59)
    // kõik parameetrid on kohustuslikud
    List<Dokument> koikDokumendidKustutatud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev);


    // ---------- Internal use -----------

    // NB! ükskõik mis tegevus muudab dokumendile viitamise parameetreid (viit või registreerimiseAeg), peab lisama dokumendi kustutatud nimekirja
    // või muudab teisi parameetreid, mille alusel dokument avalikustamise hulka arvatakse:
    // * docStatus = lõpetatud -- ainus koht mis leidsin mis muudab docStatuse 'lõpetatud' pealt millekski muuks, on dokumendi taasavamine
    // * accessRestriction != Majasisene -- ainus koht mis leidsin mis muudab selle Avalik/AK pealt Majasiseseks, on kasutajaliideses muutmine ja salvestamine

    // kohad
    // * dokumendi kustutamine - ei saagi kustutada registreeritud dokumenti; aga igaks juhuks lisatud kood
    // * arhiveerimine - liigutatakse aktiivsest dok.loetelust ära; kas siis märkida ADR jaoks kustutatuks??? real-time osas kaob ära; praegu siin ei teinud TODO
    // * registreerimine, rakendub ka kohtadest kus seda välja kutsutakse, näiteks ümberliigitamine - kui reg.nr või reg.aeg muutub, siis märgitakse vana viit+aeg kustutatuks

    void addDeletedDocument(NodeRef document);

}
