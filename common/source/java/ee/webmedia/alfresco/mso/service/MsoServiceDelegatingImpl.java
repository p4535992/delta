package ee.webmedia.alfresco.mso.service;

import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.springframework.beans.factory.InitializingBean;

<<<<<<< HEAD
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;

/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        try {
            delegate.transformToPdf(documentReader, pdfWriter);
            MonitoringUtil.logSuccess(MonitoredService.OUT_MSO);
        } catch (Exception e) {
            MonitoringUtil.logError(MonitoredService.OUT_MSO, e);
            throw e;
        }
=======
        delegate.transformToPdf(documentReader, pdfWriter);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public void replaceFormulas(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter) throws Exception {
<<<<<<< HEAD
        try {
            delegate.replaceFormulas(formulas, documentReader, documentWriter);
            MonitoringUtil.logSuccess(MonitoredService.OUT_MSO);
        } catch (Exception e) {
            MonitoringUtil.logError(MonitoredService.OUT_MSO, e);
            throw e;
        }
=======
        delegate.replaceFormulas(formulas, documentReader, documentWriter);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public void replaceFormulasAndTransformToPdf(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter, ContentWriter pdfWriter)
            throws Exception {
<<<<<<< HEAD
        try {
            delegate.replaceFormulasAndTransformToPdf(formulas, documentReader, documentWriter, pdfWriter);
            MonitoringUtil.logSuccess(MonitoredService.OUT_MSO);
        } catch (Exception e) {
            MonitoringUtil.logError(MonitoredService.OUT_MSO, e);
            throw e;
        }
=======
        delegate.replaceFormulasAndTransformToPdf(formulas, documentReader, documentWriter, pdfWriter);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public Map<String, String> modifiedFormulas(ContentReader documentReader) throws Exception {
<<<<<<< HEAD
        try {
            Map<String, String> modifiedFormulas = delegate.modifiedFormulas(documentReader);
            MonitoringUtil.logSuccess(MonitoredService.OUT_MSO);
            return modifiedFormulas;
        } catch (Exception e) {
            MonitoringUtil.logError(MonitoredService.OUT_MSO, e);
            throw e;
        }
=======
        return delegate.modifiedFormulas(documentReader);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

}
