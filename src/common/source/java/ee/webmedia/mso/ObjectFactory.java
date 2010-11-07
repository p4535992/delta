
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

    private final static QName _ReplaceFormulas_QNAME = new QName("http://webmedia.ee/mso", "replaceFormulas");
    private final static QName _ReplaceFormulasAndConvertToPdf_QNAME = new QName("http://webmedia.ee/mso", "replaceFormulasAndConvertToPdf");
    private final static QName _ReplaceFormulasAndConvertToPdfResponse_QNAME = new QName("http://webmedia.ee/mso", "replaceFormulasAndConvertToPdfResponse");
    private final static QName _ReplaceFormulasResponse_QNAME = new QName("http://webmedia.ee/mso", "replaceFormulasResponse");
    private final static QName _ConvertToPdfResponse_QNAME = new QName("http://webmedia.ee/mso", "convertToPdfResponse");
    private final static QName _ConvertToPdf_QNAME = new QName("http://webmedia.ee/mso", "convertToPdf");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ee.webmedia.mso
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MsoPdfOutput }
     * 
     */
    public MsoPdfOutput createMsoPdfOutput() {
        return new MsoPdfOutput();
    }

    /**
     * Create an instance of {@link ConvertToPdf }
     * 
     */
    public ConvertToPdf createConvertToPdf() {
        return new ConvertToPdf();
    }

    /**
     * Create an instance of {@link ReplaceFormulasResponse }
     * 
     */
    public ReplaceFormulasResponse createReplaceFormulasResponse() {
        return new ReplaceFormulasResponse();
    }

    /**
     * Create an instance of {@link ConvertToPdfResponse }
     * 
     */
    public ConvertToPdfResponse createConvertToPdfResponse() {
        return new ConvertToPdfResponse();
    }

    /**
     * Create an instance of {@link MsoDocumentInput }
     * 
     */
    public MsoDocumentInput createMsoDocumentInput() {
        return new MsoDocumentInput();
    }

    /**
     * Create an instance of {@link ReplaceFormulasAndConvertToPdf }
     * 
     */
    public ReplaceFormulasAndConvertToPdf createReplaceFormulasAndConvertToPdf() {
        return new ReplaceFormulasAndConvertToPdf();
    }

    /**
     * Create an instance of {@link ReplaceFormulas }
     * 
     */
    public ReplaceFormulas createReplaceFormulas() {
        return new ReplaceFormulas();
    }

    /**
     * Create an instance of {@link ReplaceFormulasAndConvertToPdfResponse }
     * 
     */
    public ReplaceFormulasAndConvertToPdfResponse createReplaceFormulasAndConvertToPdfResponse() {
        return new ReplaceFormulasAndConvertToPdfResponse();
    }

    /**
     * Create an instance of {@link Formula }
     * 
     */
    public Formula createFormula() {
        return new Formula();
    }

    /**
     * Create an instance of {@link MsoDocumentOutput }
     * 
     */
    public MsoDocumentOutput createMsoDocumentOutput() {
        return new MsoDocumentOutput();
    }

    /**
     * Create an instance of {@link MsoDocumentAndFormulasInput }
     * 
     */
    public MsoDocumentAndFormulasInput createMsoDocumentAndFormulasInput() {
        return new MsoDocumentAndFormulasInput();
    }

    /**
     * Create an instance of {@link MsoDocumentAndPdfOutput }
     * 
     */
    public MsoDocumentAndPdfOutput createMsoDocumentAndPdfOutput() {
        return new MsoDocumentAndPdfOutput();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReplaceFormulas }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webmedia.ee/mso", name = "replaceFormulas")
    public JAXBElement<ReplaceFormulas> createReplaceFormulas(ReplaceFormulas value) {
        return new JAXBElement<ReplaceFormulas>(_ReplaceFormulas_QNAME, ReplaceFormulas.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReplaceFormulasAndConvertToPdf }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webmedia.ee/mso", name = "replaceFormulasAndConvertToPdf")
    public JAXBElement<ReplaceFormulasAndConvertToPdf> createReplaceFormulasAndConvertToPdf(ReplaceFormulasAndConvertToPdf value) {
        return new JAXBElement<ReplaceFormulasAndConvertToPdf>(_ReplaceFormulasAndConvertToPdf_QNAME, ReplaceFormulasAndConvertToPdf.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReplaceFormulasAndConvertToPdfResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webmedia.ee/mso", name = "replaceFormulasAndConvertToPdfResponse")
    public JAXBElement<ReplaceFormulasAndConvertToPdfResponse> createReplaceFormulasAndConvertToPdfResponse(ReplaceFormulasAndConvertToPdfResponse value) {
        return new JAXBElement<ReplaceFormulasAndConvertToPdfResponse>(_ReplaceFormulasAndConvertToPdfResponse_QNAME, ReplaceFormulasAndConvertToPdfResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReplaceFormulasResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webmedia.ee/mso", name = "replaceFormulasResponse")
    public JAXBElement<ReplaceFormulasResponse> createReplaceFormulasResponse(ReplaceFormulasResponse value) {
        return new JAXBElement<ReplaceFormulasResponse>(_ReplaceFormulasResponse_QNAME, ReplaceFormulasResponse.class, null, value);
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
