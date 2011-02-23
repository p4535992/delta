package ee.webmedia.alfresco.ocr.service;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.faces.event.ActionEvent;
import javax.xml.ws.BindingProvider;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.ocr.model.OcrModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ContentReaderDataSource;
import ee.webmedia.ocr.Ocr;
import ee.webmedia.ocr.OcrInput;
import ee.webmedia.ocr.OcrOutput;

public class OcrServiceImpl implements OcrService, InitializingBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OcrServiceImpl.class);

    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private TransactionService transactionService;
    private MimetypeService mimetypeService;
    private GeneralService generalService;
    private String endpointAddress;

    private Ocr ocr;
    private OcrWorker worker;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isEmpty(endpointAddress)) {
            if (log.isDebugEnabled()) {
                log.debug("Ocr service endpoint address not set");
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Initializing Ocr service port");
        }
        Ocr port = (new ee.webmedia.ocr.OcrService()).getOcrPort();
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);

        // Set HTTP request read timeout at CXF layer
        // http://lhein.blogspot.com/2008/09/apache-cxf-and-time-outs.html
        HTTPConduit http = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        http.getClient().setReceiveTimeout(7200000); // 2 hours timeout

        ocr = port;
        if (log.isDebugEnabled()) {
            log.debug("Successfully initialized Ocr service port and set endpoint address: " + endpointAddress);
        }

        worker = new OcrWorker();
        worker.setOcrService(this); // worker uses this service without interceptors
        Thread thread = new Thread(worker, "OcrWorker");
        thread.start();
    }

    @Override
    public boolean isOcrAvailable() {
        return ocr != null;
    }

    @Override
    public void queueOcr(NodeRef nodeRef) {
        if (worker != null) {
            worker.queue(nodeRef);
        }
    }

    @Override
    // TODO remove
    public void queueOcr(ActionEvent event) {
        queueOcr(new NodeRef(ActionUtil.getParam(event, "nodeRef")));
    }

    @Override
    public void performOcr(final NodeRef nodeRef) {
        if (ocr == null) {
            throw new IllegalStateException("Ocr service is not available");
        }
        final long startTime = System.currentTimeMillis();

        // ====================================================================
        // FIRST TRANSACTION
        // ====================================================================
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        ContentReader reader = retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<ContentReader>() {
            @Override
            public ContentReader execute() throws Throwable {
                return foo(nodeRef);
            }
        }, true, true);
        if (reader == null) {
            return;
        }
        // ====================================================================
        // Web service call is under no transaction, because it takes a long time
        // (several minutes with medium documents and even longer with bigger documents)

        OcrInput ocrInput = new OcrInput();
        ocrInput.setContent(new DataHandler(new ContentReaderDataSource(reader, null)));
        if (log.isDebugEnabled()) {
            log.debug("Sending request to perform OCR, reader=" + reader);
        }
        final Date ocrStarted = new Date();
        final OcrOutput ocrOutput = ocr.convertToPdf(ocrInput);
        final Date ocrCompleted = new Date();

        // ====================================================================
        // SECOND TRANSACTION
        // ====================================================================
        retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>() {
            @Override
            public Object execute() throws Throwable {
                bar(nodeRef, ocrOutput, ocrStarted, ocrCompleted, startTime);
                return null;
            }
        }, false, true);
    }

    private ContentReader foo(NodeRef nodeRef) {
        if (!nodeService.exists(nodeRef)) {
            log.debug("File was deleted before processing started, ignoring: " + nodeRef);
            return null;
        }
        if (nodeService.hasAspect(nodeRef, OcrModel.Aspects.OCR_COMPLETED)) {
//            if (log.isDebugEnabled()) {
//                log.debug("File already has completed OCR, ignoring: " + nodeRef);
//            }
            // File already has completed OCR, ignore
            return null;
        }
        ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
        if (contentData == null) {
            log.warn("File has no ContentData: " + nodeRef);
            return null;
        }
        if (!MimetypeMap.MIMETYPE_PDF.equals(contentData.getMimetype())) {
            // ignore non-PDF files
            return null;
        }
        return fileFolderService.getReader(nodeRef);
    }

    private void bar(NodeRef nodeRef, OcrOutput ocrOutput, Date ocrStarted, Date ocrCompleted, long startTime) throws Exception {
        if (!nodeService.exists(nodeRef)) {
            log.debug("File was deleted during processing, ignoring: " + nodeRef);
            return;
        }
        ContentWriter writer = fileFolderService.getWriter(nodeRef);
        String mimeType = ocrOutput.getContent().getContentType();
        writer.setMimetype(mimeType);
        writer.setEncoding("UTF-8"); // reset encoding to default
        writer.putContent(ocrOutput.getContent().getInputStream());

        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(OcrModel.Props.OCR_LOG, ocrOutput.getLog());
        props.put(OcrModel.Props.OCR_STARTED_DATE_TIME, ocrStarted);
        props.put(OcrModel.Props.OCR_COMPLETED_DATE_TIME, ocrCompleted);
        nodeService.addAspect(nodeRef, OcrModel.Aspects.OCR_COMPLETED, props);

        // If file extension is not yet .pdf, then change it
        String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        String extension = "." + mimetypeService.getExtension(mimeType);
        if (!StringUtils.endsWithIgnoreCase(name, extension)) {
            NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
            name = FilenameUtils.getBaseName(name) + extension;
            name = generalService.getUniqueFileName(parent, name);
            fileFolderService.rename(nodeRef, name);
        }

        long duration = System.currentTimeMillis() - startTime;
        if (log.isDebugEnabled()) {
            log.debug("Completed OCR in " + duration + " ms\n    name=" + name + "\n    writer=" + writer + "\n    ocrCompleted="
                    + WmNode.toString(props.entrySet()));
        }
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setEndpointAddress(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

}
