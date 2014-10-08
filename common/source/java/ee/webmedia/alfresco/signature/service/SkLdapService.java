package ee.webmedia.alfresco.signature.service;

import java.util.List;

import ee.webmedia.alfresco.signature.model.SkLdapCertificate;

/**
 * Service for fetching certificates from SK LDAP directory - http://www.sk.ee/repositoorium/ldap/
 * Directory contains only valid certificates. Revoked certificates are immediately removed from directory, expired certificates are removed during the next day.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public interface SkLdapService {

    String BEAN_NAME = "SkLdapService";

    List<SkLdapCertificate> getCertificates(String serialNumber);

}
