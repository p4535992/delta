package ee.webmedia.alfresco.postipoiss;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Entry point for starting and stopping whole import. Manage input parameters and coordinate structure and document import.
 * 
 * @author Alar Kvell
 */
public class PostipoissImporter implements InitializingBean {
    protected final Log log = LogFactory.getLog(getClass());

    private final AtomicBoolean importerRunning = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    public boolean isImporterRunning() {
        return importerRunning.get();
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
    public synchronized void startImporterInBackground(ActionEvent event) {
        log.info("startImporterInBackground\n  dataFolders=" + dataFolders + "\n  workFolders=" + workFolders + "\n  mappingsFileNames=" + mappingsFileNames
                + "\n  archivalsStores=" + archivalsStores);
        if (!isImporterRunning()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.info("Main thread started");
                        importerRunning.set(true);
                        stopFlag.set(false);
                        AuthenticationUtil.runAs(new RunAsWork<Void>() {
                            @Override
                            public Void doWork() throws Exception {
                                try {
                                    startImporter();
                                    return null;
                                } catch (StopException e) {
                                    log.info("Stop requested");
                                } catch (Exception e) {
                                    log.error("Importer error", e);
                                }
                                return null;
                            }
                        }, AuthenticationUtil.getSystemUserName());
                    } finally {
                        importerRunning.set(false);
                        log.info("Main thread stopped");
                    }
                }
            }, "ImporterThread").start();
        } else {
            log.warn("Importer is already running");
        }
    }

    private void startImporter() throws Exception {
        LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs = generalService.getArchivalsStoreVOs();
        iterate(archivalsStoreVOs, false);
        iterate(archivalsStoreVOs, true);
    }

    private void iterate(LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs, boolean execute) throws Exception {
        int batchSizeTmp = batchSize;
        Assert.isTrue(batchSizeTmp > 0, "batchSize must be a positive integer");
        for (int i = 0; i < count; i++) {
            if (i >= dataFolders.size() || i >= workFolders.size() || i >= mappingsFileNames.size() || i >= defaultOwnerIds.size() || i >= archivalsStores.size()) {
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
            Assert.notNull(archivalsRoot, "archivalsStore " + (i + 1) + " does not exist");

            if (execute) {
                log.info("Executing importer for arguments group " + (i + 1));
                try {
                    postipoissStructureImporter.runImport(dataFolder, workFolder, archivalsRoot);
                    postipoissDocumentsImporter.runImport(dataFolder, workFolder, archivalsRoot, mappingsFile, batchSizeTmp, defaultOwnerIds.get(i));
                } catch (StopException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Importer error for arguments group " + (i + 1) + " (" + archivalsStore + ") : " + e.getMessage(), e);
                }
            }
        }
    }

    // ========================================================================

    private final int count = 5;
    private List<String> dataFolders;
    private List<String> workFolders;
    private List<String> mappingsFileNames;
    private List<String> defaultOwnerIds;
    private List<String> archivalsStores;
    private int batchSize = 50;

    private void init() {
        if (dataFolders == null || workFolders == null || mappingsFileNames == null || defaultOwnerIds == null || archivalsStores == null) {
            List<String> dataFoldersTmp = new ArrayList<String>();
            List<String> workFoldersTmp = new ArrayList<String>();
            List<String> mappingsFileNamesTmp = new ArrayList<String>();
            List<String> defaultOwnerIdsTmp = new ArrayList<String>();
            List<String> archivalsStoresTmp = new ArrayList<String>();

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
                archivalsStoresTmp.add(archivalsStore);
            }

            dataFolders = dataFoldersTmp;
            workFolders = workFoldersTmp;
            mappingsFileNames = mappingsFileNamesTmp;
            defaultOwnerIds = defaultOwnerIdsTmp;
            archivalsStores = archivalsStoresTmp;
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
        return defaultOwnerIds;
    }

    public List<String> getArchivalsStores() {
        init();
        return archivalsStores;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    private GeneralService generalService;
    private PostipoissStructureImporter postipoissStructureImporter;
    private PostipoissDocumentsImporter postipoissDocumentsImporter;

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setPostipoissStructureImporter(PostipoissStructureImporter postipoissStructureImporter) {
        this.postipoissStructureImporter = postipoissStructureImporter;
    }

    public void setPostipoissDocumentsImporter(PostipoissDocumentsImporter postipoissDocumentsImporter) {
        this.postipoissDocumentsImporter = postipoissDocumentsImporter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        postipoissStructureImporter.setPostipoissImporter(this);
        postipoissDocumentsImporter.setPostipoissImporter(this);
    }

}
