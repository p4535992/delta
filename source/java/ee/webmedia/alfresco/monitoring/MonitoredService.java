package ee.webmedia.alfresco.monitoring;

public enum MonitoredService {
    OUT_SK_OCSP,
    OUT_SK_DIGIDOCSERVICE,
    OUT_SK_LDAP,
    OUT_AD_KERBEROS,
    OUT_AD_LDAP,
    OUT_XTEE_DVK,
    OUT_SMTP,
    OUT_MSO,
    OUT_DATABASE,
    OUT_OPENOFFICE,
    IN_WWW,
    IN_WEBDAV,
    IN_IMAP,
    IN_ADD_DOCUMENT,
    IN_ADR;
    // OUT_FILESYSTEM could also be monitored, but too difficult
    // OUT/IN_EHCACHE RMI sync between cluster nodes could also be monitored, but too difficult
}
