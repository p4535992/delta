/**
 * 
 */
package ee.webmedia.xtee.client.service.impl;

import static ee.webmedia.xtee.client.service.DhlXTeeService.SendStatus.RECEIVED;
import static ee.webmedia.xtee.client.service.DhlXTeeService.SendStatus.SENT;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ee.webmedia.xtee.client.service.DhlXTeeService;
import ee.webmedia.xtee.client.service.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.service.DhlXTeeService.GetDvkOrganizationsHelper;
import ee.webmedia.xtee.client.service.DhlXTeeService.MetainfoHelper;
import ee.webmedia.xtee.client.service.DhlXTeeService.ReceivedDocumentsWrapper;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendStatus;
import ee.webmedia.xtee.client.service.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import ee.webmedia.xtee.client.service.provider.XTeeProviderPropertiesResolver;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.AadressType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.EdastusDocument.Edastus;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.EdastusDocument.Edastus.Staatus.Enum;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.TransportDocument.Transport;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.OccupationType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendStatusResponseTypeUnencoded.Item;
import ee.webmedia.xtee.types.ee.sk.digiDoc.v13.DataFileType;
import ee.webmedia.xtee.types.ee.sk.digiDoc.v13.SignedDocType;

/**
 * @author ats.uiboupin
 */
public class DhlXTeeServiceImplTest extends TestCase {

    private static Log log = LogFactory.getLog(DhlXTeeServiceImplTest.class);
    private static DhlXTeeService dhl;
    private static XTeeProviderPropertiesResolver propertiesResolver;

    private static String SENDER_REG_NR;
    private static List<String> receivedDocumentIds;
    private static List<String> receiveFaileddDocumentIds;
    private static List<String> sentDocIds = new ArrayList<String>();
    private static Map<String, String> dvkOrgList;

    private static final String RECEIVE_OUTPUT_DIR = System.getProperty("java.io.tmpdir");
    private static final String DVK_ORGANIZATIONS_CACHEFILE = RECEIVE_OUTPUT_DIR + "/dvkOrganizationsCache.ser";
    private static final File testFilesToSendFolder = new File("common/source/test/java/ee/webmedia/xtee/client/testFilesToSend");

    private static DhlXTeeService.DvkOrganizationsUpdateStrategy cachePeriodUpdateStrategy // 
    = new DhlXTeeService.DvkOrganizationsCacheingUpdateStrategy().setMaxUpdateInterval(24).setTimeUnit(Calendar.HOUR);
    private static List<String> recipients;
    private boolean EXECUTED_GET_SENDING_OPTIONS;

    protected void setUp() throws Exception {
        super.setUp();
        if (dhl == null) {
            final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("ee/webmedia/xtee/client/service/impl/service-impl-test.xml");
            dhl = (DhlXTeeService) context.getBean("dhlXTeeService");
            propertiesResolver = (XTeeProviderPropertiesResolver) context.getBean("propertiesResolver");
        }
        SENDER_REG_NR = propertiesResolver.getProperty("institution");
        recipients = Arrays.asList(SENDER_REG_NR); // testiks nii saatjale endale kui INTERINX OÜ(10425769)
    }
    
  //shared with alfresco repo testclass: DvkServiceImplTest
    public static List<String> getRecipients(){
        SENDER_REG_NR = "10391131";
        recipients = Arrays.asList(SENDER_REG_NR); // testiks nii saatjale endale kui INTERINX OÜ(10425769)
//      recipients = Arrays.asList(
//              SENDER_REG_NR // Saatjale endale 
//              , "44000122"  // Tallinna Ülikooli Ajaloo Instituut
//              , "64000122"  // Tallinna Ülikooli Infoteaduse Instituut
//              , "54000122"  // Tallinna Ülikooli Psühholoogia Instituut
//      ); // Tallinna Ülikooli Ajaloo Instituut
        return recipients;
    }

    public void testWarmUp() {
        log.debug("warmup done to get better time measure for the first test");
    }

    public void _testRunSystemCheck() {
        dhl.runSystemCheck();
    }

