package ee.webmedia.alfresco.postipoiss;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.log.LogHelper;

/**
 * Entry point for starting and stopping whole import. Manages input parameters and coordinates structure and document import.
 * 
 * @author Alar Kvell
 */
public class PostipoissImporter implements SaveListener {
    protected final Log log = LogFactory.getLog(getClass());

    private final AtomicInteger importerRunning = new AtomicInteger(0);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    public boolean isImporterRunning() {
        return importerRunning.get() != 0;
    }

    public boolean isImporterStopping() {
        return isImporterRunning() && stopFlag.get();
    }

    /** @param event */
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
            throw new StopException();
        }
    }

    /** @param event */
    public synchronized void startImporterInBackground(ActionEvent event) throws Exception {
        if (!isImporterRunning()) {
            log.info("startImporterInBackground\n  dataFolders=" + dataFolders + "\n  workFolders=" + workFolders + "\n  mappingsFileNames=" + mappingsFileNames
                    + "\n  publishToAdrWithFilesStartingFromDates=" + publishToAdrWithFilesStartingFromDates + "\n  defaultOwnerIds=" + defaultOwnerIds + "\n  archivalsStores="
                    + archivalsStores + "\n  setPublicFilesToBackgroundFiles=" + publicFilesToBackgroundFiles + "\n  openUnits=" + openUnits);
            LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs = generalService.getArchivalsStoreVOs();
            iterate(archivalsStoreVOs, false);
            iterate(archivalsStoreVOs, true);
        } else {
            log.warn("Importer is already running");
        }
    }

    private void iterate(LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs, boolean execute) throws Exception {
        int countTmp = count;
        int batchSizeTmp = batchSize;
        Assert.isTrue(batchSizeTmp > 0, "batchSize must be a positive integer");
        if (execute) {
            postipoissStructureImporters = new ArrayList<PostipoissStructureImporter>();
            postipoissDocumentsImporters = new ArrayList<PostipoissDocumentsImporter>();
            for (int i = 0; i < countTmp; i++) {
                PostipoissStructureImporter postipoissStructureImporter = new PostipoissStructureImporter(this);
                postipoissStructureImporters.add(postipoissStructureImporter);
                PostipoissDocumentsImporter postipoissDocumentsImporter = new PostipoissDocumentsImporter(this);
                postipoissDocumentsImporters.add(postipoissDocumentsImporter);
            }
            stopFlag.set(false);
        }
        Set<NodeRef> archivalsRoots = new HashSet<NodeRef>();
        for (int i = 0; i < countTmp; i++) {
            if (i >= dataFolders.size() || i >= workFolders.size() || i >= mappingsFileNames.size() || i >= defaultOwnerIds.size()
                    || i >= publishToAdrWithFilesStartingFromDates.size() || i >= archivalsStores.size() || i >= publicFilesToBackgroundFiles.size() || i >= openUnits.size()) {
                if (execute) {
                    log.info("Skipping input arguments group " + (i + 1));
                }
                continue;
            }
            String archivalsStore = archivalsStores.get(i);
            if (StringUtils.isBlank(dataFolders.get(i)) || StringUtils.isBlank(workFolders.get(i)) || StringUtils.isBlank(mappingsFileNames.get(i))
                    || StringUtils.isBlank(defaultOwnerIds.get(i)) || StringUtils.isBlank(archivalsStore)) {
                if (execute) {
                    log.info("Skipping input arguments group " + (i + 1));
                }
                continue;
            }
            File dataFolder = new File(dataFolders.get(i));
            File workFolder = new File(workFolders.get(i));
            File mappingsFile = new File(mappingsFileNames.get(i));

            Assert.isTrue(dataFolder.isDirectory(), "dataFolder " + (i + 1) + " does not exist or is not a folder: " + dataFolder);
            Assert.isTrue(workFolder.isDirectory(), "workFolder " + (i + 1) + " does not exist or is not a folder: " + workFolder);
            Assert.isTrue(mappingsFile.isFile() && mappingsFile.canRead(), "mappingsFile " + (i + 1) + " does not exist or is not a file or is not readable: " + mappingsFile);

            NodeRef archivalsRoot = null;
            for (ArchivalsStoreVO archivalsStoreVO : archivalsStoreVOs) {
                if (archivalsStoreVO.getStoreRef().toString().equals(archivalsStore)) {
                    archivalsRoot = archivalsStoreVO.getNodeRef();
                    break;
                }
            }
            if (archivalsRoot == null && generalService.getStore().toString().equals(archivalsStore)) {
                archivalsRoot = functionsService.getFunctionsRoot();
            }
            Assert.notNull(archivalsRoot, "archivalsStore " + (i + 1) + " does not exist");
            Assert.isTrue(!archivalsRoots.contains(archivalsRoot), "archivalsStore " + (i + 1) + " documentList root nodeRef is already used: " + archivalsRoot);
            archivalsRoots.add(archivalsRoot);

            Date publishToAdrWithFilesStartingFromDate = null;
            if (StringUtils.isNotBlank(publishToAdrWithFilesStartingFromDates.get(i))) {
                DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                dateFormat.setLenient(false);
                publishToAdrWithFilesStartingFromDate = dateFormat.parse(publishToAdrWithFilesStartingFromDates.get(i));
            }

            if (execute) {
                log.info("Executing importer for arguments group " + (i + 1));
                try {
                    PostipoissStructureImporter postipoissStructureImporter = postipoissStructureImporters.get(i);
                    PostipoissDocumentsImporter postipoissDocumentsImporter = postipoissDocumentsImporters.get(i);
                    startImporter(i, batchSizeTmp, dataFolder, workFolder, mappingsFile, archivalsRoot, defaultOwnerIds.get(i),
                            publishToAdrWithFilesStartingFromDate, Boolean.TRUE.equals(publicFilesToBackgroundFiles.get(i)), Boolean.TRUE.equals(openUnits.get(i)),
                            postipoissStructureImporter, postipoissDocumentsImporter);
                } catch (StopException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Importer error for arguments group " + (i + 1) + " (" + archivalsStore + ") : " + e.getMessage(), e);
                }
            }
        }
    }

    private void startImporter(final int i, final int batchSizeTmp, final File dataFolder, final File workFolder, final File mappingsFile, final NodeRef archivalsRoot,
            final String defaultOwnerId, final Date publishToAdrWithFilesStartingFromDate, final boolean publicFileToBackgroundFile, final boolean openUnit,
            final PostipoissStructureImporter postipoissStructureImporter, final PostipoissDocumentsImporter postipoissDocumentsImporter)
            throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int numThreads = importerRunning.incrementAndGet();
                    log.info("Thread started; running threads with current one = " + numThreads);
                    AuthenticationUtil.runAs(new RunAsWork<Void>() {
                        @Override
                        public Void doWork() throws Exception {
                            try {
                                LogHelper.setUserInfo("127.0.0.1", "localhost");
                                postipoissStructureImporter.runImport(dataFolder, workFolder, archivalsRoot, openUnit);
                                postipoissDocumentsImporter
                                        .runImport(dataFolder, workFolder, archivalsRoot, mappingsFile, batchSizeTmp, defaultOwnerId, publishToAdrWithFilesStartingFromDate,
                                                publicFileToBackgroundFile);
                                return null;
                            } catch (StopException e) {
                                log.info("Stop completed");
                            } catch (Exception e) {
                                log.error("Importer error", e);
                            }
                            return null;
                        }
                    }, "IMPORT");
                } finally {
                    int numThreads = importerRunning.decrementAndGet();
                    log.info("Thread stopped; remaining running threads after current one = " + numThreads);
                }
            }
        }, "ImporterThread" + i).start();
    }

    // ========================================================================

    private final int count = 5;
    private List<String> dataFolders;
    private List<String> workFolders;
    private List<String> mappingsFileNames;
    private List<String> defaultOwnerIds;
    private List<String> publishToAdrWithFilesStartingFromDates;
    private List<String> archivalsStores;
    private List<Boolean> publicFilesToBackgroundFiles;
    private List<Boolean> openUnits;
    private int batchSize = 50;
    private boolean seriesComparisonIncludesTitle = false;

    private void init() {
        if (dataFolders == null || workFolders == null || mappingsFileNames == null || defaultOwnerIds == null || publishToAdrWithFilesStartingFromDates == null
                || archivalsStores == null || publicFilesToBackgroundFiles == null
                || openUnits == null) {
            List<String> dataFoldersTmp = new ArrayList<String>();
            List<String> workFoldersTmp = new ArrayList<String>();
            List<String> mappingsFileNamesTmp = new ArrayList<String>();
            List<String> defaultOwnerIdsTmp = new ArrayList<String>();
            List<String> publishToAdrWithFilesStartingFromDatesTmp = new ArrayList<String>();
            List<String> archivalsStoresTmp = new ArrayList<String>();
            List<Boolean> publicFilesToBackgroundFilesTmp = new ArrayList<Boolean>();
            List<Boolean> openUnitsTmp = new ArrayList<Boolean>();

            LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs = generalService.getArchivalsStoreVOs();
            Iterator<ArchivalsStoreVO> it = archivalsStoreVOs.iterator();
            if (it.hasNext()) {
                it.next(); // skip primary archivals store
            }
            for (int i = 0; i < count; i++) {
                String archivalsStore = "";
                if (it.hasNext()) {
                    archivalsStore = it.next().getStoreRef().toString();
                }
                dataFoldersTmp.add("");
                workFoldersTmp.add("");
                mappingsFileNamesTmp.add("");
                defaultOwnerIdsTmp.add("");
                publishToAdrWithFilesStartingFromDatesTmp.add("");
                archivalsStoresTmp.add(archivalsStore);
                publicFilesToBackgroundFilesTmp.add(Boolean.FALSE);
                openUnitsTmp.add(Boolean.FALSE);
            }

            dataFolders = dataFoldersTmp;
            workFolders = workFoldersTmp;
            mappingsFileNames = mappingsFileNamesTmp;
            defaultOwnerIds = defaultOwnerIdsTmp;
            publishToAdrWithFilesStartingFromDates = publishToAdrWithFilesStartingFromDatesTmp;
            archivalsStores = archivalsStoresTmp;
            publicFilesToBackgroundFiles = publicFilesToBackgroundFilesTmp;
            openUnits = openUnitsTmp;
        }
    }

    public List<String> getDataFolders() {
        init();
        return dataFolders;
    }

    public List<String> getWorkFolders() {
        init();
        return workFolders;
    }

    public List<String> getMappingsFileNames() {
        init();
        return mappingsFileNames;
    }

    public List<String> getDefaultOwnerIds() {
        init();
        return defaultOwnerIds;
    }

    public List<String> getPublishToAdrWithFilesStartingFromDates() {
        return publishToAdrWithFilesStartingFromDates;
    }

    public List<String> getArchivalsStores() {
        init();
        return archivalsStores;
    }

    public List<Boolean> getPublicFilesToBackgroundFiles() {
        init();
        return publicFilesToBackgroundFiles;
    }

    public List<Boolean> getOpenUnits() {
        init();
        return openUnits;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isSeriesComparisonIncludesTitle() {
        return seriesComparisonIncludesTitle;
    }

    public void setSeriesComparisonIncludesTitle(boolean seriesComparisonIncludesTitle) {
        this.seriesComparisonIncludesTitle = seriesComparisonIncludesTitle;
    }

    private GeneralService generalService;
    private FunctionsService functionsService;
    private List<PostipoissStructureImporter> postipoissStructureImporters;
    private List<PostipoissDocumentsImporter> postipoissDocumentsImporters;

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    // SaveListener that sets draft=true on document

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
