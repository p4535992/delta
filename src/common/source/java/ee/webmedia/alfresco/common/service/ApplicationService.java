package ee.webmedia.alfresco.common.service;

/**
 * Information about specific project and common project.
 * 
 * @author Alar Kvell (alar.kvell@webmedia.ee)
 */
public interface ApplicationService {

    /**
     * Version number of common project, in the format of '1.2.3.4' or '1.2.3.4-change98765'
     */
    String getCommonVersion();

    /**
     * Version number of specific project, in the format of '1.2.3.4' or '1.2.3.4-change98765'
     */
    String getProjectVersion();

    /**
     * Name of specific project
     */
    String getProjectName();

    /**
     * Title of specific project
     */
    String getProjectTitle();

}
