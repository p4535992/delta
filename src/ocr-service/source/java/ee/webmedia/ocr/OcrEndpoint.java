package ee.webmedia.ocr;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@WebService(name = "Ocr", targetNamespace = "http://webmedia.ee/ocr", serviceName = "OcrService")
public class OcrEndpoint implements Ocr {
    private static Logger log = Logger.getLogger(OcrServiceImpl.class);

    private OcrService ocrService;

    @Override
    @WebMethod
    @WebResult(name = "ocrOutput", targetNamespace = "")
    @RequestWrapper(localName = "convertToPdf", targetNamespace = "", className = "ee.webmedia.ocr.ConvertToPdf")
    @ResponseWrapper(localName = "convertToPdfResponse", targetNamespace = "", className = "ee.webmedia.ocr.ConvertToPdfResponse")
    public OcrOutput convertToPdf(
            @WebParam(name = "ocrInput", targetNamespace = "") OcrInput ocrInput) {

        try {
            return ocrService.convertToPdf(ocrInput);
        } catch (Exception e) {
            log.debug("Error while processing request", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    @WebMethod(exclude = true)
    public void setOcrService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ee/webmedia/ocr/context.xml");
//        OcrServiceImpl bean = (OcrServiceImpl) applicationContext.getBean(OcrService.class);
//        bean.waitForOcrComplete("scan1-001");
//        bean.readLog();
    }

}
