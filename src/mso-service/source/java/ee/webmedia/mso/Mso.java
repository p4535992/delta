package ee.webmedia.mso;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "Mso", targetNamespace = "http://webmedia.ee/mso")
public interface Mso {

    @WebMethod
    @WebResult(name = "msoOutput", targetNamespace = "http://webmedia.ee/mso")
    @RequestWrapper(localName = "convertToPdf", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ConvertToPdf")
    @ResponseWrapper(localName = "convertToPdfResponse", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ConvertToPdfResponse")
    MsoOutput convertToPdf(
            @WebParam(name = "msoInput", targetNamespace = "http://webmedia.ee/mso") MsoInput msoInput);

}
