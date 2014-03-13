package ee.webmedia.alfresco.adr.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.adr.ws.Dokumendiliik;
import ee.webmedia.alfresco.adr.ws.Dokument;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidega;
import ee.webmedia.alfresco.adr.ws.DokumentDetailidegaV2;
import ee.webmedia.alfresco.adr.ws.DokumentId;
import ee.webmedia.alfresco.adr.ws.Fail;
import ee.webmedia.alfresco.adr.ws.FailV2;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

public interface AdrService {

    String BEAN_NAME = "AdrService";

    // ---------- Reaalajas osa -----------

    // tagastab nimekirja dokumentidest koos lühiinfoga
    // vastuste arvu piirangut ei ole
    // sisendparameetrites olev periood jääb reg.kuup järgi
    // kuupäevade vahemikus on nii algus- kui lõppkuupäeva kaasaarvatud (ehk aaaa-kk-ppT00:00:00 kuni aaaa-kk-ppT23:59:59)
    // kõik parameetrid on mittekohustuslikud ehk võib ära jätta
    // implementatsioonis on ainult üks erikäitumine tehtud: kui ühtegi parameetrit antud pole, siis tagastatakse mitte midagi (mitte KÕIK dokumendid)
    // aga sellest hoolimata on võimalik väga lihtsalt saada KÕIK või väga suure hulga dokumente vastusena (sest vastuste arvu piirangut ei ole),
    // näiteks kui panna väga lai kuupäeva vahemik
    // seepärast peab kasutajale piiranguid seadma väljakutsuv süsteem!
    List<Dokument> otsiDokumendid(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, String dokumendiLiik, String otsingusona);

    // tagastab ühe dokumendi detailinfo + nimekirja seotud dokumentidest lühiinfoga + nimekirja failidest (ilma faili sisudeta)
    // kui JPP != Avalik, siis nimekirja failidest ei tagastata
    // kõik parameetrid on kohustuslikud, otsitakse täpset vastet!
    DokumentDetailidega dokumentDetailidega(String viit, XMLGregorianCalendar registreerimiseAeg);

    // tagastab ühe faili info ja faili sisu
    // kõik parameetrid on kohustuslikud, otsitakse täpset vastet!
    // kui JPP != Avalik, siis tagastatakse tühi vastus (mitte faili info ilma sisuta)
    Fail failSisuga(String viit, XMLGregorianCalendar registreerimiseAeg, String filename);

    // sama mis eelmine
    // aga dokumendi identifitseerimine käib nodeRef järgi
    // + lisaväljad
    FailV2 failSisugaV2(NodeRef nodeRef, String filename);

    List<Dokumendiliik> dokumendiliigid();

    // ---------- Perioodilise sünkimise osa -----------

    // tagastab nimekirja dokumentidest koos detailinfoga + nimekirja seotud dokumentidest (ainult viitadega) + nimekirja failidest (koos faili sisudega)
    // kui JPP != Avalik, siis nimekirja failidest ei tagastata
    // vastuste arvu piirangut ei ole
    // sisendparameetrites olev periood tähendab viimase muutmise aega
    // kuupäevade vahemikus on nii algus- kui lõppkuupäeva kaasaarvatud (ehk aaaa-kk-ppT00:00:00 kuni aaaa-kk-ppT23:59:59)
    // kõik parameetrid on kohustuslikud
    List<DokumentDetailidega> koikDokumendidLisatudMuudetud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev);

    // sama mis eelmine
    // aga dokumendi identifitseerimine käib nodeRef järgi
    // + lisaväljad
    // ja failid on ilma sisuta
    List<DokumentDetailidegaV2> koikDokumendidLisatudMuudetudV2(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, int jataAlgusestVahele,
            int tulemustePiirang);

    // XXX kui dokumenti on muudetud pärast perioodiLoppKuupaev'a, siis ta ei ole näha siin vastuses, olenemata sellest et registreeriti ta näiteks selles vahemikus
    // Aga ei tohiks olla oluline, sest ADR võtab samal öösel eelmise kuupäeva kohta, ehk vahemik on ainult paar tundi ja keegi siis enam ei muuda

    // tagastab nimekirja dokumentidest (ainult viidad)
    // vastuste arvu piirangut ei ole
    // sisendparameetrites olev periood tähendab kustutamise aega
    // kuupäevade vahemikus on nii algus- kui lõppkuupäeva kaasaarvatud (ehk aaaa-kk-ppT00:00:00 kuni aaaa-kk-ppT23:59:59)
    // kõik parameetrid on kohustuslikud
    List<Dokument> koikDokumendidKustutatud(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev);

    // sama mis eelmine
    // aga dokumendi identifitseerimine käib nodeRef järgi
    List<DokumentId> koikDokumendidKustutatudV2(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, int jataAlgusestVahele, int tulemustePiirang);

    // ---------- Internal use -----------

    // NB! ükskõik mis tegevus muudab dokumendile viitamise parameetreid (viit või registreerimiseAeg), peab lisama dokumendi kustutatud nimekirja
    // või muudab teisi parameetreid, mille alusel dokument avalikustamise hulka arvatakse:
    // * docStatus = lõpetatud -- ainus koht mis leidsin mis muudab docStatuse 'lõpetatud' pealt millekski muuks, on dokumendi taasavamine
    // * accessRestriction != Majasisene -- ainus koht mis leidsin mis muudab selle Avalik/AK pealt Majasiseseks, on kasutajaliideses muutmine ja salvestamine

    // kohad
    // * dokumendi kustutamine - ei saagi kustutada registreeritud dokumenti; aga igaks juhuks lisatud kood
    // * registreerimine, rakendub ka kohtadest kus seda välja kutsutakse, näiteks ümberliigitamine - kui reg.nr või reg.aeg muutub, siis märgitakse vana viit+aeg kustutatuks
    // * (arhiveerimine - ei tehta midagi, jääb ADR'i alles)
    // * hävitamine - kustutatakse ADR'ist

    NodeRef addDeletedDocument(NodeRef document);

    NodeRef addDeletedDocumentFromArchive(NodeRef document, String regNumber, Date regDateTime);

    void deleteDocumentType(QName documentType);

    void addDocumentType(QName documentType);

    DokumentDetailidegaV2 buildDokumentDetailidegaV2(DocumentDynamic doc, boolean includeFileContent, Set<String> documentTypeIds,
            Map<NodeRef, Map<QName, Serializable>> functionsCache, Map<NodeRef, Map<QName, Serializable>> seriesCache, Map<NodeRef, Map<QName, Serializable>> volumesCache,
            boolean includeAssocsAndDocTypeName);

}