    public void testGetSendingOptions() {
        dvkOrgList = dhl.getSendingOptions();
        writeCacheToFile(dvkOrgList, Calendar.getInstance());
        log.debug("got "+dvkOrgList.size()+" organizations:");
        assertTrue(dvkOrgList.size() > 0);
        for (String regNr : dvkOrgList.keySet()) {
            log.debug("\tregNr: "+regNr+"\t- "+dvkOrgList.get(regNr));
        }
        EXECUTED_GET_SENDING_OPTIONS = true;
    }

    public void testGetDvkOrganizationsHelper() {
        log.debug("testGetDvkOrganizationsHelper dhl=" + dhl);
        GetDvkOrganizationsHelper dvkOrganizationsHelper = dhl.getDvkOrganizationsHelper();
        dvkOrganizationsHelper.setUpdateStrategy(cachePeriodUpdateStrategy);
        final Object[] cacheAndTime = readCacheFromFile();
        if (cacheAndTime != null) {
            @SuppressWarnings("unchecked")
            final Map<String, String> cacheFromFile = (Map<String, String>) cacheAndTime[0];
            dvkOrganizationsHelper.setDvkOrganizationsCache(cacheFromFile);
            cachePeriodUpdateStrategy.setLastUpdated((Calendar) cacheAndTime[1]);
        }
        // dvkOrganizationsHelper.setUpdateStrategy(neverUpdateStrategy);
        Map<String, String> dvkOrganizationsCache = dvkOrganizationsHelper.getDvkOrganizationsCache();
        assertTrue(dvkOrganizationsCache.size() > 0);
        if (EXECUTED_GET_SENDING_OPTIONS) { // if executed getSendingOptions service call, lets compare results
            assertEquals(dvkOrgList.size(), dvkOrganizationsCache.size());
            for (String regNr : dvkOrgList.keySet()) {
                String orgName = dvkOrgList.get(regNr);
                String cachedOrgName = dvkOrganizationsCache.get(regNr);
                assertNotNull(orgName);
                assertNotNull(cachedOrgName);
                assertTrue(orgName.equalsIgnoreCase(cachedOrgName));
            }
            String testRegNr = dvkOrgList.keySet().iterator().next();
            String testOrgName = dvkOrgList.get(testRegNr);
            String cachedOrgName = dvkOrganizationsHelper.getOrganizationName(testRegNr);
            assertEquals(cachedOrgName, testOrgName);
        }
    }

    public void testSendDocuments() {
        final Set<ContentToSend> contentsToSend = getContentsToSend();
        AadressType[] recipientsArray = new AadressType[recipients.size()];
        assertTrue(recipients.size() > 0);
        for (int i = 0; i < recipientsArray.length; i++) {
            recipientsArray[i] = getRecipient(recipients.get(i));
        }
        sentDocIds = dhl.sendDocuments(contentsToSend, recipientsArray, getSenderAddress(), null, null);
        assertTrue(sentDocIds.size() > 0);
        for (String dhlId : sentDocIds) {
            log.debug("\tdocument sent to DVK, dhlId=" + dhlId);
            assertTrue(StringUtils.isNotBlank(dhlId));
        }
    }

