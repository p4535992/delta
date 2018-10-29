package ee.smit.tera;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.sharepoint.ImportStatus;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.faces.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class TeraProcess {

    protected final Log log = LogFactory.getLog(getClass());
    private final AtomicBoolean fileFinderRunning = new AtomicBoolean();
    private final AtomicBoolean fileStopFlag = new AtomicBoolean();

    private final AtomicBoolean overStampingRunning = new AtomicBoolean();
    private final AtomicBoolean overStampingStopFlag = new AtomicBoolean();

    private final AtomicBoolean dataFixRunning = new AtomicBoolean();
    private final AtomicBoolean dataFixStopFlag = new AtomicBoolean();

    private final ImportStatus status = new ImportStatus();
    private final ProcessSettings data  = new ProcessSettings();

    private int threadCounter;

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public ImportStatus getStatus() {
        return status;
    }

    // ---------------------------------------------------------------------------------------------------------
    // DATA FIX PROCESS
    // ---------------------------------------------------------------------------------------------------------
    public boolean isDataFixRunning() {
        return dataFixRunning.get();
    }
    public boolean isDataFixStopping() {
        return isDataFixRunning() && dataFixStopFlag.get();
    }

    /** @param event */
    public synchronized void stopDataFix(ActionEvent event) {
        fileFinderRunning.set(true);
        log.info("ASIC-S process stop requested; raised the flag.");
    }

    /** @param event */
    public synchronized void startDataFixImporterInBackground(ActionEvent event) {
        if (!isDataFixRunning()) {
            dataFixRunning.set(true);
            log.info("Importing in background...");
            ProcessSettings processData = data.clone();

            DataFixWork dataFixWork = new DataFixWork(processData);
            startDataFixProcessThread(dataFixWork);
        } else {
            log.warn("Filefinder process is already running; skipping another request to run in background");
        }
    }

    private void startDataFixProcessThread(DataFixBaseProcessWork... work){
        new Thread(new DataFixJob(work), "DataFixProcessThread" + threadCounter++).start();
    }

    private class DataFixWork extends DataFixBaseProcessWork {
        private final ProcessSettings processData;
        private DataFixProcess dataFixProcess;

        public DataFixWork(ProcessSettings processSetting){
            this.processData = processSetting;
        }

        @Override
        public Boolean doWork() {
            try {
                status.beginFiles();
                dataFixProcess = new DataFixProcess(processData);
                super.doWork();
            } finally {
                status.endFiles();
            }
            return null;
        }

        @Override
        public Boolean execute() {
            // Do nothing
            return false;
        }
    }

    /**
     * A Runnable implementation which runs given work in transaction.
     */
    private class DataFixJob implements Runnable {

        private final DataFixBaseProcessWork[] work;

        public DataFixJob(DataFixBaseProcessWork... work) {
            this.work = work;
        }

        @Override
        public void run() {
            try {
                for (DataFixBaseProcessWork workItem : work) {
                    AuthenticationUtil.runAs(workItem, AuthenticationUtil.getSystemUserName());
                    if (dataFixStopFlag.get()) {
                        log.info("Process was stopped; quitting the process work.");
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Process error", e);
            } finally {
                dataFixRunning.set(false);
                dataFixStopFlag.set(false);
                log.info("Process in background completed!");
            }
        }
    }

    private abstract class DataFixBaseProcessWork implements AuthenticationUtil.RunAsWork<Boolean>, RetryingTransactionHelper.RetryingTransactionCallback<Boolean> {
        @Override
        public Boolean doWork() {
            boolean someDataWasImported = false;
            try {
                while (executeWithoutTransaction()) {
                    someDataWasImported = true;
                    if (dataFixStopFlag.get()) {
                        log.info("Process was stopped; quitting the import work.");
                        break;
                    }

                    log.trace("Process next document import batch");
                }
            } catch (Exception e) {
                log.error("Process error", e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return someDataWasImported;
        }

        protected Boolean executeWithoutTransaction() {
            return getTransactionHelper().doInTransaction(this);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // FILE FINDER PROCESS
    // ---------------------------------------------------------------------------------------------------------
    public boolean isFileFinderRunning() {
        return fileFinderRunning.get();
    }
    public boolean isFileFinderStopping() {
        return isFileFinderRunning() && fileStopFlag.get();
    }

    /** @param event */
    public synchronized void stopFileFinder(ActionEvent event) {
        fileFinderRunning.set(true);
        log.info("ASIC-S process stop requested; raised the flag.");
    }

    /** @param event */
    public synchronized void startFileImporterInBackground(ActionEvent event) {
        if (!isFileFinderRunning()) {
            fileFinderRunning.set(true);
            log.info("Importing in background...");
            FindingFilesWork findingFilesWork = new FindingFilesWork();
            startFileFinderProcessThread(findingFilesWork);
        } else {
            log.warn("Filefinder process is already running; skipping another request to run in background");
        }
    }

    private void startFileFinderProcessThread(FileFinderBaseProcessWork... work){
        new Thread(new FileFinderJob(work), "TeraFileFinderProcessThread" + threadCounter++).start();
    }

    private class FindingFilesWork extends FileFinderBaseProcessWork {
        @Override
        public Boolean doWork() {
            try {
                status.beginFiles();
                new FindingFilesProcess();
                super.doWork();
            } finally {
                status.endFiles();
            }
            return null;
        }

        @Override
        public Boolean execute() {
            // Do nothing
            return false;
        }
    }

    /**
     * A Runnable implementation which runs given work in transaction.
     */
    private class FileFinderJob implements Runnable {

        private final FileFinderBaseProcessWork[] work;

        public FileFinderJob(FileFinderBaseProcessWork... work) {
            this.work = work;
        }

        @Override
        public void run() {
            try {
                for (FileFinderBaseProcessWork workItem : work) {
                    AuthenticationUtil.runAs(workItem, AuthenticationUtil.getSystemUserName());
                    if (fileStopFlag.get()) {
                        log.info("Process was stopped; quitting the process work.");
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Process error", e);
            } finally {
                fileFinderRunning.set(false);
                fileStopFlag.set(false);
                log.info("Process in background completed!");
            }
        }
    }

    private abstract class FileFinderBaseProcessWork implements AuthenticationUtil.RunAsWork<Boolean>, RetryingTransactionHelper.RetryingTransactionCallback<Boolean> {
        @Override
        public Boolean doWork() {
            boolean someDataWasImported = false;
            try {
                while (executeWithoutTransaction()) {
                    someDataWasImported = true;
                    if (fileStopFlag.get()) {
                        log.info("Process was stopped; quitting the import work.");
                        break;
                    }

                    log.trace("Process next document import batch");
                }
            } catch (Exception e) {
                log.error("Process error", e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return someDataWasImported;
        }

        protected Boolean executeWithoutTransaction() {
            return getTransactionHelper().doInTransaction(this);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // OVER STAMPING PROCESS
    // ---------------------------------------------------------------------------------------------------------
    public boolean isOverStampingRunning() {
        return overStampingRunning.get();
    }
    public boolean isOverStampingStopping() {
        return isOverStampingRunning() && overStampingStopFlag.get();
    }

    /** @param event */
    public synchronized void stopOverStamping(ActionEvent event) {
        overStampingStopFlag.set(true);
        log.info("ASIC-S process stop requested; raised the flag.");
    }


    /** @param event */
    public synchronized void startTeraImporterInBackground(ActionEvent event) {
        if (!isOverStampingRunning()) {
            overStampingRunning.set(true);
            log.info("Importing in background...");

            //status.reset();

            ProcessSettings processData = data.clone();

            // 2 - check and create ASIC-S if needed
            TimesStampingWork timesStampingWork = new TimesStampingWork(processData);

            startOverStampingProcessThread(timesStampingWork);

        } else {
            log.warn("Overstamping process is already running; skipping another request to run in background");
        }
    }

    private void startOverStampingProcessThread(OverStampingBaseProcessWork... work){
        new Thread(new OverStampingJob(work), "TeraOverStampingProcessThread" + threadCounter++).start();
    }

    private class TimesStampingWork extends OverStampingBaseProcessWork {
        private final ProcessSettings processData;
        private TimeStampingFilesProcess timeStampingFilesProcess;

        public TimesStampingWork(ProcessSettings processSetting){
            this.processData = processSetting;
        }

        @Override
        public Boolean doWork() {
            try {
                status.beginTimestamping();
                timeStampingFilesProcess = new TimeStampingFilesProcess(processData, status);
                super.doWork();
            } finally {
                status.endTimestamping();
            }
            return null;
        }

        @Override
        protected Boolean executeWithoutTransaction() {
            return timeStampingFilesProcess.doBatch();
        }
        @Override
        public Boolean execute() {
            // Do nothing
            return false;
        }
    }

    // Base methods
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setTransactionService(BeanHelper.getTransactionService());
        return helper;
    }

    /**
     * A Runnable implementation which runs given work in transaction.
     */
    private class OverStampingJob implements Runnable {

        private final OverStampingBaseProcessWork[] work;

        public OverStampingJob(OverStampingBaseProcessWork... work) {
            this.work = work;
        }

        @Override
        public void run() {
            try {
                for (OverStampingBaseProcessWork workItem : work) {
                    AuthenticationUtil.runAs(workItem, AuthenticationUtil.getSystemUserName());
                    if (overStampingStopFlag.get()) {
                        log.info("Process was stopped; quitting the process work.");
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Process error", e);
            } finally {
                overStampingRunning.set(false);
                overStampingStopFlag.set(false);
                log.info("Process in background completed!");
            }
        }
    }

    private abstract class OverStampingBaseProcessWork implements AuthenticationUtil.RunAsWork<Boolean>, RetryingTransactionHelper.RetryingTransactionCallback<Boolean> {
        @Override
        public Boolean doWork() {
            boolean someDataWasImported = false;
            try {
                while (executeWithoutTransaction()) {
                    someDataWasImported = true;
                    if (overStampingStopFlag.get()) {
                        log.info("Process was stopped; quitting the import work.");
                        break;
                    }

                    log.trace("Process next document import batch");
                }
            } catch (Exception e) {
                log.error("Process error", e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return someDataWasImported;
        }

        protected Boolean executeWithoutTransaction() {
            return getTransactionHelper().doInTransaction(this);
        }
    }
}
