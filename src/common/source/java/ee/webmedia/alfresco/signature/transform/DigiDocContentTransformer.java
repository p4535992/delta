package ee.webmedia.alfresco.signature.transform;

import java.io.IOException;
import java.io.InputStream;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformer2;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.log4j.Logger;
import org.springframework.util.FileCopyUtils;

import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;

/**
 * Tries to transform DigiDoc contents into plain text.
 * Reads signature and data file info. Applies available transformations to the contained files.
 * For every file inside the container, 2 temporary files are created (for writing and reading).
 */
public class DigiDocContentTransformer extends AbstractContentTransformer2 {

    private static final Logger log = Logger.getLogger(DigiDocContentTransformer.class);

    private ContentService contentService;
    private SignatureService signatureService;

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setSignatureService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @Override
    protected void transformInternal(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Starting the transformation process.");
        }
        InputStream is = null;
        try {
            StringBuilder text = new StringBuilder();
            is = reader.getContentInputStream();
            SignatureItemsAndDataItems items = signatureService.getDataItemsAndSignatureItems(is, true);
            for (SignatureItem signatureItem : items.getSignatureItems()) {
                transformSignatureItem(text, signatureItem);
            }
            for (DataItem dataItem : items.getDataItems()) {
                transformDataItem(text, dataItem);
            }
            // add the data into the original writer
            writer.putContent(text.toString());
            if (log.isDebugEnabled()) {
                log.debug("Finished transformation, produced " + text.length() + " characters of text");
            }
            if (log.isTraceEnabled()) {
                log.trace("Index data:\n" + text.toString());
            }
        } catch (SignatureException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception caught, rethrowing as ContentIOException");
            }
            throw new ContentIOException("Failed to parse ddoc file", e);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception caught, rethrowing. ", e);
            }
            throw e;
        }
    }

    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
        // DDOC -> TEXT
        if (!SignatureService.DIGIDOC_MIMETYPE.equals(sourceMimetype) ||
                !MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(targetMimetype)) {
            return false;
        } else {
            return true;
        }
    }

    private void transformSignatureItem(StringBuilder text, SignatureItem signatureItem) {
        if (signatureItem.getName() != null) {
            text.append(signatureItem.getName() + "\n");
        }
        if (signatureItem.getLegalCode() != null) {
            text.append(signatureItem.getLegalCode() + "\n");
        }
        if (signatureItem.getAddress() != null) {
            text.append(signatureItem.getAddress() + "\n");
        }
    }

    private void transformDataItem(StringBuilder text, DataItem dataItem) throws ContentIOException, IOException {
        // add the file's name
        if (dataItem.getName() != null) {
            text.append(dataItem.getName() + "\n");
        }

        // create the writer
        ContentWriter dataItemWriter = contentService.getTempWriter();
        dataItemWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        dataItemWriter.setEncoding("UTF-8");

        // create the reader and get the transformer
        ContentReader dataItemReader = getTempContentReader(dataItem);
        ContentTransformer transformer = contentService.getTransformer(dataItemReader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN);
        if (dataItemReader.exists() && transformer != null) {
            transformer.transform(dataItemReader, dataItemWriter);
            ContentReader r = dataItemWriter.getReader();
            if (r.exists()) {
                String content = r.getContentString();
                if (content != null) {
                    text.append(content + "\n");
                }
            }
        }
    }

    private ContentReader getTempContentReader(DataItem dataItem) throws ContentIOException, IOException {
        ContentWriter writer = contentService.getTempWriter();
        writer.setMimetype(dataItem.getMimeType());
        // this is what the analyzers expect on the stream
        writer.setEncoding(dataItem.getEncoding());
        FileCopyUtils.copy(dataItem.getData(), writer.getContentOutputStream());
        return writer.getReader();
    }

}