    public void testGetSendStatus1() {
        final List<Item> items = dhl.getSendStatuses(sentDocIds);
        log.debug("got " + items.size() + " items with send statuses:");
        assertTrue(items.size() > 0 || sentDocIds.size() == 0);
        System.out.println(items.size());
        Assert.assertTrue(items != null && items.size() > 0);
        for (Item item : items) {
            log.debug("\titem=" + item);
            String dhlId = item.getDhlId();
            String olek = item.getOlek();
            Assert.assertNotNull(dhlId);
            Assert.assertNotNull(olek);
            log.debug("\tdhlId=" + dhlId);
            log.debug("\tolek=" + olek);
            assertTrue(SendStatus.get(olek).equals(SENT));
            List<Edastus> edastusList = item.getEdastusList();
            log.debug("\tedastusList " + edastusList.size());
            Assert.assertTrue(edastusList.size() > 0);
            for (Edastus edastus : edastusList) {
                log.debug("\t\tedastus=" + edastus);

                Calendar saadud = edastus.getSaadud();
                ee.webmedia.xtee.types.ee.riik.schemas.dhl.EdastusDocument.Edastus.Meetod.Enum meetod = edastus.getMeetod();
                Calendar loetud = edastus.getLoetud();
                Enum staatus = edastus.getStaatus();
                BigInteger vStaatus = edastus.getVastuvotjaStaatusId();

                log.debug("\t\tsaadud=" + saadud);
                log.debug("\t\t\tmeetod=" + meetod);
                log.debug("\t\tloetud=" + loetud);
                log.debug("\t\tstaatus=" + staatus);
                assertTrue(SendStatus.get(staatus).equals(SENT));

                log.debug("\t\tvStaatus=" + vStaatus);
                Assert.assertNotNull(vStaatus);
                final AadressType saaja = edastus.getSaaja();

                final String regnr = saaja.getRegnr();
                final String asutuseNimi = saaja.getAsutuseNimi();
                log.debug("\t\tsaaja regnr=" + regnr);
                log.debug("\t\tsaaja asutuseNimi=" + asutuseNimi);
            }
        }
    }

    /**
     * Test method for {@link ee.webmedia.xtee.client.service.impl.DhlXTeeServiceImpl#receiveDocuments()
     */
    public void testReceiveDocuments() {
        receivedDocumentIds = new ArrayList<String>(); // using static field to be able to use the result in other tests
        receiveFaileddDocumentIds = new ArrayList<String>(); // using static field to be able to use the result in other tests
        // List<DhlDokumentType> receivedDocuments = dhl.receiveDocuments();
        final ReceivedDocumentsWrapper receiveDocuments = dhl.receiveDocuments(300);
        assertTrue(receiveDocuments.size() > 0 || sentDocIds.size() == 0);
        for (String dhlId : receiveDocuments) {
            final ReceivedDocument receivedDocument = receiveDocuments.get(dhlId);
            final MetainfoHelper metaInfoHelper = receivedDocument.getMetaInfoHelper();
            final DhlDokumentType dhlDokument = receivedDocument.getDhlDocument();
            final SignedDocType signedDoc = receivedDocument.getSignedDoc();
            // ee.webmedia.xtee.types.ee.riik.schemas.dhl.TransportDocument.Transport transport = receivedDocument.getTransport();
            log.debug("dokument element=" + dhlDokument + "'");
            // Metainfo metainfo = dhlDokument.getMetainfo();
            // MetainfoHelper metaInfoHelper = new MetainfoHelper(metainfo);
            // String dhlId = metaInfoHelper.getDhlId();
            log.debug("helper.getObject(DhlIdDocumentImpl)=" + dhlId + " " + metaInfoHelper.getDhlSaatjaAsutuseNimi() + " "
                    + metaInfoHelper.getDhlSaatjaAsutuseNr() + " saadeti: " + metaInfoHelper.getDhlSaatmisAeg() + " saabus: "
                    + metaInfoHelper.getDhlSaabumisAeg());
            assertTrue(StringUtils.isNotBlank(dhlId));
            // SignedDocType signedDoc = dhlDokument.getSignedDoc();
            Transport transport = dhlDokument.getTransport();
            AadressType saatja = transport.getSaatja();
            assertTrue(saatja != null && StringUtils.isNotBlank(saatja.getRegnr()));
            log.debug("sender: " + saatja.getRegnr() + " : " + saatja.getAsutuseNimi());
            final List<AadressType> recipients = transport.getSaajaList();
            log.debug("document was sent to " + recipients.size() + " recipients:");
            assertTrue(recipients.size() > 0);
            for (AadressType recipient : recipients) {
                String regnr = recipient.getRegnr();
                log.debug("\trecipient:" + regnr + ": " + recipient.getAsutuseNimi());
                assertTrue(StringUtils.isNotBlank(regnr));
            }
            try {
                List<DataFileType> dataFileList = signedDoc.getDataFileList();
                log.debug("document contain " + dataFileList.size() + " datafiles: " + dataFileList);
                for (DataFileType dataFile : dataFileList) {
                    try {
                        File outFile = File.createTempFile("DVK_" + dhlId + "_" + dataFile.getId() + "_", dataFile.getFilename());
                        log.debug("writing file '" + dataFile.getFilename() + "' from dvk document with dhlId '" + dataFile.getId() + "'  to file '"
                                + outFile.getAbsolutePath() + "'");
                        OutputStream os = new FileOutputStream(outFile);
                        os.write(Base64.decode(dataFile.getStringValue()));
                        os.close();
                        outFile.delete();
                        receivedDocumentIds.add(dhlId);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException("", e);
                    } catch (Base64DecodingException e) {
                        throw new RuntimeException("Failed to decode", e);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed write output to temporary file ", e);
                    }
                }
            } catch (Exception e) {
                log.error("signedDoc=" + signedDoc + ", dhlDokument=" + dhlDokument + ", can't get dataFileList from dhlDokument:\n" + dhlDokument + "\n\n", e);
                receiveFaileddDocumentIds.add(dhlId);
                continue;
            }
        }
        log.debug("received " + receivedDocumentIds.size() + " documents: " + receivedDocumentIds);
        log.debug("FAILED to receive \" + receiveFaileddDocumentIds.size() + \" documents: " + receiveFaileddDocumentIds);
        assertTrue(sentDocIds == null || receivedDocumentIds.containsAll(sentDocIds));
        for (String dhlId : receiveFaileddDocumentIds) {
            assertFalse(sentDocIds.contains(dhlId));
        }
    }

