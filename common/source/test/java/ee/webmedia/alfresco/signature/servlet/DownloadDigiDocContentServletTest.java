package ee.webmedia.alfresco.signature.servlet;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.BaseAlfrescoSpringTest;

import ee.webmedia.alfresco.util.ContentCreatorHelper;

public class DownloadDigiDocContentServletTest extends BaseAlfrescoSpringTest {

    private NodeRef nodeRef;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        ContentCreatorHelper contentCreator = new ContentCreatorHelper(applicationContext);
        nodeRef = contentCreator.createTestFile(contentCreator.createTestFolder("testFolder"), "testDownload.txt");
    }

    public void testGenerateUrl() {
        String url = DownloadDigiDocContentServlet.generateUrl(nodeRef, "0", "testname");
        assertTrue(url.startsWith("/ddc/workspace/SpacesStore/"));
        assertTrue(url.endsWith("/0/testname"));
    }
}
