package ee.webmedia.alfresco.common.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.parameters.service.ParametersService.ParameterChangedCallback;

public class ApplicationServiceImpl implements ApplicationService, InitializingBean, ApplicationContextAware {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ApplicationServiceImpl.class);

    public static final String versionPropertyKey = "currentVersion";

    private ModuleService moduleService;
    private ParametersService parametersService;
    private MimetypeService mimetypeService;

    private String commonVersion;
    private String projectVersion;
    private String projectName;
    private boolean test;
    private String logoutRedirectUrl;
    private String serverUrl;
    private File logoFile;
    private String logoMimeType;

    // Cache parameter values here, because these are accessed very frequently
    // (Although they always hit Hibernate cache, 8 calls to ParametersService add a total of 50 ms to each page render)
    private String headerText;
    private String footerText;
    private String mDeltaFooterText;

    public void setModuleService(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
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
        parametersService.addParameterChangeListener(Parameters.M_DELTA_FOOTER_TEXT.getParameterName(), new ParameterChangedCallback() {
            @Override
            public void doWithParameter(Serializable value) {
                mDeltaFooterText = (String) value;
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

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    public void setLogoFile(String logoFile) {
        this.logoFile = StringUtils.isBlank(logoFile) ? null : new File(logoFile);
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

    @Override
    public String getMDeltaFooterText() {
        if (mDeltaFooterText == null) {
            AuthenticationUtil.runAs(new RunAsWork<Object>() {
                @Override
                public Object doWork() throws Exception {
                    mDeltaFooterText = parametersService.getStringParameter(Parameters.M_DELTA_FOOTER_TEXT);
                    return null;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
        return mDeltaFooterText;
    }

    @Override
    public String getLogoUrl() {
        if (logoFile == null) {
            return "/images/logo/logo.png";
        }
        return "/n/logo";
    }

    @Override
    public Pair<byte[], String> getCustomLogo() {
        if (logoFile == null) {
            return null;
        }
        try {
            byte[] logoBytes = FileUtils.readFileToByteArray(logoFile);
            if (logoMimeType == null) {
                logoMimeType = mimetypeService.guessMimetype(logoFile.getName());
            }
            return Pair.newInstance(logoBytes, logoMimeType);
        } catch (IOException e) {
            LOG.warn("Error reading logo file '" + logoFile.getPath() + "': " + e.getMessage(), e);
            return null;
        }
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

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