    public void testMarkDocumentsReceived() {
        log.debug("receivedDocumentIds=" + receivedDocumentIds);
        dhl.markDocumentsReceived(receivedDocumentIds);
    }

    /**
     * Test method for {@link ee.webmedia.xtee.dvk.service.impl.DvkServiceImpl#getSendStatuses(String)
     */
    public void testGetSendStatus2() {
        final List<Item> items = dhl.getSendStatuses(sentDocIds);
        assertTrue(items.size() > 0 || sentDocIds.size() == 0);
        System.out.println(items.size());
        Assert.assertTrue(items != null && items.size() > 0);
        for (Item item : items) {
            log.debug("--item=" + item);
            String dhlId = item.getDhlId();
            String olek = item.getOlek();
            Assert.assertNotNull(dhlId);
            Assert.assertNotNull(olek);
            log.debug("--dhlId=" + dhlId);
            log.debug("--olek=" + olek);
            List<Edastus> edastusList = item.getEdastusList();
            log.debug("--edastusList " + edastusList.size());
            Assert.assertTrue(edastusList != null && edastusList.size() > 0);
            boolean oneNotReceived = false;
            for (Edastus edastus : edastusList) {
                // edastus = (Edastus)XmlObject.Factory.parse(edastus.toString(), new XmlOptions().setDocumentType(EdastusDocument.Edastus.type));
                log.debug("\tedastus=" + edastus);

                Calendar saadud = edastus.getSaadud();
                ee.webmedia.xtee.types.ee.riik.schemas.dhl.EdastusDocument.Edastus.Meetod.Enum meetod = edastus.getMeetod();
                Calendar loetud = edastus.getLoetud();
                Enum staatus = edastus.getStaatus();
                BigInteger vStaatus = edastus.getVastuvotjaStaatusId();

                log.debug("\tsaadud=" + saadud);
                log.debug("\tmeetod=" + meetod);
                log.debug("\tloetud=" + loetud);
                log.debug("\tstaatus=" + staatus);
                log.debug("\tvStaatus=" + vStaatus);
                Assert.assertNotNull(vStaatus);
                final AadressType saaja = edastus.getSaaja();

                final String regnr = saaja.getRegnr();
                final String asutuseNimi = saaja.getAsutuseNimi();
                if (regnr.equalsIgnoreCase(SENDER_REG_NR)) {
                    Assert.assertTrue(SendStatus.get(staatus).equals(RECEIVED));
                }
                if (SendStatus.get(staatus).equals(SENT)) {
                    oneNotReceived = true;
                }
                log.debug("\tsaaja regnr=" + regnr);
                log.debug("\tsaaja asutuseNimi=" + asutuseNimi);
            }
            assertTrue(oneNotReceived ? SendStatus.get(olek).equals(SENT) : SendStatus.get(olek).equals(RECEIVED)); // kui üle 1 saaja, siis ilmselt pole
            // saadetud
        }
    }

