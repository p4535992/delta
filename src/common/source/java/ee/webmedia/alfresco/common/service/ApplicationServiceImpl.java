package ee.webmedia.alfresco.common.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.alfresco.service.cmr.module.ModuleService;
import org.springframework.core.io.Resource;

public class ApplicationServiceImpl implements ApplicationService {

    public static final String versionPropertyKey = "currentVersion";

    private ModuleService moduleService;

    private String commonVersion;
    private String projectVersion;
    private String projectName;
    private boolean test;
    private String logoutRedirectUrl;
    private String serverUrl;

    public void setModuleService(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    public void setCommonVersionLocation(Resource resource) {
        commonVersion = getVersionProperty(loadProperties(resource));
    }

    public void setProjectVersionLocation(Resource resource) {
        projectVersion = getVersionProperty(loadProperties(resource));
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public void setLogoutRedirectUrl(String logoutRedirectUrl) {
        this.logoutRedirectUrl = logoutRedirectUrl;
    }

    @Override
    public String getCommonVersion() {
        return commonVersion;
    }

    @Override
    public String getProjectVersion() {
        return projectVersion;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public String getProjectTitle() {
        return moduleService.getModule(projectName).getTitle();
    }

    @Override
    public boolean isTest() {
        return test;
    }

    @Override
    public String getLogoutRedirectUrl() {
        return logoutRedirectUrl;
    }
    
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public String getServerUrl(){
        return serverUrl;
    }    

    private static Properties loadProperties(Resource resource) {
        try {
            InputStream is = null;
            try {
                is = resource.getInputStream();
                Properties properties = new Properties();
                properties.load(is);
                return properties;
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getVersionProperty(Properties properties) {
        return properties.getProperty(versionPropertyKey);
    }

}
