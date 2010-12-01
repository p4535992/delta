
package ee.webmedia.mso;

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
@WebServiceClient(name = "MsoService", targetNamespace = "http://webmedia.ee/mso", wsdlLocation = "MsoService.wsdl")
public class MsoService
    extends Service
{

    private final static URL MSOSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(ee.webmedia.mso.MsoService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = ee.webmedia.mso.MsoService.class.getResource("");
            url = new URL(baseUrl, "MsoService.wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'MsoService.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        MSOSERVICE_WSDL_LOCATION = url;
    }

    public MsoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public MsoService() {
        super(MSOSERVICE_WSDL_LOCATION, new QName("http://webmedia.ee/mso", "MsoService"));
    }

    /**
     * 
     * @return
     *     returns Mso
     */
    @WebEndpoint(name = "MsoPort")
    public Mso getMsoPort() {
        return super.getPort(new QName("http://webmedia.ee/mso", "MsoPort"), Mso.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Mso
     */
    @WebEndpoint(name = "MsoPort")
    public Mso getMsoPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://webmedia.ee/mso", "MsoPort"), Mso.class, features);
    }

}