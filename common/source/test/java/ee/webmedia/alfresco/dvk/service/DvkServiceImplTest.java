package ee.webmedia.alfresco.dvk.service;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.apache.commons.io.output.ByteArrayOutputStream;

import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocumentsImpl;
import ee.webmedia.alfresco.util.ContentCreatorHelper;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.DhlXTeeServiceImplTest;

/**
 * @author Ats Uiboupin
 */
public class DvkServiceImplTest extends BaseAlfrescoSpringTest {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DvkServiceImplTest.class);
    public static final String TEST_FILE_1 = "testFile1.txt";

    private FileFolderService fileFolderService;
    private DvkService dvkService;
    // private SendOutService sendOutService; //TODO: SendOutService

    private NodeRef sendableDocFolderNodeRef;
    private NodeRef sendableDocFileNodeRef;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        ContentCreatorHelper contentCreator = new ContentCreatorHelper(applicationContext);
        // Create test folder and DigiDoc NodeRef
        sendableDocFolderNodeRef = contentCreator.createTestFolder("testOutputDocumentSpace");
        sendableDocFileNodeRef = contentCreator.createTestFileWithContentUtf8(sendableDocFolderNodeRef, TEST_FILE_1);
    }

    public void testWarmUp() {
        log.debug("warmup done to get better time measure for the first test");
    }

    public final void testSendDocuments() {
        // Collection<ContentToSend> contentsToSend = getContentsToSend();
        Collection<ContentToSend> contentsToSend = getContentsToSend();
        final DvkSendLetterDocuments sendDocument = getSendDocument();
        final String dvkId = dvkService.sendLetterDocuments(sendableDocFolderNodeRef, contentsToSend, sendDocument);
        log.debug("Sent document id's:" + dvkId);
        // TODO: SendOutService
        // final List<SendOutItem> sendOutItems = sendOutService.getSendOut(sendableDocFolderNodeRef);
        // Assert.assertEquals(sendDocument.getRecipientsRegNrs().size(), sendOutItems.size());
        // for (SendOutItem sendOutItem : sendOutItems) {
        // log.debug("sendOutItem: " + ToStringBuilder.reflectionToString(sendOutItem, ToStringStyle.MULTI_LINE_STYLE) + "'");
        // }
    }

    private static DvkSendLetterDocuments getSendDocument() {
        DvkSendLetterDocuments sd = new DvkSendLetterDocumentsImpl();
        sd.setSenderOrgName("Test Org");
        sd.setSenderRegNr("10391131");
        sd.setSenderEmail("test@alfresco.wm.komm");
        sd.setLetterSenderDocSignDate(new Date());
        sd.setLetterSenderDocNr("bla-bla number/13");

        List<String> recipients = DhlXTeeServiceImplTest.getRecipients();
        sd.setRecipientsRegNrs(recipients);
        // sd.setRecipientsRegNrs(Arrays.asList("10391131", "10425769"));
        // sd.setRecipientsRegNrs(Arrays.asList("10391131"));

        sd.setLetterSenderTitle("Tähtis dokument - testimiseks");
        sd.setDocType("someType1");
        sd.setLetterCompilatorFirstname("Saatja-enimi");
        sd.setLetterCompilatorSurname("Saatja-pnimi");
        sd.setLetterCompilatorJobTitle("Doc-haldur");
        // access
        sd.setLetterAccessRestriction("letterAccessRestriction1");
        final Calendar cal1 = Calendar.getInstance();
        cal1.set(1918, 10, 3, 4, 5);
        sd.setLetterAccessRestrictionBeginDate(cal1.getTime());
        final Calendar cal2 = Calendar.getInstance();
        cal2.set(2945, 8, 2, 7, 8);
        sd.setLetterAccessRestrictionEndDate(cal2.getTime());
        sd.setLetterAccessRestrictionReason("no particular reason");

        return sd;
    }

    private Set<ContentToSend> getContentsToSend() {
        final Set<ContentToSend> contentsToSend = DhlXTeeServiceImplTest.getContentsToSend();
        final ContentToSend content1 = new ContentToSend();
        final ContentReader reader = fileFolderService.getReader(sendableDocFileNodeRef);
        final ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        reader.getContent(bos1);

        content1.setFileName("test1.txt");
        content1.setInputStream(new ByteArrayInputStream(bos1.toByteArray()));
        content1.setMimeType("text/plain");

        contentsToSend.add(content1);
        contentsToSend.addAll(getContentsToSend2());
        return contentsToSend;
    }

    private Set<ContentToSend> getContentsToSend2() {
        final HashSet<ContentToSend> contentsToSend = new HashSet<ContentToSend>();
        try {
            final ContentToSend content1 = new ContentToSend();
            final ContentToSend content2 = new ContentToSend();

            final ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
            final ByteArrayOutputStream bos2 = new ByteArrayOutputStream();

            BufferedWriter out1 = new BufferedWriter(new OutputStreamWriter(bos1));
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(bos2));

            DateFormat df = new SimpleDateFormat("d HH:mm:ss");
            out1.write("testfile1 " + df.format(Calendar.getInstance().getTime()));
            out1.close();
            out2.write("testfile2 žõäöüš");
            out2.close();

            content1.setFileName("test1.txt");
            content1.setInputStream(new ByteArrayInputStream(bos1.toByteArray()));
            content1.setMimeType("text/plain");

            content2.setFileName("test2.txt");
            content2.setInputStream(new ByteArrayInputStream(bos2.toByteArray()));
            content2.setMimeType("text/plain");

            contentsToSend.add(content1);
            contentsToSend.add(content2);
            return contentsToSend;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test content to be sent to DVK", e);
        }
    }

    public void setDvkService(DvkService dvkService) {
        this.dvkService = dvkService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    // TODO: SendOutService
    // public void setSendOutService(SendOutService sendOutService) {
    // this.sendOutService = sendOutService;
    // }

}
