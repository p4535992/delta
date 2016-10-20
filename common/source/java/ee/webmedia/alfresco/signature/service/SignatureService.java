package ee.webmedia.alfresco.signature.service;

import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;

/**
 * Service class that handles DigiDoc containers.
 * Has methods that create, check, sign, retrieve signatures, signature digests and files from the container.
 */
public interface SignatureService {

    String BEAN_NAME = "SignatureService";
    String DIGIDOC_MIMETYPE = "application/digidoc";

    /**
     * Parse certificates and return only certificates suitable for encryption recipients.
     *
     * @param certificates
     * @throws SignatureRuntimeException
     * @return
     */
    List<X509Certificate> getCertificatesForEncryption(List<SkLdapCertificate> certificates);
    
    X509Certificate getCertificateForEncryption(SkLdapCertificate skLdapCertificate);
    
    X509Certificate getCertificateForEncryption(byte [] certData, String certName);
    
    /**
     * Create a CDOC file (encrypted container).
     *
     * @param output output stream that newly created CDOC file is written to; stream is closed automatically.
     * @param contents input files that are encrypted; at least one file is required.
     * @param recipientCerts certificates of recipient or recipient; at least one is required.
     * @param containerFileName name of the container being written
     * @throws SignatureRuntimeException
     */
    void writeEncryptedContainer(OutputStream output, List<NodeRef> contents, List<X509Certificate> recipientCerts, String containerFileName);

}
