package ee.webmedia.alfresco.ocr.service;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Interface to OCR (Optical Character Recognition) webservice.
 * <p>
 * When an OCR operation is executed (either background or foreground), then:
 * <ol>
 * <li>if input file does not have {@code ocr:ocrCompleted} aspect and file format is supported (currently only PDF), then file contents from repository are passed to OCR program;
 * otherwise file is ignored;</li>
 * <li>OCR program processes the images/pages found in the file and returns the result as PDF/A file, where the recognized text is written under page images;</li>
 * <li>file contents in repository are replaced with the returned file contents, file extension is changed to .pdf, {@code ocr:ocrCompleted} aspect is added.</li>
 * </ol>
 * </p>
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public interface OcrService {

    boolean isOcrAvailable();

    /**
     * Perform OCR operation in the background. If OCR service is not configured, then returns silently. Otherwise adds command to queue and returns silently.
     * Does not throw exceptions.
     */
    void queueOcr(NodeRef nodeRef);

    /**
     * Perform OCR operation now! Throws exceptions on errors.
     * <b>Do not use this method, use the one above instead!</b>
     */
    void performOcr(NodeRef nodeRef);

    // TODO remove
    void queueOcr(ActionEvent event);

}
