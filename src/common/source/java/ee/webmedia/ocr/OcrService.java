
package ee.webmedia.ocr;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "OcrService", targetNamespace = "http://webmedia.ee/ocr", wsdlLocation = "OcrService.wsdl")
public class OcrService
    extends Service
{

    private final static URL OCRSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(ee.webmedia.ocr.OcrService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = ee.webmedia.ocr.OcrService.class.getResource("");
            url = new URL(baseUrl, "OcrService.wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'OcrService.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        OCRSERVICE_WSDL_LOCATION = url;
    }

    public OcrService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public OcrService() {
        super(OCRSERVICE_WSDL_LOCATION, new QName("http://webmedia.ee/ocr", "OcrService"));
    }

    /**
     * 
     * @return
     *     returns Ocr
     */
    @WebEndpoint(name = "OcrPort")
    public Ocr getOcrPort() {
        return super.getPort(new QName("http://webmedia.ee/ocr", "OcrPort"), Ocr.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Ocr
     */
    @WebEndpoint(name = "OcrPort")
    public Ocr getOcrPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://webmedia.ee/ocr", "OcrPort"), Ocr.class, features);
    }

}
