package ee.webmedia.alfresco.common.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.module.ModuleService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.parameters.service.ParametersService.ParameterChangedCallback;

public class ApplicationServiceImpl implements ApplicationService, InitializingBean {

    public static final String versionPropertyKey = "currentVersion";

    private ModuleService moduleService;
    private ParametersService parametersService;

    private String commonVersion;
    private String projectVersion;
    private String projectName;
    private boolean test;
    private String logoutRedirectUrl;
    private String serverUrl;

    // Cache parameter values here, because these are accessed very frequently
    // (Although they always hit Hibernate cache, 8 calls to ParametersService add a total of 50 ms to each page render) 
    private String headerText;
    private String footerText;

    public void setModuleService(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
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
    public void afterPropertiesSet() throws Exception {
        parametersService.addParameterChangeListener(Parameters.HEADER_TEXT.getParameterName(), new ParameterChangedCallback() {
            @Override
            public void doWithParameter(Serializable value) {
                headerText = (String) value;
            }
        });
        parametersService.addParameterChangeListener(Parameters.FOOTER_TEXT.getParameterName(), new ParameterChangedCallback() {
            @Override
            public void doWithParameter(Serializable value) {
                footerText = (String) value;
            }
        });
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

    @Override
    public String getHeaderText() {
        if (headerText == null) {
            // When application is started and first login fails, then relogin.jsp page wants to get headerText, but no user is logged in
            AuthenticationUtil.runAs(new RunAsWork<Object>() {
                @Override
                public Object doWork() throws Exception {
                    headerText = parametersService.getStringParameter(Parameters.HEADER_TEXT);
                    return null;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
        return headerText;
    }

    @Override
    public String getFooterText() {
        if (footerText == null) {
            AuthenticationUtil.runAs(new RunAsWork<Object>() {
                @Override
                public Object doWork() throws Exception {
                    footerText = parametersService.getStringParameter(Parameters.FOOTER_TEXT);
                    return null;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
        return footerText;
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
