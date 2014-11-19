package ee.webmedia.alfresco.ikt;

import java.math.BigDecimal;
import java.util.Date;

import javax.activation.DataHandler;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.ikt.generated.Dokument;
import ee.webmedia.alfresco.ikt.generated.DokumentDetailidega;
import ee.webmedia.alfresco.ikt.generated.Fail;
import ee.webmedia.alfresco.ikt.generated.FailSisuga;
import ee.webmedia.alfresco.ikt.generated.FailSisugaParing;
import ee.webmedia.alfresco.ikt.generated.FailSisugaVastus;
import ee.webmedia.alfresco.ikt.generated.LepinguPool;
import ee.webmedia.alfresco.ikt.generated.OtsiDokumendidParing;
import ee.webmedia.alfresco.ikt.generated.OtsiDokumendidVastus;
import ee.webmedia.alfresco.utils.MimeUtil;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.xtee.jaxb.ByteArrayDataSource;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
@Endpoint
public class IktEndpoint {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(IktEndpoint.class);

    // TODO instruct JAXB compiler to generate Date fields instead of XMLGregorianCalendar - uncomment binding attribute in build-common.xml

    // TODO real implementation should reside in RelvastusService, because then it is performed inside one transaction;
    // here should only be setting the authentication, like in AvalikDokumendiRegisterEndpoint

    private GeneralService generalService;

    @PayloadRoot(localPart = "otsiDokumendidParing", namespace = "http://delta/ikt/schemas")
    public OtsiDokumendidVastus otsiDokumendid(OtsiDokumendidParing request) {
        Assert.hasText(request.getDokumendiViit());
        Assert.hasText(request.getKasutajaIsikukood());

        OtsiDokumendidVastus result = new OtsiDokumendidVastus();

        if (StringUtils.isNotBlank(request.getDokumendiId())) {
            DokumentDetailidega doc = new DokumentDetailidega();
            doc.setDokumendiLiik("Väljaminev kiri");
            doc.setPealkiri("Väga hea test dokument");
            doc.setViit("1/2");
            doc.setRegistreerimiseKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() + 100000)));
            doc.setId(new NodeRef(generalService.getArchivalsStoreRef(), GUID.generate()).toString());
            doc.setJuurdepaasuPiirang("AK");
            if (Math.random() > 0.5d) {
                doc.setJuurdepaasuPiiranguAlus("AvTS § 35 lg 2");
                doc.setJuurdepaasuPiiranguKehtivuseAlgusKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() - 900000)));
                doc.setJuurdepaasuPiiranguKehtivuseLoppKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() + 900000)));
                doc.setJuurdepaasuPiiranguKehtivuseLoppKirjeldus("Kuni nõuniku tagasiastumiseni");
                doc.setEsimeseLepinguPooleNimi("Minu enda asutus");
                doc.setEsimeseLepinguPooleKontaktisik("Minu enda asutuse kontaktisik");

                LepinguPool party1 = new LepinguPool();
                party1.setNimi("Hiiu Kuningriik");
                party1.setKontaktisik("Jaan Tamm");
                party1.setEpost("jaan.tamm@hiiukuningriik.ee");
                doc.getLepinguPool().add(party1);

                LepinguPool party2 = new LepinguPool();
                party2.setNimi("Viru Linnriik");
                party2.setKontaktisik("Maie");
                party2.setEpost("maie@maie.ee");
                doc.getLepinguPool().add(party2);

                doc.setLepinguRahastusallikas("Tõe ministeerium");
                doc.setLepinguLoppsumma(BigDecimal.valueOf(123.45d));
                doc.setLepinguLoppKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() + 2900000)));
                doc.setLepinguLoppKirjeldus("kirjeldus...");
                doc.setLepinguLoppaktiRegistreerimiseKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() + 1900000)));

                Fail file1 = new Fail();
                file1.setNimi("Tõde ja õigus (1).docx");
                file1.setPealkiri("Tõde ja õigus");
                file1.setMimeTuup("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                file1.setKodeering("UTF-8");
                file1.setSuurus(12345678);
                file1.setMuutmiseAeg(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() - 90000)));
                doc.getFail().add(file1);

                Fail file2 = new Fail();
                file2.setNimi("kirjutan siia äärmiselt pika f....txt");
                file2.setPealkiri("kirjutan siia äärmiselt pika failinime");
                file2.setMimeTuup(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                file2.setKodeering("iso-8859-1");
                file2.setSuurus(98765432);
                file2.setMuutmiseAeg(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() - 6690000)));
                doc.getFail().add(file2);

                Fail file3 = new Fail();
                file3.setNimi("lehekülg.html");
                file3.setPealkiri("lehekülg");
                file3.setMimeTuup(MimetypeMap.MIMETYPE_HTML);
                file3.setKodeering("windows-1257");
                file3.setSuurus(54321);
                file3.setMuutmiseAeg(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() - 9990000)));
                doc.getFail().add(file3);
            }
            result.setDokumentDetailidega(doc);

        } else {
            Dokument doc1 = new Dokument();
            doc1.setDokumendiLiik("Sissetulev kiri");
            doc1.setPealkiri("Kõige võimsam <b>dokument</b>!!!");
            doc1.setViit("12.3-45/678-Ü");
            doc1.setRegistreerimiseKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() - 100000)));
            doc1.setId(new NodeRef(generalService.getStore(), GUID.generate()).toString());
            result.getDokument().add(doc1);

            Dokument doc2 = new Dokument();
            doc2.setDokumendiLiik("Väljaminev kiri");
            doc2.setPealkiri("Väga hea test dokument");
            doc2.setViit("1/2");
            doc2.setRegistreerimiseKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() + 100000)));
            doc2.setId(new NodeRef(generalService.getArchivalsStoreRef(), GUID.generate()).toString());
            result.getDokument().add(doc2);
        }

        return result;
    }

    @PayloadRoot(localPart = "failSisugaParing", namespace = "http://delta/ikt/schemas")
    public FailSisugaVastus failSisuga(FailSisugaParing request) {
        Assert.hasText(request.getDokumendiId());
        Assert.hasText(request.getKasutajaIsikukood());
        Assert.hasText(request.getFailiNimi());
        FailSisugaVastus result = new FailSisugaVastus();
        FailSisuga file = new FailSisuga();
        file.setNimi("Vihm ja tuul (1).txt");
        file.setPealkiri("Vihm ja tuul");
        file.setMimeTuup(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        file.setKodeering("UTF-8");
        file.setSuurus(4);
        file.setMuutmiseAeg(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() - 90000)));
        file.setSisu(new DataHandler(new ByteArrayDataSource(MimeUtil.getContentType(MimetypeMap.MIMETYPE_TEXT_PLAIN, "UTF-8"), "ABCD".getBytes())));
        // real implementation should use ContentReaderDataSource
        result.setFailSisuga(file);
        return result;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
