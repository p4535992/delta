package ee.smit.tera;

import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.DigiDoc4JSignatureService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.List;
import org.alfresco.service.cmr.model.FileFolderService;


import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.node.integrity.IntegrityChecker;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;

import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Andmebaasist otse failide küsimiseks SQL
 *
 * SELECT
 public.alf_content_url.content_url, SUBSTRING(public.alf_content_url.content_url, '[0-9]{4}\/[0-9]+\/[0-9]+')
 FROM
 public.alf_content_data
 INNER JOIN public.alf_content_url ON (public.alf_content_data.content_url_id = public.alf_content_url.id)
 WHERE
 public.alf_content_data.id IN
 (SELECT public.alf_node_properties.long_value FROM public.alf_node_properties INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id) WHERE public.alf_node_properties.node_id IN
 (SELECT public.alf_node_properties.node_id FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.ddoc' AND
 public.alf_qname.local_name = 'name') AND public.alf_qname.local_name = 'content')

 Päringud failide tüüpide koguarvu andmete küsimiseks:
 SELECT count(*)
 FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.ddoc' AND
 public.alf_qname.local_name = 'name';

 SELECT count(*)
 FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.bdoc' AND
 public.alf_qname.local_name = 'name';

 SELECT count(*)
 FROM
 public.alf_node_properties
 INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id)
 WHERE
 LOWER(public.alf_node_properties.string_value) LIKE '%.asice' AND
 public.alf_qname.local_name = 'name';

 */
public class TeraProcess implements SaveListener {

    protected final Log log = LogFactory.getLog(getClass());
    //private static Log log = LogFactory.getLog(TeraProcess.class);
    //private static final ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private final AtomicInteger importerRunning = new AtomicInteger(0);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    private NodeService nodeService;
    private NodeRef rootNodeRef;
    private FileFolderService fileFolderService;


    private TransactionService transactionService;

    private DictionaryDAO dictionaryDAO;
    private UserTransaction txn;
    private DigiDoc4JSignatureService digiDoc4JSignatureService;

    public boolean isImporterRunning() {
        return importerRunning.get() != 0;
    }

    public boolean isImporterStopping() {
        return isImporterRunning() && stopFlag.get();
    }

    public synchronized void stopImporter(ActionEvent event) {
        stopFlag.set(true);
        log.info("Stop requested.");
    }

