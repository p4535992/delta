package ee.webmedia.alfresco.mso.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.ws.BindingProvider;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.mso.ws.Formula;
import ee.webmedia.alfresco.mso.ws.ModifiedFormulasOutput;
import ee.webmedia.alfresco.mso.ws.Mso;
import ee.webmedia.alfresco.mso.ws.MsoDocumentAndFormulasInput;
import ee.webmedia.alfresco.mso.ws.MsoDocumentAndPdfOutput;
import ee.webmedia.alfresco.mso.ws.MsoDocumentInput;
import ee.webmedia.alfresco.mso.ws.MsoDocumentOutput;
import ee.webmedia.alfresco.mso.ws.MsoPdfOutput;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.ContentReaderDataSource;
import ee.webmedia.alfresco.utils.MimeUtil;

/*
 * This service doesn't deal with charset information, input and output files are passed to and from MSO Service unmodified.
 * MSO Service itself performs necessary charset conversion for TXT/HTML files and adds <HTML> tags to HTML file contents when needed.
 */
public class MsoServiceImpl implements MsoService, InitializingBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MsoServiceImpl.class);

    private String endpointAddress;
    private int httpClientReceiveTimeout;
    private Set<String> supportedSourceMimetypes;

    private Mso mso;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isEmpty(endpointAddress)) {
            log.info("Mso service endpoint address not set");
            return;
        }

        log.info("Initializing Mso service port");
        Mso port = (new ee.webmedia.alfresco.mso.ws.MsoService()).getMsoPort();
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);

        // Set HTTP request read timeout at CXF layer
        // http://lhein.blogspot.com/2008/09/apache-cxf-and-time-outs.html
        HTTPConduit http = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        http.getClient().setReceiveTimeout(httpClientReceiveTimeout * 1000);

        mso = port;
        log.info("Successfully initialized Mso service port and set endpoint address: " + endpointAddress);
    }

    @Override
    public boolean isAvailable() {
        return mso != null;
    }

    @Override
    public boolean isTransformableToPdf(String sourceMimetype) {
        return supportedSourceMimetypes.contains(StringUtils.lowerCase(sourceMimetype));
    }

    @Override
    public boolean isFormulasReplaceable(String sourceMimetype) {
        // XXX DOT files have mimetype application/octet-stream
        // Ideally both DOC and DOT should have mimetype application/msword
        // Fortunately, DocumentTemplateService passes only DOC or DOT files to us, not every binary file
        // So without the check in DocumentTemplateService, every binary file would be passed to word, which would be unnecessary and very time consuming
        return MimetypeMap.MIMETYPE_WORD.equalsIgnoreCase(sourceMimetype) || MimetypeMap.MIMETYPE_BINARY.equalsIgnoreCase(sourceMimetype);
    }

    @Override
    public ModifiedFormulasOutput modifiedFormulas(ContentReader documentReader) throws Exception {
        try {
            if (documentReader == null) {
                return null;
            }

            MsoDocumentInput input = new MsoDocumentInput();
            DataSource dataSource = new ContentReaderDataSource(documentReader, null);
            input.setDocumentFile(new DataHandler(dataSource));
            log.info("Sending request to perform Mso.convertToPdf, documentReader=" + documentReader);

            long startTime = System.nanoTime();
            ModifiedFormulasOutput output;
            try {
                output = mso.modifiedFormulas(input);
            } finally {
                StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
            }
            long duration = CalendarUtil.duration(startTime);

            log.info("Completed Mso.getModifiedFormulas in " + duration + " ms");
            return output;
        } catch (Exception e) {
            log.error("Error in getModifiedFormulas", e);
            throw e;
        }
    }

    @Override
    public void transformToPdf(ContentReader documentReader, ContentWriter pdfWriter) throws Exception {
        try {
            requireAvailable();
            if (!MimetypeMap.MIMETYPE_PDF.equalsIgnoreCase(pdfWriter.getMimetype())) {
                throw new IllegalArgumentException("Only target mime type " + MimetypeMap.MIMETYPE_PDF + " is supported");
            }
            if (documentReader == null) {
                return;
            }
            String mimetype = documentReader.getMimetype().toLowerCase();
            if (!isTransformableToPdf(mimetype)) {
                throw new IllegalArgumentException("Source mime type is not supported for transformToPdf: " + mimetype);
            }

            MsoDocumentInput input = new MsoDocumentInput();
            DataSource dataSource = new ContentReaderDataSource(documentReader, null);
            input.setDocumentFile(new DataHandler(dataSource));
            log.info("Sending request to perform Mso.convertToPdf, documentReader=" + documentReader);

            long startTime = System.nanoTime();
            MsoPdfOutput output;
            try {
                output = mso.convertToPdf(input);
            } finally {
                StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
            }
            long duration = CalendarUtil.duration(startTime);

            Pair<String, String> pair = MimeUtil.getMimeTypeAndEncoding(output.getPdfFile().getContentType());
            pdfWriter.setMimetype(pair.getFirst());
            pdfWriter.setEncoding(pair.getSecond());
            pdfWriter.putContent(output.getPdfFile().getInputStream());

            log.info("Completed Mso.convertToPdf in " + duration + " ms, pdfWriter=" + pdfWriter);
        } catch (Exception e) {
            log.error("Error in transformToPdf", e);
            throw e;
        }
    }

    private void requireAvailable() {
        if (mso == null) {
            throw new IllegalStateException("Mso service is not available");
        }
    }

    @Override
    public void replaceFormulas(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter) throws Exception {
        try {
            MsoDocumentAndFormulasInput input = replaceFormulasPrepare(formulas, documentReader);
            if (input == null) {
                return;
            }

            log.info("Sending request to perform Mso.replaceFormulas, formulas=[" + formulas.size() + "] documentReader=" + documentReader);

            long startTime = System.nanoTime();
            MsoDocumentOutput output;
            try {
                output = mso.replaceFormulas(input);
            } finally {
                StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
            }
            long duration = CalendarUtil.duration(startTime);

            Pair<String, String> pair = MimeUtil.getMimeTypeAndEncoding(output.getDocumentFile().getContentType());
            documentWriter.setMimetype(pair.getFirst());
            documentWriter.setEncoding(pair.getSecond());
            documentWriter.putContent(output.getDocumentFile().getInputStream());

            log.info("Completed Mso.replaceFormulas in " + duration + " ms, documentWriter=" + documentWriter);

        } catch (Exception e) {
            log.error("Error in replaceFormulas", e);
            throw e;
        }
    }

    @Override
    public void replaceFormulasAndTransformToPdf(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter, ContentWriter pdfWriter)
            throws Exception {
        try {
            MsoDocumentAndFormulasInput input = replaceFormulasPrepare(formulas, documentReader);
            if (input == null) {
                return;
            }

            log.info("Sending request to perform Mso.replaceFormulasAndTransformToPdf, formulas=[" + formulas.size() + "] documentReader=" + documentReader);

            long startTime = System.nanoTime();
            MsoDocumentAndPdfOutput output;
            try {
                output = mso.replaceFormulasAndConvertToPdf(input);
            } finally {
                StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
            }
            long duration = CalendarUtil.duration(startTime);

            Pair<String, String> pair = MimeUtil.getMimeTypeAndEncoding(output.getDocumentFile().getContentType());
            documentWriter.setMimetype(pair.getFirst());
            documentWriter.setEncoding(pair.getSecond());
            documentWriter.putContent(output.getDocumentFile().getInputStream());

            pair = MimeUtil.getMimeTypeAndEncoding(output.getPdfFile().getContentType());
            pdfWriter.setMimetype(pair.getFirst());
            pdfWriter.setEncoding(pair.getSecond());
            pdfWriter.putContent(output.getPdfFile().getInputStream());

            log.info("Completed Mso.replaceFormulasAndTransformToPdf in " + duration + " ms, documentWriter=" + documentWriter + ", pdfWriter=" + pdfWriter);

        } catch (Exception e) {
            log.error("Error in replaceFormulasAndTransformToPdf", e);
            throw e;
        }
    }

    private MsoDocumentAndFormulasInput replaceFormulasPrepare(Map<String, String> formulas, ContentReader documentReader) {
        requireAvailable();
        if (documentReader == null) {
            return null;
        }

        String mimetype = documentReader.getMimetype().toLowerCase();
        if (MimetypeMap.MIMETYPE_BINARY.equals(mimetype)) { // XXX DOT files have mimetype application/octet-stream
            mimetype = MimetypeMap.MIMETYPE_WORD;
        }
        if (!isFormulasReplaceable(mimetype)) {
            throw new IllegalArgumentException("Source mime type is not supported for replaceFormulas: " + mimetype);
        }

        MsoDocumentAndFormulasInput input = new MsoDocumentAndFormulasInput();
        DataSource dataSource = new ContentReaderDataSource(documentReader, null, mimetype, documentReader.getEncoding());
        input.setDocumentFile(new DataHandler(dataSource));
        List<Formula> formulaList = input.getFormula();
        Set<Entry<String, String>> entrySet = formulas.entrySet();
        for (Entry<String, String> entry : entrySet) {
            Formula formula = new Formula();
            formula.setKey(entry.getKey());
            formula.setValue(entry.getValue());
            formulaList.add(formula);
        }
        return input;
    }

    public void setEndpointAddress(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    public void setHttpClientReceiveTimeout(int httpClientReceiveTimeout) {
        this.httpClientReceiveTimeout = httpClientReceiveTimeout;
    }

    public void setSupportedSourceMimetypes(Set<String> supportedSourceMimetypes) {
        this.supportedSourceMimetypes = supportedSourceMimetypes;
    }

}
