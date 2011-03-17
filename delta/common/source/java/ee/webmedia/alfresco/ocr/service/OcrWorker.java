package ee.webmedia.alfresco.ocr.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;

public class OcrWorker implements Runnable {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(OcrWorker.class);

    private OcrService ocrService;
    private BlockingQueue<NodeRef> queue = new LinkedBlockingQueue<NodeRef>();

    public void queue(NodeRef nodeRef) {
        queue.add(nodeRef);
    }

    @Override
    public void run() {
        log.debug("Started thread " + Thread.currentThread().getName());
        AuthenticationUtil.setRunAsUserSystem();
        NodeRef nodeRef = null;
        while (true) {
            try {
                nodeRef = queue.take();
                ocrService.performOcr(nodeRef);
                nodeRef = null;
            } catch (Exception e) {
                log.error("Error while processing queue, current nodeRef=" + nodeRef, e);
            }
        }
    }
    
    public void setOcrService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

}
