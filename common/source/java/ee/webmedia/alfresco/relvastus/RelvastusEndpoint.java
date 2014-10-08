package ee.webmedia.alfresco.relvastus;

import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.springframework.util.Assert;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.relvastus.generated.Dokument;
import ee.webmedia.alfresco.relvastus.generated.OtsiDokumendidParing;
import ee.webmedia.alfresco.relvastus.generated.OtsiDokumendidVastus;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.XmlUtil;

@Endpoint
public class RelvastusEndpoint {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(RelvastusEndpoint.class);

    // TODO instruct JAXB compiler to generate Date fields instead of XMLGregorianCalendar - uncomment binding attribute in build-common.xml

    // TODO real implementation should reside in RelvastusService, because then it is performed inside one transaction;
    // here should only be setting the authentication, like in AvalikDokumendiRegisterEndpoint

    private GeneralService generalService;
    private DocumentTemplateService documentTemplateService;

    @PayloadRoot(localPart = "otsiDokumendidParing", namespace = "http://delta/relvastus/schemas")
    public OtsiDokumendidVastus otsiDokumendid(OtsiDokumendidParing request) {
        Assert.hasText(request.getDokumendiViit());

        OtsiDokumendidVastus result = new OtsiDokumendidVastus();

        Dokument doc1 = new Dokument();
        doc1.setDokumendiLiik("Sissetulev kiri");
        doc1.setPealkiri("Kõige võimsam <b>dokument</b>!!!");
        doc1.setViit("12.3-45/678-Ü");
        doc1.setRegistreerimiseKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() - 100000)));
        NodeRef doc1ref = new NodeRef(generalService.getArchivalsStoreRef(), GUID.generate());
        doc1.setUrl(documentTemplateService.getDocumentUrl(doc1ref));
        result.getDokument().add(doc1);

        Dokument doc2 = new Dokument();
        doc2.setDokumendiLiik("Väljaminev kiri");
        doc2.setPealkiri("Väga hea test dokument");
        doc2.setViit("1/2");
        doc2.setRegistreerimiseKuupaev(XmlUtil.getXmlGregorianCalendar(new Date(System.currentTimeMillis() + 100000)));
        NodeRef doc2ref = new NodeRef(generalService.getStore(), GUID.generate());
        doc2.setUrl(documentTemplateService.getDocumentUrl(doc2ref));
        result.getDokument().add(doc2);

        return result;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

}
