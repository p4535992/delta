package ee.webmedia.alfresco.signature.service;

import ee.sk.digidoc.SignedDoc;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import org.apache.commons.lang.StringUtils;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.simple.AbstractParameterizedContextMapper;
import org.springframework.ldap.core.simple.SimpleLdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.util.Assert;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ee.webmedia.alfresco.utils.CalendarUtil.duration;

public class SkLdapServiceImpl implements SkLdapService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SkLdapServiceImpl.class);

    private int pageSize = 1000;
    private SimpleLdapTemplate ldapTemplate;

    @Override
    public List<SkLdapCertificate> getCertificates(String serialNumber) {
        LOG.debug("Get certificates by serialNumber...");
        Assert.isTrue(StringUtils.isNotEmpty(serialNumber));

        // XXX It would be better to pass serialNumber argument directly to query, so we wouldn't have to do any checks ourselves.
        // But it would have to be done safely (like JDBC query arguments or similar), and currently there was no time to find out how to do that.
        if (!StringUtils.isNumeric(serialNumber)) {
            return Collections.emptyList();
        }

        String filter = "(serialNumber=" + serialNumber + ")";
        return skLdapRequestByFilter(filter);
    }
    
    @Override
    public List<SkLdapCertificate> getCertificatesByName(String cnName) {
        LOG.debug("Get certificates by cnName...");
        Assert.isTrue(StringUtils.isNotEmpty(cnName));

        String filter = "(cn=" + cnName + "*)";
        return skLdapRequestByFilter(filter);
    }

    private List<SkLdapCertificate> skLdapRequestByFilter(String filter){
        long startTime = System.nanoTime();
        try {
            LOG.debug("LDAP search filter: [" + filter + "]");
            List<SkLdapCertificate> list = ldapTemplate.search("", filter,  new SkLdapCertificateMapper());
            if(list != null){
                LOG.debug("Find certificates: " + list.size());
            }
            list.removeAll(Collections.singleton(null));
            if(list != null){
                LOG.debug("After removing null objects: Find certificates: " + list.size());
            }
            long stopTime = System.nanoTime();
            LOG.info("PERFORMANCE: query skLdapSearchByFilter - " + duration(startTime, stopTime) + " ms");
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_LDAP);
            if (list != null && !list.isEmpty()) {
                // update orgCertificates
                LOG.info("Found certificates: " + list.size());
            }
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

        int pageSize = 50;

        @Override
        protected SkLdapCertificate doMapFromContext(DirContextOperations ctx) {
        	String cn = ctx.getStringAttribute("cn");
        	LOG.debug("doMapFromContext(ctx): CN:" + cn);
        	String serialNumber = ctx.getStringAttribute("serialNumber");
            LOG.debug("doMapFromContext(ctx): serialNumber:" + serialNumber);
        	if (StringUtils.isBlank(cn) || StringUtils.isBlank(serialNumber)) {
                LOG.warn("doMapFromContext(ctx): CN or SerialNumber is BLANK! Return NULL...");
        		return null;
        	} else {
                List<byte[]> certList = new ArrayList<>();
                Object[] c = ctx.getObjectAttributes("userCertificate;binary");
                for (Object o : c){
                    LOG.debug("CERT object: " + o.toString());
                    LOG.debug("CERT object class: " + o.getClass().toString());
                    if(o.getClass().toString().equals("class [B")){
                        LOG.debug("Use byte[] format...");
                        byte[] cert = (byte[]) o;
                        certList.add(cert);
                    } else {
                        LOG.error("NOT SUPPORTED OR UNKNOWN OBJECT TYPE!!!");
                    }
                }

        	    byte[] userEncryptionCertificate = null;
        	    if(certList != null){
        	        LOG.debug("cerList size(): " + certList);
        	        userEncryptionCertificate = getCertificateForEncryption(certList);
                }
        		return new SkLdapCertificate(
                    ctx.getStringAttribute("cn"),
                    ctx.getStringAttribute("serialNumber"),
                    certList,
                    userEncryptionCertificate);
        	}
        }

        private static byte [] getCertificateForEncryption(List<byte[]> certList) {
            byte[] cert = null;
            for (byte[] certData : certList){
                cert = getCertificateForEncryption(certData);
                if(cert != null){
                    break;
                }
            }
            return cert;
        }

        private static byte [] getCertificateForEncryption(byte [] certData) {
            X509Certificate cert = null;
            try {
                LOG.debug("Reading certificate...");
                cert = SignedDoc.readCertificate(certData);

                boolean[] keyUsageArray = cert.getKeyUsage();

                boolean keyEncipherment = keyUsageArray[2];
                LOG.debug("Is KeyEncipherment in use: " + keyEncipherment);
                if (!keyEncipherment) {
                    LOG.debug("keyEncipherment is not in use! Returning NULL!");
                    return null;
                }

                LOG.debug("FOUND certificate with encryption support! Returning it...");
                return certData;
            } catch (Exception e) {
                LOG.error("Failed to get encryption certificate!", e);
                return null;
            }
        }

    }

    public void setLdapContextSource(LdapContextSource ldapContextSource) {
        ldapTemplate = new SimpleLdapTemplate(ldapContextSource);
    }
}
