package ee.webmedia.alfresco.mso.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.mso2.ws.ArrayOfformula;
import ee.webmedia.alfresco.mso2.ws.Formula;
import ee.webmedia.alfresco.mso2.ws.FormulaOutput;
import ee.webmedia.alfresco.mso2.ws.MsoDocumentAndFormulasInput;
import ee.webmedia.alfresco.mso2.ws.MsoDocumentInput;
import ee.webmedia.alfresco.mso2.ws.MsoDocumentOutput;
import ee.webmedia.alfresco.mso2.ws.MsoPdfOutput;
import ee.webmedia.alfresco.mso2.ws.MsoPortBinding;
import ee.webmedia.alfresco.mso2.ws.ObjectFactory;
import ee.webmedia.alfresco.utils.CalendarUtil;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class MsoService2Impl implements MsoService, InitializingBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MsoService2Impl.class);

    /*
     * This service doesn't deal with charset information, input and output files are passed to and from MSO Service unmodified.
     * MSO Service itself performs necessary charset conversion for TXT/HTML files and adds <HTML> tags to HTML file contents when needed.
     */

    private String endpointAddress;
    private int httpClientReceiveTimeout;
    private Set<String> supportedSourceMimetypesForPdf;

    private final ObjectFactory objectFactory = new ObjectFactory();
    private MsoPortBinding mso;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isEmpty(endpointAddress)) {
            log.info("Mso service 2 endpoint address not set");
            return;
        }

        log.info("Initializing Mso service 2 port");
        MsoPortBinding port = (new ee.webmedia.alfresco.mso2.ws.MsoService()).getMsoPortBinding();
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);

        // Set HTTP request read timeout at CXF layer
        // http://lhein.blogspot.com/2008/09/apache-cxf-and-time-outs.html
        HTTPConduit http = (HTTPConduit) ClientProxy.getClient(port).getConduit();
<<<<<<< HEAD
        http.getClient().setReceiveTimeout(httpClientReceiveTimeout * 1000);
