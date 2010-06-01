package ee.webmedia.ocr;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "Ocr", targetNamespace = "http://webmedia.ee/ocr")
public interface Ocr {

    @WebMethod
    @WebResult(name = "ocrOutput", targetNamespace = "http://webmedia.ee/ocr")
    @RequestWrapper(localName = "convertToPdf", targetNamespace = "http://webmedia.ee/ocr", className = "ee.webmedia.ocr.ConvertToPdf")
    @ResponseWrapper(localName = "convertToPdfResponse", targetNamespace = "http://webmedia.ee/ocr", className = "ee.webmedia.ocr.ConvertToPdfResponse")
    OcrOutput convertToPdf(
            @WebParam(name = "ocrInput", targetNamespace = "http://webmedia.ee/ocr") OcrInput ocrInput);

}
