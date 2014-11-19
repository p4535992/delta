<<<<<<< HEAD
package ee.webmedia.alfresco.report.model;

/**
 * @author Riina Tens
 */
public enum ReportStatus {
    /** Waiting for execution */
    IN_QUEUE,
    /** Executing */
    RUNNING,
    /** Execution completed */
    FINISHED,
    /** User has downloaded report previously in status FINISHED */
    FINISHED_DOWNLOADED,
    /** Execution succeeded, result contained more rows than Excel worksheet can hold, rows exceeding the limit are not added to file */
    EXCEL_FULL,
    /** User has downloaded report previously in status EXCEL_FULL */
    EXCEL_FULL_DOWNLOADED,
    /** Execution was cancelled by user */
    CANCELLED,
    /** Execution failed due to technical reasons (invalid template etc) */
    FAILED,
    /** User marked report for cancelling, waiting for cancel command to execute */
    CANCELLING_REQUESTED,
    /** User marked report for deleting, waiting for delete command to execute */
    DELETING_REQUESTED,
    /** Indicates that this report should be deleted as result of execution. This status is never written to repo. */
    DELETED
}
=======
package ee.webmedia.alfresco.report.model;

public enum ReportStatus {
    /** Waiting for execution */
    IN_QUEUE,
    /** Executing */
    RUNNING,
    /** Execution completed */
    FINISHED,
    /** User has downloaded report previously in status FINISHED */
    FINISHED_DOWNLOADED,
    /** Execution succeeded, result contained more rows than Excel worksheet can hold, rows exceeding the limit are not added to file */
    EXCEL_FULL,
    /** User has downloaded report previously in status EXCEL_FULL */
    EXCEL_FULL_DOWNLOADED,
    /** Execution was cancelled by user */
    CANCELLED,
    /** Execution failed due to technical reasons (invalid template etc) */
    FAILED,
    /** User marked report for cancelling, waiting for cancel command to execute */
    CANCELLING_REQUESTED,
    /** User marked report for deleting, waiting for delete command to execute */
    DELETING_REQUESTED,
    /** Indicates that this report should be deleted as result of execution. This status is never written to repo. */
    DELETED
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
