<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Delete tasks that have no store_id field filled, assuming that those tasks don't exist in repo ant more
 * 
 * @author Riina Tens
 */
public class LogAndDeleteNotExistingTasks extends AbstractModuleComponent {
    private final Log log = LogFactory.getLog(getClass());

    String inputFolderPath;

    @Override
    protected void executeInternal() throws Throwable {

        List<List<String>> deletedTasks = BeanHelper.getWorkflowDbService().deleteNotExistingTasks();
        File resultsFile = new File(new File(inputFolderPath), getName() + ".csv");
        log.info("Writing " + deletedTasks.size() + " tasks to file " + resultsFile.getAbsolutePath());
        List<String[]> records = new ArrayList<String[]>();
        for (List<String> taskData : deletedTasks) {
            records.add(taskData.toArray(new String[taskData.size()]));
        }
        AbstractNodeUpdater.writeRecordsToCsvFile(resultsFile, records);

    }

    public void setInputFolderPath(String inputFolderPath) {
        this.inputFolderPath = inputFolderPath;
    }

}
=======
package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Delete tasks that have no store_id field filled, assuming that those tasks don't exist in repo ant more
 */
public class LogAndDeleteNotExistingTasks extends AbstractModuleComponent {
    private final Log log = LogFactory.getLog(getClass());

    String inputFolderPath;

    @Override
    protected void executeInternal() throws Throwable {
        log.info("Executing LogAndDeleteNotExistingTasks updater.");
        List<List<String>> deletedTasks = BeanHelper.getWorkflowDbService().deleteNotExistingTasks();
        File resultsFile = new File(new File(inputFolderPath), getName() + ".csv");
        log.info("Writing " + (deletedTasks.size() - 1) + " tasks to file " + resultsFile.getAbsolutePath());
        List<String[]> records = new ArrayList<String[]>();
        for (List<String> taskData : deletedTasks) {
            records.add(taskData.toArray(new String[taskData.size()]));
        }
        AbstractNodeUpdater.writeRecordsToCsvFile(resultsFile, records);
        log.info("Successfully finished LogAndDeleteNotExistingTasks updater.");
    }

    public void setInputFolderPath(String inputFolderPath) {
        this.inputFolderPath = inputFolderPath;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
