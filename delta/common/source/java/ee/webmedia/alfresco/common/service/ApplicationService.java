package ee.webmedia.alfresco.common.service;

/**
 * Information about specific project and common project.
 * 
 * @author Alar Kvell (alar.kvell@webmedia.ee)
 */
public interface ApplicationService {

    String BEAN_NAME = "ApplicationService";

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

    /**
     * Is current environment a test or production environment
     */
    boolean isTest();

    String getLogoutRedirectUrl();

    String getServerUrl();

    /**
     * Text to display at the top part of web page. Plain text, should be escaped.
     */
    String getHeaderText();

    /**
     * Text to display at the bottom part of web page. Text can contain HTML, should not be escaped.
     */
    String getFooterText();

}
