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
    @WebResult(name = "msoPdfOutput", targetNamespace = "http://webmedia.ee/mso")
    @RequestWrapper(localName = "convertToPdf", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ConvertToPdf")
    @ResponseWrapper(localName = "convertToPdfResponse", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ConvertToPdfResponse")
    MsoPdfOutput convertToPdf(
            @WebParam(name = "msoDocumentInput", targetNamespace = "http://webmedia.ee/mso") MsoDocumentInput msoDocumentInput);

    @WebMethod
    @WebResult(name = "msoDocumentOutput", targetNamespace = "http://webmedia.ee/mso")
    @RequestWrapper(localName = "replaceFormulas", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ReplaceFormulas")
    @ResponseWrapper(localName = "replaceFormulasResponse", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ReplaceFormulasResponse")
    MsoDocumentOutput replaceFormulas(
            @WebParam(name = "msoDocumentAndFormulasInput", targetNamespace = "http://webmedia.ee/mso") MsoDocumentAndFormulasInput msoDocumentAndFormulasInput);

    @WebMethod
    @WebResult(name = "msoDocumentAndPdfOutput", targetNamespace = "http://webmedia.ee/mso")
    @RequestWrapper(localName = "replaceFormulasAndConvertToPdf", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ReplaceFormulasAndConvertToPdf")
    @ResponseWrapper(localName = "replaceFormulasAndConvertToPdfResponse", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ReplaceFormulasAndConvertToPdfResponse")
    MsoDocumentAndPdfOutput replaceFormulasAndConvertToPdf(
            @WebParam(name = "msoDocumentAndFormulasInput", targetNamespace = "http://webmedia.ee/mso") MsoDocumentAndFormulasInput msoDocumentAndFormulasInput);

    @WebMethod
    @WebResult(name = "modifiedFormulasOutput", targetNamespace = "http://webmedia.ee/mso")
    @RequestWrapper(localName = "modifiedFormulas", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ModifiedFormulas")
    @ResponseWrapper(localName = "modifiedFormulasResponse", targetNamespace = "http://webmedia.ee/mso", className = "ee.webmedia.mso.ModifiedFormulasResponse")
    ModifiedFormulasOutput getModifiedFormulas(
            @WebParam(name = "msoDocumentInput", targetNamespace = "http://webmedia.ee/mso") MsoDocumentInput msoDocumentInput);

}