    public void testGetOccupationList() {
        List<OccupationType> responseList = dhl.getOccupationList(Arrays.asList(SENDER_REG_NR));
        log.debug("Occupations list contains " + responseList.size() + " occupations:");
        assertTrue(responseList.size() > 0);
        for (OccupationType institutionType : responseList) {
            log.debug("\t" + institutionType.getAsutuseKood() + ":\t" + institutionType.getNimetus() + "\t " + institutionType.getKood());
            assertTrue(StringUtils.isNotBlank(institutionType.getAsutuseKood()));
        }
    }

    private AadressType getRecipient(String regNr) {
        AadressType recipient = AadressType.Factory.newInstance();
        recipient.setRegnr(regNr);
        // recipient.setAsutuseNimi(recipientName); //seatakse DVK'st saadud reg-numbri järgi DvkServiceImpl.constructDokumentDocument()
        log.debug("recipient: " + ToStringBuilder.reflectionToString(recipient) + "'");
        return recipient;
    }

    private AadressType getSenderAddress() {
        AadressType sender = AadressType.Factory.newInstance();
        sender.setRegnr(SENDER_REG_NR);
        sender.setAsutuseNimi(propertiesResolver.getProperty("institution_name"));
        log.debug("Sender: " + ToStringBuilder.reflectionToString(sender) + "'");
        return sender;
    }

    /**
     * 
     * @return content to send
     */
    public static Set<ContentToSend> getContentsToSend() {
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
            String mimeTypeTextPlain = "text/plain";
            content1.setMimeType(mimeTypeTextPlain);

            content2.setFileName("test2.txt");
            content2.setInputStream(new ByteArrayInputStream(bos2.toByteArray()));
            content2.setMimeType(mimeTypeTextPlain);
            
            
            contentsToSend.add(content1);
            contentsToSend.add(content2);
            contentsToSend.add(getContentFromTestFile("document1.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            contentsToSend.add(getContentFromTestFile("tekstifail-iso-8859-1.txt", mimeTypeTextPlain));
            contentsToSend.add(getContentFromTestFile("tekstifail-utf-8.txt", mimeTypeTextPlain));
            contentsToSend.add(getContentFromTestFile("digidocSigned.ddoc", "application/digidoc"));
            return contentsToSend;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test content to be sent to DVK", e);
        }
    }

    private static ContentToSend getContentFromTestFile(String fileName, String mimeType) throws FileNotFoundException {
        final ContentToSend content3 = new ContentToSend();
        File fileToSend = new File(testFilesToSendFolder, fileName);
        content3.setInputStream(new FileInputStream(fileToSend));
        content3.setFileName(fileName);
        content3.setMimeType(mimeType);
        return content3;
    }
    
    

    private void writeCacheToFile(Map<String, String> dvkOrganizationsCache, Calendar updateTime) {
        try {
            File serFile = new File(DVK_ORGANIZATIONS_CACHEFILE);
            if (!serFile.exists()) {
                serFile.createNewFile();
            }
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serFile));
            oos.writeObject(updateTime);
            oos.writeObject(dvkOrganizationsCache);
            IOUtils.closeQuietly(oos);
            log.debug("wrote data to "+serFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("failed to serialize organizations cache to file", e);
        }
    }

    private Object[] readCacheFromFile() {
        File serFile = new File(DVK_ORGANIZATIONS_CACHEFILE);
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serFile));
            Calendar cal = (Calendar) ois.readObject();
            @SuppressWarnings("unchecked")
            Map<String, String> dvkOrganizationsCache = (Map<String, String>) ois.readObject();
            return new Object[] { dvkOrganizationsCache, cal };
        } catch (FileNotFoundException e) {
            log.debug("Didn't find serialized cache file");
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error reading in cache file", e);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
