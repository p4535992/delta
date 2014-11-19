package ee.webmedia.alfresco.document.scanned.bootstrap;

import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.ocr.service.OcrService;

/**
 * Iterate over all files in scanned folder and queue them to Ocr service.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class ScannedOcrBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ScannedOcrBootstrap.class);

    private GeneralService generalService;
    private OcrService ocrService;
    private String scannedFilesPath;

    @Override
    protected void executeInternal() throws Throwable {
        if (!ocrService.isOcrAvailable()) {
            log.debug("Ocr service is not available, skipping scanned files queuing for ocr");
            return;
        }

        log.debug("Ocr service is available, starting scanned files queuing for ocr");
        long startTime = System.currentTimeMillis();
        int filesCount = 0;
        NodeRef scannedRoot = generalService.getNodeRef(scannedFilesPath);
        List<FileInfo> folders = serviceRegistry.getFileFolderService().listFolders(scannedRoot);
        for (FileInfo folder : folders) {
            List<FileInfo> files = serviceRegistry.getFileFolderService().listFiles(folder.getNodeRef());
            filesCount += files.size();
            for (FileInfo file : files) {
                ocrService.queueOcr(file.getNodeRef());
            }
        }
        long duration = System.currentTimeMillis() - startTime;
        log.debug("Completed scanned files queuing for ocr in " + duration + " ms - processed " + folders.size() + " folders, " + filesCount + " files");
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setOcrService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public void setScannedFilesPath(String scannedFilesPath) {
        this.scannedFilesPath = scannedFilesPath;
    }

}
