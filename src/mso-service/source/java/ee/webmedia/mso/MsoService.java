package ee.webmedia.mso;

import java.io.IOException;

public interface MsoService {

    MsoOutput convertToPdf(MsoInput msoInput) throws Exception;

}
