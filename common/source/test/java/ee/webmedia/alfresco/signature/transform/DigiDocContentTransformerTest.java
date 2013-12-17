package ee.webmedia.alfresco.signature.transform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.repo.content.transform.AbstractContentTransformerTest;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.signature.service.SignatureService;

public class DigiDocContentTransformerTest extends AbstractContentTransformerTest {

    public static final String TEST_RESOURCE_PATH = "build/test/classes/ee/webmedia/alfresco/signature";
    public static final String TEST_DIGIDOC = "digidocTestEnvironment.ddoc";

    private DigiDocContentTransformer transformer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        transformer = (DigiDocContentTransformer) ctx.getBean("transformer.DigiDocContent");
    }

    @Override
    protected ContentTransformer getTransformer(String sourceMimetype, String targetMimetype) {
        return transformer;
    }

    @Override
    public void testSetUp() throws Exception {
        assertNotNull(transformer);
    }

    public void testIsTransformable() throws Exception {
        assertTrue(transformer.isTransformable(SignatureService.DIGIDOC_MIMETYPE, MimetypeMap.MIMETYPE_TEXT_PLAIN, new TransformationOptions()));
        assertFalse(transformer.isTransformable(MimetypeMap.MIMETYPE_TEXT_PLAIN, SignatureService.DIGIDOC_MIMETYPE, new TransformationOptions()));
    }

    public void testDigiDocToText() throws Exception {
        ContentReader contentReader = new FileContentReader(new File(TEST_RESOURCE_PATH, TEST_DIGIDOC));
        contentReader.setMimetype(SignatureService.DIGIDOC_MIMETYPE);
        contentReader.setEncoding(AppConstants.CHARSET);

        File output = File.createTempFile("digiDocTest", ".txt");
        ContentWriter contentWriter = new FileContentWriter(output);
        contentWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        contentWriter.setEncoding(AppConstants.CHARSET);

        transformer.transform(contentReader, contentWriter);

        StringBuilder sb = new StringBuilder();
        String line = null;
        BufferedReader reader = new BufferedReader(new FileReader(output));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        output.delete();
        // System.out.println(sb.toString());
        assertTrue(sb.indexOf("DMITRI") != -1);
        assertTrue(sb.indexOf("digidocTestEnvironment.txt") != -1);
        assertTrue(sb.indexOf("plain text") != -1);
    }

}