    public static class StopException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StopException() {
            //
        }

    }

    public void checkStop() {
        if (stopFlag.get()) {
            throw new TeraProcess.StopException();
        }
    }

    public synchronized void startInBackground(ActionEvent event) throws Exception {
        log.info("TERA PROCESS STARTED!");
        if (!isImporterRunning()) {
            log.info("Start");
            //LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs = generalService.getArchivalsStoreVOs();
            log.debug("GET NODE SERVICE....");
            this.nodeService = BeanHelper.getNodeService();

            log.debug("GET FILE-FOLDER SERVICE...");
            this.fileFolderService = BeanHelper.getFileFolderService();

            this.digiDoc4JSignatureService = BeanHelper.getDigiDoc4JSignatureService();
            log.debug("getListOfDDOCFiles()... START!");
            getListOfDDOCFiles();
            log.debug("getListOfDDOCFiles()... END!");

        } else {
            log.warn("Importer is already running!");
        }

        log.info("TERA PROCESS ENDED!");
    }

    public void getListOfDDOCFiles(){
        if (!isImporterRunning()) {
            log.debug("Process getListOfDDOCFiles()... START!");
        }


        if(nodeService == null){
            log.error("NODE SERVICE IS NULL! return null...");
            return;
        }

        List<String> namePattern = Arrays.asList("*.bdoc", "*.ddoc");

        log.debug("Filename pattern: " + Arrays.toString(namePattern.toArray()));

        try{
            //setUp();
            List<StoreRef> storeRefsList = nodeService.getStores();

            if(storeRefsList == null){
                log.error("Store REF list is NULL! Break process!");
                return;
            } else {
                log.debug("Find storeRefs: " + storeRefsList.size());
                for(StoreRef storeRef : storeRefsList){
                    log.info("StoreRef.getProtocol: " + storeRef.getProtocol());
                    log.info("StoreRef.getIdentifier: " + storeRef.getIdentifier());
                    if(storeRef.getProtocol().equals("workspace")  || storeRef.getProtocol().equals("archive")){
                        rootNodeRef = nodeService.getRootNode(storeRef);

                        log.info("NodeService.getRootNode: " + rootNodeRef);

                        findAllFiles(rootNodeRef, namePattern);

                    } else {
                        log.info("Not allowed! Not permitted! Get next store");
                    }

                    log.debug("Get next storeRef from list...");
                }
                //SearchParameters sp = new SearchParameters();
                //sp.addStore();

            }

        } catch (Exception ex){
            log.error("getAllDDocFiles ERROR: " + ex.getMessage(), ex);
        }

        log.debug("Process getListOfDDOCFiles()... END!");
    }

    void findAllFiles(NodeRef rootNodeRef, List<String> namePatternList){

        for(String namePattern : namePatternList) {
            log.debug("FILES NAME PATTERN to SEARCH: " + namePattern);
            List<FileInfo> fileIn = fileFolderService.search(rootNodeRef, namePattern, true);
            if(fileIn == null){
                log.warn("Find all files: fileForderService.search returned NULL!");
            } else {
                log.debug("Find files: " + fileIn.size());
                log.debug("============================================================================================");
                for (FileInfo fileInfo : fileIn){
                    String fileName = fileInfo.getName();
                    if(fileInfo.isFolder()){
                        log.warn("DIRECTORY: Name: [" + fileName + "] is FOLDER! continue to next...");
                        continue;
                    }

                    NodeRef fileNodeRef = fileInfo.getNodeRef();

                    log.debug("FILE: NodeRef" + fileNodeRef + ", Name: " + fileName + ", CreatedDate: " + fileInfo.getCreatedDate() + ", ModifiedDate: " + fileInfo.getModifiedDate());

                    ContentData fileContentData = fileInfo.getContentData();
                    if(fileContentData == null){
                        log.error("FILE CONTENTDATA IS NULL!... continue.");
                        continue;
                    }

                    log.debug("FILE contentUrl: " + fileContentData.getContentUrl());
                    log.debug("FILE encoding: " + fileContentData.getEncoding());
                    log.debug("FILE mimetype: " + fileContentData.getMimetype());
                    log.debug("FILE size: " + fileContentData.getSize());

                    SignatureItemsAndDataItems items = getDigidocDataItems(fileNodeRef);

                    if(FilenameUtil.isFileBdoc(fileName)){
                        log.debug("File is BDOC!");
                        boolean needsOverstamping = DigidocFileContainsSHA1(items);
                        if(needsOverstamping){
                            log.warn("CONTAINER CONTAINS SHA-1 ENCRYPTION AND NEEDS TIMESTAMPING (ASIC-S CONTAINER)!");
                        }

                    }

                    if(FilenameUtil.isFileDdoc(fileName)){
                        log.debug("File is DDOC!");
                        boolean needsOverstamping = DigidocFileContainsSHA1(items);
                        if(needsOverstamping){
                            log.warn("CONTAINER CONTAINS SHA-1 ENCRYPTION AND NEEDS TIMESTAMPING (ASIC-S CONTAINER)!");
                            //digiDoc4JSignatureService.createAsicSContainer(fileNodeRef);
                        }

                    }

                    log.debug("----------------------------------------------------------------------------------------");
                }
            }
        }

    }

    private boolean DigidocFileContainsSHA1(SignatureItemsAndDataItems items){
        boolean sha1 = false;
        List<SignatureItem> signatureItems = items.getSignatureItems();

        for(SignatureItem signatureItem : signatureItems){
            String encrytionType = signatureItem.getEncrytionType();
            log.debug("Signature encryption type: " + encrytionType);
            if( encrytionType.startsWith("SHA1")){
                sha1 = true;
                break;
            }
        }

        return sha1;
    }

    private SignatureItemsAndDataItems getDigidocDataItems(NodeRef fileNodeRef) {
        try {
            SignatureItemsAndDataItems items = digiDoc4JSignatureService.getDataItemsAndSignatureItems(fileNodeRef, true);
            return items;
        } catch (SignatureException ex){
            log.error("getDigidocDataItems file nodeRef: " + fileNodeRef + " Exception error: " + ex.getMessage(), ex);
        }
        return null;
    }


    @Override
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
        // do nothing
    }

    @Override
    public void save(DynamicBase document) {
        if (document instanceof DocumentDynamic) {
            ((DocumentDynamic) document).setDraft(true);
            ((DocumentDynamic) document).setDraftOrImapOrDvk(true);
        }
    }

    @Override
    public String getBeanName() {
        return null;
    }

}
