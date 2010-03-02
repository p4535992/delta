package ee.webmedia.alfresco.adr.service;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import ee.webmedia.alfresco.adr.Dokument;
import ee.webmedia.alfresco.adr.DokumentDetailidega;
import ee.webmedia.alfresco.adr.FailSisuga;

public interface AdrService {
    
    String BEAN_NAME = "AdrService";
    
    List<Dokument> otsiDokumendid(XMLGregorianCalendar perioodiAlgusKuupaev, XMLGregorianCalendar perioodiLoppKuupaev, String dokumendiLiik, String otsingusona);

    List<Dokument> otsiDokumendidSamasTeemas(String viit, XMLGregorianCalendar perioodiAlgusKuupaev);

    DokumentDetailidega dokumentDetailidega(String viit, XMLGregorianCalendar registreerimiseAeg);

    FailSisuga failSisuga(String viit, XMLGregorianCalendar registreerimiseAeg, String filename);
}
