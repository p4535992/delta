package ee.webmedia.alfresco.utils;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

public class SystemPropertyInitializingBean implements InitializingBean {

    /** Properties to be set */
    private Map<String, String> systemProperties;

    /** Sets the system properties */
    public void afterPropertiesSet() throws Exception {
        if (systemProperties == null || systemProperties.isEmpty()) {
            return;
        }

        for (String key : systemProperties.keySet()) {
            String value = systemProperties.get(key);
            System.setProperty(key, value);
        }
    }

    public void setSystemProperties(Map<String, String> systemProperties) {
        this.systemProperties = systemProperties;
    }
}