=======
        http.getClient().setReceiveTimeout(httpClientReceiveTimeout * 1000); // Takes milliseconds
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);

        mso = port;
        log.info("Successfully initialized Mso service 2 port and set endpoint address: " + endpointAddress);
    }

    @Override
    public boolean isAvailable() {
        return mso != null;
    }

    @Override
    public boolean isTransformableToPdf(String sourceMimetype) {
        return supportedSourceMimetypesForPdf.contains(StringUtils.lowerCase(sourceMimetype));
    }

    @Override
    public boolean isFormulasReplaceable(String sourceMimetype) {
        // Basically the same check is performed in DocumentTemplateServiceImpl#replaceFormulas, but with file extensions (.doc .docx .dot .dotx)
        return MIMETYPE_DOC.equalsIgnoreCase(sourceMimetype) || MIMETYPE_DOCX.equalsIgnoreCase(sourceMimetype)
                || MIMETYPE_DOT.equalsIgnoreCase(sourceMimetype) || MIMETYPE_DOTX.equalsIgnoreCase(sourceMimetype);
    }

    @Override
    public Map<String, String> modifiedFormulas(ContentReader documentReader) throws Exception {
        try {
            if (documentReader == null) {
                return null;
            }

            Map<String, String> formulas = new LinkedHashMap<String, String>();
            long duration = -1;
            try {

                MsoDocumentInput input = getDocumentInput(documentReader);
                log.info("Sending request to perform Mso2.modifiedFormulas, documentReader=" + documentReader);

                long startTime = System.nanoTime();
                FormulaOutput output;
                try {
                    output = mso.modifiedFormulas(input);
                } finally {
                    StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
                }
                duration = CalendarUtil.duration(startTime);

                if (output.getFormulas() == null || output.getFormulas().getValue() == null) {
                    return null;
                }
                for (Formula formula : output.getFormulas().getValue().getFormula()) {
                    if (formula.getKey() == null || StringUtils.isBlank(formula.getKey().getValue())) {
                        continue;
                    }
                    formulas.put(formula.getKey().getValue(), formula.getValue() == null ? null : formula.getValue().getValue());
                }
                return formulas;

            } finally {
                log.info("PERFORMANCE: query mso2.modifiedFormulas - " + duration + " ms|" + documentReader.getSize() + "|" + documentReader.getMimetype() + "|"
                        + documentReader.getEncoding() + "|" + formulas.size());
            }
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

            long duration = -1;
            try {
                MsoDocumentInput input = getDocumentInput(documentReader);
                log.info("Sending request to perform Mso2.convertToPdf, documentReader=" + documentReader);

                long startTime = System.nanoTime();
                MsoPdfOutput output;
                try {
                    output = mso.convertToPdf(input);
                } finally {
                    StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
                }
                duration = CalendarUtil.duration(startTime);

                pdfWriter.setMimetype(MimetypeMap.MIMETYPE_PDF);
                pdfWriter.setEncoding("UTF-8");
                ByteArrayInputStream bis = new ByteArrayInputStream(output.getPdfFile().getValue());
                pdfWriter.putContent(bis);

            } finally {
                log.info("PERFORMANCE: query mso2.convertToPdf - " + duration + " ms|" + documentReader.getSize() + "|" + documentReader.getMimetype() + "|"
                        + documentReader.getEncoding() + "|" + pdfWriter.getSize());
            }
        } catch (Exception e) {
            log.error("Error in transformToPdf! | ContentURL: " + documentReader.getContentUrl() + " | Input mimetype: " + documentReader.getMimetype().toLowerCase(), e);
            throw e;
        }
    }

    private void requireAvailable() {
        if (mso == null) {
            throw new IllegalStateException("Mso service 2 is not available");
        }
    }

    @Override
    public void replaceFormulas(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter) throws Exception {
        try {
            MsoDocumentAndFormulasInput input = replaceFormulasPrepare(formulas, documentReader);
            if (input == null) {
                return;
            }

            long duration = -1;
            try {
                log.info("Sending request to perform Mso2.replaceFormulas, formulas=[" + formulas.size() + "] documentReader=" + documentReader);

                long startTime = System.nanoTime();
                MsoDocumentOutput output;
                try {
                    output = mso.replaceFormulas(input);
                } finally {
                    StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
                }
                duration = CalendarUtil.duration(startTime);

                String inputMimeType = documentReader.getMimetype();
                String outputMimeType;
                if (inputMimeType.equalsIgnoreCase(MIMETYPE_DOT) || inputMimeType.equalsIgnoreCase(MIMETYPE_DOC)) {
                    outputMimeType = MIMETYPE_DOC;
                } else if (inputMimeType.equalsIgnoreCase(MIMETYPE_DOTX) || inputMimeType.equalsIgnoreCase(MIMETYPE_DOCX)) {
                    outputMimeType = MIMETYPE_DOCX;
                } else {
                    throw new IllegalArgumentException("Unsupported input mimeType " + inputMimeType);
                }
                documentWriter.setMimetype(outputMimeType);
                documentWriter.setEncoding("UTF-8");
                ByteArrayInputStream bis = new ByteArrayInputStream(output.getDocumentFile().getValue());
                documentWriter.putContent(bis);

            } finally {
                log.info("PERFORMANCE: query mso2.replaceFormulas - " + duration + " ms|" + documentReader.getSize() + "|" + documentReader.getMimetype() + "|"
                        + documentReader.getEncoding() + "|" + documentWriter.getSize() + "|" + formulas.size());
            }
        } catch (Exception e) {
            log.error("Error in replaceFormulas", e);
            throw e;
        }
    }

    @Override
    public void replaceFormulasAndTransformToPdf(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter, ContentWriter pdfWriter)
            throws Exception {
        // TODO SMIT's new MSO service didn't return document file in the interface as of 13.04.2012
// @formatter:off
/*
        try {
            MsoDocumentAndFormulasInput input = replaceFormulasPrepare(formulas, documentReader);
            if (input == null) {
                return;
            }

            long duration = -1;
            try {
                log.info("Sending request to perform Mso2.replaceFormulasAndTransformToPdf, formulas=[" + formulas.size() + "] documentReader=" + documentReader);

                long startTime = System.nanoTime();
                MsoDocumentAndPdfOutput output;
                try {
                    output = mso.replaceFormulasAndConvertToPdf(input);
                } finally {
                    StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.SRV_MSO, startTime);
                }
                duration = CalendarUtil.duration(startTime);

                String inputMimeType = documentReader.getMimetype();
                String outputMimeType;
                if (inputMimeType.equalsIgnoreCase(MIMETYPE_DOT) || inputMimeType.equalsIgnoreCase(MIMETYPE_DOC)) {
                    outputMimeType = MIMETYPE_DOC;
                } else if (inputMimeType.equalsIgnoreCase(MIMETYPE_DOTX) || inputMimeType.equalsIgnoreCase(MIMETYPE_DOCX)) {
                    outputMimeType = MIMETYPE_DOCX;
                } else {
                    throw new IllegalArgumentException("Unsupported input mimeType " + inputMimeType);
                }
                documentWriter.setMimetype(outputMimeType);
                documentWriter.setEncoding("UTF-8");
                ByteArrayInputStream bis = new ByteArrayInputStream(output.getDocumentFile().getValue());
                documentWriter.putContent(bis);

                pdfWriter.setMimetype(MimetypeMap.MIMETYPE_PDF);
                pdfWriter.setEncoding("UTF-8");
                bis = new ByteArrayInputStream(output.getPdfFile().getValue());
                pdfWriter.putContent(bis);

            } finally {
                log.info("PERFORMANCE: query mso2.replaceFormulasAndConvertToPdf - " + duration + " ms|" + documentReader.getSize() + "|" + documentReader.getMimetype() + "|"
                        + documentReader.getEncoding() + "|" + documentWriter.getSize() + "|" + pdfWriter.getSize() + "|" + formulas.size());
            }
        } catch (Exception e) {
            log.error("Error in replaceFormulasAndTransformToPdf", e);
            throw e;
        }
 */
// @formatter:on
    }

    private MsoDocumentAndFormulasInput replaceFormulasPrepare(Map<String, String> formulas, ContentReader documentReader) {
        requireAvailable();
        if (documentReader == null) {
            return null;
        }

        String mimetype = documentReader.getMimetype().toLowerCase();
        if (!isFormulasReplaceable(mimetype)) {
            throw new IllegalArgumentException("Source mime type is not supported for replaceFormulas: " + mimetype);
        }

        return getDocumentAndFormulasInput(formulas, documentReader);
    }

    private MsoDocumentAndFormulasInput getDocumentAndFormulasInput(Map<String, String> formulas, ContentReader documentReader) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) documentReader.getSize());
        documentReader.getContent(bos);
        JAXBElement<byte[]> documentFile = objectFactory.createMsoDocumentAndFormulasInputDocumentFile(bos.toByteArray());

