package ee.webmedia.alfresco.user.service;

import java.net.URL;

import org.springframework.beans.factory.InitializingBean;

public class KerberosConfig implements InitializingBean {

    private static final String DEFAULT_LOGIN_CONFIG = "/login.conf";
    private static final String SYS_PROP_LOGIN_CONF = "java.security.auth.login.config";
    private static final String SYS_PROP_KERBEROS_CONF = "java.security.krb5.conf";
    private static final String SYS_PROP_KERBEROS_REALM = "java.security.krb5.realm";
    private static final String SYS_PROP_KERBEROS_KDC = "java.security.krb5.kdc";

    @Override
    public void afterPropertiesSet() throws Exception {
        URL url = getClass().getResource(DEFAULT_LOGIN_CONFIG);
        System.setProperty(SYS_PROP_LOGIN_CONF, url.toExternalForm());
    }

    public void setKerberosConf(final String kerberosConf) {
        System.setProperty(SYS_PROP_KERBEROS_CONF, kerberosConf);
    }

    public void setKerberosKdc(final String kerberosKdc) {
        System.setProperty(SYS_PROP_KERBEROS_KDC, kerberosKdc);
    }

    public void setKerberosRealm(final String kerberosRealm) {
        System.setProperty(SYS_PROP_KERBEROS_REALM, kerberosRealm);
    }

}
