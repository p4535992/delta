package ee.webmedia.alfresco.gopro;

import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.generator.SaveListener.ValidationHelper;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.log.LogHelper;
import ee.webmedia.alfresco.postipoiss.PostipoissImporter.StopException;

/**
 * Entry point for starting and stopping whole import. Manages input parameters and coordinates structure and document import.
 */
public class GoProImporter implements SaveListener {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(GoProImporter.class);

    private final AtomicBoolean updaterRunning = new AtomicBoolean();
    private final AtomicBoolean importerRunning = new AtomicBoolean();
    private final AtomicBoolean stopFlag = new AtomicBoolean();
    private final ImportSettings data = new ImportSettings();
    private final ImportStatus status = new ImportStatus();

    /*
     * Each time import is started, the counter will be incremented.
     * The counter value will reflect in importer thread name.
     * This value is not used for controlling anything.
     */
    private int threadCounter;

    private SimpleJdbcTemplate jdbcTemplate;
    
    private String institutionCode;

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public ImportSettings getData() {
        return data;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public boolean isUpdaterRunning() {
        return updaterRunning.get();
    }

    public boolean isImporterRunning() {
        return importerRunning.get();
    }

    public boolean isImporterStopping() {
        return isImporterRunning() && stopFlag.get();
    }

    /** @param event */
    public synchronized void stopImporter(ActionEvent event) {
        stopFlag.set(true);
        LOG.info("Import stop requested; raised the flag.");
    }

    /** @param event */
    public synchronized void startImporterInBackground(ActionEvent event) {
        if (!isDataValid()) {
            return;
        }

        if (!isImporterRunning()) {
            importerRunning.set(true);
            LOG.info("Importing in background with following settings:\n" + ReflectionToStringBuilder.toString(data, ToStringStyle.MULTI_LINE_STYLE));

            status.reset();

            ImportSettings importData = data.clone();
            StructureImportWork structureImport = new StructureImportWork(importData);
            DocumentImportWork documentImport = new DocumentImportWork(importData);

            startImporterThread(structureImport, documentImport);
            
        } else {
            LOG.warn("Importer is already running; skipping another request to run import in background");
        }
    }

    private void startImporterThread(BaseImportWork... work) {
        new Thread(new ImportJob(work), "GoProImporterThread" + threadCounter++).start();
    }

    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setTransactionService(BeanHelper.getTransactionService());
        return helper;
    }

    private boolean isDataValid() {
        boolean valid = data.isValid();
        if (!valid) {
            Utils.addErrorMessage("GoPro importi parameetrid pole kõik korrektsed!");
        }
        return valid;
    }

    /**
     * A Runnable implementation which runs given work in transaction.
     */
    private class ImportJob implements Runnable {

        private final BaseImportWork[] work;

        public ImportJob(BaseImportWork... work) {
            this.work = work;
        }

        @Override
        public void run() {
            try {
                LogHelper.setUserInfo("127.0.0.1", "localhost");
                getLogService().clearPastIdSuffixCache();
                for (BaseImportWork workItem : work) {
                    AuthenticationUtil.runAs(workItem, AuthenticationUtil.getSystemUserName());

                    if (stopFlag.get()) {
                        LOG.info("Import was stopped; quitting the import work.");
                        break;
                    }
                }
            } catch (Exception e) {
                LOG.error("Importer error", e);
            } finally {
                getLogService().clearPastIdSuffixCache();
                importerRunning.set(false);
                stopFlag.set(false);
                LOG.info("Importing in background completed!");
            }
        }
    }

    private abstract class BaseImportWork implements RunAsWork<Boolean>, RetryingTransactionCallback<Boolean> {

        @Override
        public Boolean doWork() {
            boolean someDataWasImported = false;
            try {
                while (executeWithoutTransaction()) {
                    someDataWasImported = true;
                    if (stopFlag.get()) {
                        LOG.info("Import was stopped; quitting the import work.");
                        break;
                    }

                    LOG.trace("Starting next document import batch");
                }
            } catch (Exception e) {
                LOG.error("Importer error", e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return someDataWasImported;
        }

        protected Boolean executeWithoutTransaction() {
            return getTransactionHelper().doInTransaction(this);
        }

    }

    /**
     * A class with a callback for importing structure (functions, series, volumes, cases).
     */
    private class StructureImportWork extends BaseImportWork {

        private final ImportSettings importData;

        private StructureImporter importer;

        public StructureImportWork(ImportSettings importData) {
            this.importData = importData;
        }

        @Override
        public Boolean doWork() {
            try {
                status.beginStruct();
                importer = new StructureImporter();
                super.doWork();
            } finally {
            	importer.checkSeriesOrders();
                status.endStruct();
                status.setStructImportFinished(true);
            }
            return null;
        }

        @Override
        public Boolean execute() {
            importer.doImport(importData, status);
            return false;
        }
    }

    
     // A class with a callback for importing documents and their files.
    private class DocumentImportWork extends BaseImportWork {

        private final ImportSettings importData;

        private GoProDocumentsImporter importer;

        public DocumentImportWork(ImportSettings importData) {
            this.importData = importData;
        }

        @Override
        public Boolean doWork() {
            boolean someDataWasImported = false;
            try {
                status.beginDocs();
                importer = new GoProDocumentsImporter(GoProImporter.this);
                someDataWasImported = super.doWork();
            } finally {
                if (someDataWasImported) {
                    BeanHelper.getDocumentListService().updateDocCounters();
                } else {
                    LOG.info("No documents were currently imported, skipping updating document list counters");
                }
                status.endDocs();
            }
            return null;
        }

        @Override
        protected Boolean executeWithoutTransaction() {
        	try {
        		// to let structure import to finish
                int count = 0;
                while (!status.isStructImportFinished() && count < 60) {
                	count++;
                	try {
                		Thread.sleep(60000);
                	} catch (InterruptedException e) {
                	}
                }
                
                try {
            		Thread.sleep(180000);
            	} catch (InterruptedException e) {
            	}
        		importer.runImport(new File(importData.getDataFolder()), new File(importData.getWorkFolder()), importData.getMappingsFile(), importData.getBatchSize(), importData.getDefaultOwnerId(), institutionCode);
        	} catch (Exception e) {
        		LOG.error("GoProDocumentsImporter failed: " + e.getMessage());
        		return false;
        	}
        	return false;
            
        }

        @Override
        public Boolean execute() {
            // Do nothing
            return false;
        }
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
