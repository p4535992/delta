
package ee.webmedia.mso;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ee.webmedia.mso package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ConvertToPdfResponse_QNAME = new QName("http://webmedia.ee/mso", "convertToPdfResponse");
    private final static QName _ConvertToPdf_QNAME = new QName("http://webmedia.ee/mso", "convertToPdf");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ee.webmedia.mso
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MsoInput }
     * 
     */
    public MsoInput createMsoInput() {
        return new MsoInput();
    }

    /**
     * Create an instance of {@link ConvertToPdf }
     * 
     */
    public ConvertToPdf createConvertToPdf() {
        return new ConvertToPdf();
    }

    /**
     * Create an instance of {@link ConvertToPdfResponse }
     * 
     */
    public ConvertToPdfResponse createConvertToPdfResponse() {
        return new ConvertToPdfResponse();
    }

    /**
     * Create an instance of {@link MsoOutput }
     * 
     */
    public MsoOutput createMsoOutput() {
        return new MsoOutput();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ConvertToPdfResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webmedia.ee/mso", name = "convertToPdfResponse")
    public JAXBElement<ConvertToPdfResponse> createConvertToPdfResponse(ConvertToPdfResponse value) {
        return new JAXBElement<ConvertToPdfResponse>(_ConvertToPdfResponse_QNAME, ConvertToPdfResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ConvertToPdf }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webmedia.ee/mso", name = "convertToPdf")
    public JAXBElement<ConvertToPdf> createConvertToPdf(ConvertToPdf value) {
        return new JAXBElement<ConvertToPdf>(_ConvertToPdf_QNAME, ConvertToPdf.class, null, value);
    }

}
