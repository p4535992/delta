package ee.webmedia.alfresco.signature.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformer2;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.commons.io.IOUtils;
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
            log.trace("Starting the transformation process\nReader: " + reader + "\nWriter: " + writer);
        }
        long startTime = System.currentTimeMillis();
        Writer out = null;
        try {
            InputStream is = reader.getContentInputStream();
            SignatureItemsAndDataItems items = signatureService.getDataItemsAndSignatureItems(is, true, true);
            out = new OutputStreamWriter(writer.getContentOutputStream(), writer.getEncoding());
            for (SignatureItem signatureItem : items.getSignatureItems()) {
                transformSignatureItem(out, signatureItem);
            }
            for (DataItem dataItem : items.getDataItems()) {
                transformDataItem(out, dataItem, writer.getEncoding());
            }
        } catch (SignatureException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception caught, rethrowing as ContentIOException:\n" + e.getMessage());
            }
            throw new ContentIOException("Failed to parse ddoc file", e);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception caught, rethrowing. ", e);
            }
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished DigiDoc transformation, produced " + writer.getSize() + " bytes of text, time " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
        // DDOC -> TEXT
        if (!SignatureService.DIGIDOC_MIMETYPE.equals(sourceMimetype) ||
                !MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(targetMimetype)) {
            return false;
        }
        return true;
    }

    private void transformSignatureItem(Writer out, SignatureItem signatureItem) throws IOException {
        if (signatureItem.getName() != null) {
            out.write(signatureItem.getName());
            out.write('\n');
        }
        if (signatureItem.getLegalCode() != null) {
            out.write(signatureItem.getLegalCode());
            out.write('\n');
        }
        if (signatureItem.getAddress() != null) {
            out.write(signatureItem.getAddress());
            out.write('\n');
        }
    }

    private void transformDataItem(Writer out, DataItem dataItem, String encoding) throws ContentIOException, IOException, SignatureException {
        // add the file's name
        if (dataItem.getName() != null) {
            out.write(dataItem.getName());
            out.write('\n');
        }

        // create the writer
        ContentWriter dataItemWriter = contentService.getTempWriter();
        dataItemWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        dataItemWriter.setEncoding(encoding);

        // create the reader and get the transformer
        ContentReader dataItemReader = getTempContentReader(dataItem);
        ContentTransformer transformer = contentService.getTransformer(dataItemReader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN);
        if (dataItemReader.exists() && transformer != null) {
            try {
                transformer.transform(dataItemReader, dataItemWriter);
                ContentReader r = dataItemWriter.getReader();
                if (r.exists()) {
                    InputStream in = r.getContentInputStream();
                    if (in != null) {
                        try {
                            IOUtils.copy(in, out, r.getEncoding());
                        } finally {
                            in.close();
                        }
                        out.write('\n');
                    }
                }
            } catch (ContentIOException e) {
                log.debug("Transformation failed, ignoring and continuing\n" + dataItem, e);
            }
        }
    }

    private ContentReader getTempContentReader(DataItem dataItem) throws ContentIOException, IOException, SignatureException {
        ContentWriter writer = contentService.getTempWriter();
        writer.setMimetype(dataItem.getMimeType());
        // this is what the analyzers expect on the stream
        writer.setEncoding(dataItem.getEncoding());
        FileCopyUtils.copy(dataItem.getData(), writer.getContentOutputStream()); // closes both streams
        return writer.getReader();
    }

}
