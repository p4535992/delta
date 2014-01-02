package ee.webmedia.alfresco.signature.service;

import static ee.webmedia.alfresco.utils.CalendarUtil.duration;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.simple.AbstractParameterizedContextMapper;
import org.springframework.ldap.core.simple.SimpleLdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Alar Kvell
 */
public class SkLdapServiceImpl implements SkLdapService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SkLdapServiceImpl.class);

    private SimpleLdapTemplate ldapTemplate;

    @Override
    public List<SkLdapCertificate> getCertificates(String serialNumber) {
        Assert.isTrue(StringUtils.isNotEmpty(serialNumber));

        // XXX It would be better to pass serialNumber argument directly to query, so we wouldn't have to do any checks ourselves.
        // But it would have to be done safely (like JDBC query arguments or similar), and currently there was no time to find out how to do that.
        if (!StringUtils.isNumeric(serialNumber)) {
            return Collections.emptyList();
        }

        String filter = "(serialNumber=" + serialNumber + ")";
        long startTime = System.nanoTime();
        try {
            List<SkLdapCertificate> list = ldapTemplate.search("", filter, new SkLdapCertificateMapper());
            long stopTime = System.nanoTime();
            LOG.info("PERFORMANCE: query skLdapSearchBySerialNumber - " + duration(startTime, stopTime) + " ms");
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_LDAP);
            return list;
        } catch (NamingException e) {
            long stopTime = System.nanoTime();
            MonitoringUtil.logError(MonitoredService.OUT_SK_LDAP, e);
            LOG.error("Error performing query from SK LDAP service (took " + duration(startTime, stopTime) + " ms) : " + filter, e);
            throw new UnableToPerformException("sk_ldap_error");
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_SK_LDAP, e);
            throw e;
        }
    }

    public static class SkLdapCertificateMapper extends AbstractParameterizedContextMapper<SkLdapCertificate> {

        @Override
        protected SkLdapCertificate doMapFromContext(DirContextOperations ctx) {
            return new SkLdapCertificate(
                    (String) ctx.getObjectAttribute("cn"),
                    (String) ctx.getObjectAttribute("serialNumber"),
                    (byte[]) ctx.getObjectAttribute("userCertificate;binary"));
        }

    }

    public void setLdapContextSource(LdapContextSource ldapContextSource) {
        ldapTemplate = new SimpleLdapTemplate(ldapContextSource);
    }

}
