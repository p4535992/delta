package ee.webmedia.alfresco.mso.service;

import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Alar Kvell
 */
public class MsoServiceDelegatingImpl implements MsoService, InitializingBean {

    private MsoService delegate;

    private boolean interfaceVersion2;
    private String endpointAddress;
    private int httpClientReceiveTimeout;
    private Set<String> supportedSourceMimetypesForPdf;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (interfaceVersion2) {
            MsoService2Impl mso = new MsoService2Impl();
            mso.setEndpointAddress(endpointAddress);
            mso.setHttpClientReceiveTimeout(httpClientReceiveTimeout);
            mso.setSupportedSourceMimetypesForPdf(supportedSourceMimetypesForPdf);
            mso.afterPropertiesSet();
            delegate = mso;
        } else {
            MsoService1Impl mso = new MsoService1Impl();
            mso.setEndpointAddress(endpointAddress);
            mso.setHttpClientReceiveTimeout(httpClientReceiveTimeout);
            mso.setSupportedSourceMimetypesForPdf(supportedSourceMimetypesForPdf);
            mso.afterPropertiesSet();
            delegate = mso;
        }
    }

    public void setInterfaceVersion2(boolean interfaceVersion2) {
        this.interfaceVersion2 = interfaceVersion2;
    }

    public void setEndpointAddress(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    public void setHttpClientReceiveTimeout(int httpClientReceiveTimeout) {
        this.httpClientReceiveTimeout = httpClientReceiveTimeout;
    }

    public void setSupportedSourceMimetypesForPdf(Set<String> supportedSourceMimetypesForPdf) {
        this.supportedSourceMimetypesForPdf = supportedSourceMimetypesForPdf;
    }

    // Delegate methods

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public boolean isTransformableToPdf(String sourceMimetype) {
        return delegate.isTransformableToPdf(sourceMimetype);
    }

    @Override
    public boolean isFormulasReplaceable(String sourceMimetype) {
        return delegate.isFormulasReplaceable(sourceMimetype);
    }

    @Override
    public void transformToPdf(ContentReader documentReader, ContentWriter pdfWriter) throws Exception {
        delegate.transformToPdf(documentReader, pdfWriter);
    }

    @Override
    public void replaceFormulas(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter) throws Exception {
        delegate.replaceFormulas(formulas, documentReader, documentWriter);
    }

    @Override
    public void replaceFormulasAndTransformToPdf(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter, ContentWriter pdfWriter)
            throws Exception {
        delegate.replaceFormulasAndTransformToPdf(formulas, documentReader, documentWriter, pdfWriter);
    }

    @Override
    public Map<String, String> modifiedFormulas(ContentReader documentReader) throws Exception {
        return delegate.modifiedFormulas(documentReader);
    }

}
