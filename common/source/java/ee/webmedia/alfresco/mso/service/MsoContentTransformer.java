package ee.webmedia.alfresco.mso.service;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformer2;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;

public class MsoContentTransformer extends AbstractContentTransformer2 {
    // private static Log log = LogFactory.getLog(MsoContentTransformer.class);

    private MsoService msoService;

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
        return msoService.isAvailable() && MimetypeMap.MIMETYPE_PDF.equalsIgnoreCase(targetMimetype) && msoService.isTransformableToPdf(sourceMimetype);
    }

    @Override
    protected void transformInternal(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
        try {
            msoService.transformToPdf(reader, writer);
        } catch (Exception e) {
            throw new ContentIOException("Mso conversion failed: \n" + "   reader: " + reader + "\n" + "   writer: " + writer, e);
        }
    }

    public void setMsoService(MsoService msoService) {
        this.msoService = msoService;
    }

}
