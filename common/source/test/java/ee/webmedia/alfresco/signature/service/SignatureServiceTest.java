package ee.webmedia.alfresco.signature.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.springframework.core.io.ClassPathResource;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.util.ContentCreatorHelper;

public class SignatureServiceTest extends BaseAlfrescoSpringTest {

    public static final String TEST_RESOURCE_PATH = "ee/webmedia/alfresco/signature";
    public static final String TEST_DIGIDOC = "digidocTestEnvironment.ddoc";
    public static final String TEST_CERTHEX = "cert.hex";

    public static final String TEST_ENCODING1 = AppConstants.CHARSET;
    public static final String TEST_DATA1 = "abcdef";

    private SignatureService signatureService;
    private DigiDoc4JSignatureService digiDoc4JSignatureService;

    private NodeRef folderNodeRef;
    private NodeRef nodeRef;
    private List<NodeRef> nodeRefList;

    private String certHex;

    @Override
    protected void onSetUpInTransaction()
            throws Exception {
        super.onSetUpInTransaction();
        signatureService = (SignatureService) applicationContext.getBean("signatureService");
        digiDoc4JSignatureService = (DigiDoc4JSignatureService) applicationContext.getBean(DigiDoc4JSignatureService.BEAN_NAME);
        ContentCreatorHelper contentCreator = new ContentCreatorHelper(applicationContext);

        // Create test folder and DigiDoc NodeRef
        folderNodeRef = contentCreator.createTestFolder("testFolder");
        nodeRef = contentCreator.createTestFile(folderNodeRef, TEST_DIGIDOC);

        // List of nodeRefs
        nodeRefList = new ArrayList<NodeRef>();
        nodeRefList.add(contentCreator.createTestFile(folderNodeRef, "testFile1.txt"));

        contentCreator.writeTestContent(nodeRefList.get(0), TEST_ENCODING1, MimetypeMap.MIMETYPE_TEXT_PLAIN, TEST_DATA1);
        /*
         * // Get the SignedDoc
         * InputStream fileInputStream = ContentCreatorHelper.getClassPathResourceInputStream(TEST_RESOURCE_PATH + "/" + TEST_DIGIDOC);
         * DigiDocFactory digiDocFactory = new SAXDigiDocFactory();
         * SignedDoc signedDoc = digiDocFactory.readSignedDoc(fileInputStream);
         * fileInputStream.close();
         * // Write DigiDoc to nodeRef
         * ContentWriter writer = contentCreator.getContentWriter(nodeRef, AppConstants.CHARSET, SignatureService.DIGIDOC_MIMETYPE);
         * OutputStream os = writer.getContentOutputStream();
         * signedDoc.writeToStream(os);
         * os.close();
         */
        // get the cert, should be less than 2048 bytes
        certHex = ContentCreatorHelper.getClassPathResource(TEST_RESOURCE_PATH + "/" + TEST_CERTHEX);
    }

    public void testIsDigiDoc() {
        assertTrue(digiDoc4JSignatureService.isDigiDocContainer(nodeRef));
        assertFalse(digiDoc4JSignatureService.isDigiDocContainer(folderNodeRef));
        assertFalse(digiDoc4JSignatureService.isDigiDocContainer(nodeRefList.get(0)));
    }

    public void testGetSignatureDigestFromNodeRef() throws Exception {
        SignatureDigest digest = digiDoc4JSignatureService.getSignatureDigest(nodeRef, certHex);
        assertNotNull(digest);
        assertNotNull(digest.getDigestHex());
        assertEquals(40, digest.getDigestHex().length());
    }

    public void testGetSignatureDigestFromNodeRefList() throws Exception {
        SignatureDigest digest = digiDoc4JSignatureService.getSignatureDigest(nodeRefList, certHex);
        assertNotNull(digest);
        assertNotNull(digest.getDigestHex());
        assertEquals(40, digest.getDigestHex().length());
    }

    public void testGetDataItemsAndSignatureItems() throws Exception {
        SignatureItemsAndDataItems items = digiDoc4JSignatureService.getDataItemsAndSignatureItems(nodeRef, true);
        assertTrue(items.getDataItems().size() >= 1);
        assertTrue(items.getSignatureItems().size() >= 0);
        assertNotNull(items.getDataItems().get(0).getData());
    }

    public void testGetDataItemsAndSignatureItemsFromStream() throws Exception {
        InputStream fileInputStream = new ClassPathResource(TEST_RESOURCE_PATH + "/" + TEST_DIGIDOC).getInputStream();
        SignatureItemsAndDataItems items = digiDoc4JSignatureService.getDataItemsAndSignatureItems(fileInputStream, true);
        assertTrue(items.getDataItems().size() >= 1);
        assertTrue(items.getSignatureItems().size() >= 0);
        assertNotNull(items.getDataItems().get(0).getData());
    }

    // signatureHex must come from the sign applet
    // public void testCreateContainer() {
    // SignatureDigest sd = signatureService.getSignatureDigest(nodeRefList, certHex);
    // createContainer(folderNodeRef, nodeRefList, "testContainer.ddoc", sd, signatureHex);
    // }

    // signatureHex must come from the sign applet
    // public void testAddSignature() {
    // SignatureDigest sd = signatureService.getSignatureDigest(nodeRefList, certHex);
    // signatureService.addSignature(nodeRef, sd, signatureHex);
    // }

}
