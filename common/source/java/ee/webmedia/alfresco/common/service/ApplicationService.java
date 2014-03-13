package ee.webmedia.alfresco.common.service;

import org.alfresco.util.Pair;
import org.springframework.context.ApplicationContext;

/**
 * Information about specific project and common project.
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

    ApplicationContext getApplicationContext();

    /**
     * @return URL of custom logo if configured, or URL of standard logo otherwise
     */
    String getLogoUrl();

    /**
     * @return image of custom logo if configured, or {@code null} otherwise
     */
    Pair<byte[], String> getCustomLogo();

    /**
     * @return URL for signed jumploader applet if configured
     */
    String getJumploaderUrl();

    /**
     * @return jar of signed jumploader applet if configured, or {@code null} otherwise
     */
    Pair<byte[], String> getJumploaderApplet();
}
