package ee.webmedia.ocr;

import java.io.IOException;

public interface OcrService {

    OcrOutput convertToPdf(OcrInput ocrInput) throws IOException;

}