<<<<<<< HEAD
=======
        JAXBElement<String> fileEncoding = objectFactory.createMsoDocumentAndFormulasInputFileEncoding(documentReader.getEncoding());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        JAXBElement<String> fileType = objectFactory.createMsoDocumentAndFormulasInputFileType(documentReader.getMimetype());

        ArrayOfformula arrayOfformula = objectFactory.createArrayOfformula();
        for (Entry<String, String> entry : formulas.entrySet()) {
            Formula formula = objectFactory.createFormula();
            formula.setKey(objectFactory.createFormulaKey(entry.getKey()));
            formula.setValue(objectFactory.createFormulaValue(entry.getValue()));
            arrayOfformula.getFormula().add(formula);
        }
        JAXBElement<ArrayOfformula> msoFormulas = objectFactory.createMsoDocumentAndFormulasInputFormulas(arrayOfformula);

        MsoDocumentAndFormulasInput input = objectFactory.createMsoDocumentAndFormulasInput();
        input.setDocumentFile(documentFile);
<<<<<<< HEAD
=======
        input.setFileEncoding(fileEncoding);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        input.setFileType(fileType);
        input.setFormulas(msoFormulas);
        return input;
    }

    private MsoDocumentInput getDocumentInput(ContentReader documentReader) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) documentReader.getSize());
        documentReader.getContent(bos);
        JAXBElement<byte[]> documentFile = objectFactory.createMsoDocumentInputDocumentFile(bos.toByteArray());

<<<<<<< HEAD
=======
        JAXBElement<String> fileEncoding = objectFactory.createMsoDocumentInputFileEncoding(documentReader.getEncoding());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        JAXBElement<String> fileType = objectFactory.createMsoDocumentInputFileType(documentReader.getMimetype());

        final MsoDocumentInput input = objectFactory.createMsoDocumentInput();
        input.setDocumentFile(documentFile);
<<<<<<< HEAD
=======
        input.setFileEncoding(fileEncoding);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        input.setFileType(fileType);
        return input;
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

}
