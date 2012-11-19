package org.jasig.cas.adaptors.x509.authentication.handler.support;

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.factory.BouncyCastleNotaryFactory;
import ee.sk.digidoc.factory.NotaryFactory;
import ee.sk.utils.ConfigManager;

import java.io.File;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

import org.jasig.cas.adaptors.x509.authentication.principal.X509CertificateCredentials;
import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.handler.support.AbstractPreAndPostProcessingAuthenticationHandler;
import org.jasig.cas.authentication.principal.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

/**
 * Authentication Handler that accepts X509 Certificiates, determines their
 * validity and performs OCSP check.
 * 
 * @author Alar Kvell
 */
public class X509CredentialsEstEidAuthenticationHandler extends AbstractPreAndPostProcessingAuthenticationHandler implements InitializingBean {

    /** Instance of Logging. */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private NotaryFactory notaryFactory;

    private boolean ocspEnabled = true;
    private boolean test = false;
    private String pkcs12Container;
    private String pkcs12Password;
    private String pkcs12CertSerial;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!ocspEnabled) {
            return;
        }
        if (StringUtils.hasLength(pkcs12Container)) {
            File container = new File(pkcs12Container);
            if (!container.canRead()) {
                throw new RuntimeException("Cannot read PKCS12 container file: " + container);
            }
            if (!container.isFile()) {
                throw new RuntimeException("PKCS12 container is not a regular file: " + container);
            }
            if (!StringUtils.hasText(pkcs12Password)) {
                throw new RuntimeException("PKCS12 container password must not be empty");
            }
            if (!StringUtils.hasText(pkcs12CertSerial)) {
                throw new RuntimeException("PKCS12 certificate serial number must not be empty");
            }
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put("SIGN_OCSP_REQUESTS", "true");
            props.put("DIGIDOC_PKCS12_CONTAINER", pkcs12Container);
            props.put("DIGIDOC_PKCS12_PASSWD", pkcs12Password);
            props.put("DIGIDOC_OCSP_SIGN_CERT_SERIAL", pkcs12CertSerial);
            log.info("Signing OCSP requests with certificate (serial number " + pkcs12CertSerial + ") from PKCS12 container " + pkcs12Container);
            // This must be done first, because init(Hashtable) resets configuration
            ConfigManager.init(props);
        }
        String config = "jdigidoc" + (isTest() ? "-test" : "") + ".cfg";
        // This is done second, because init(String) does not reset configuration
        if (!ConfigManager.init("jar://" + config)) {
            throw new RuntimeException("JDigiDoc initialization failed");
        }

        notaryFactory = new BouncyCastleNotaryFactory();
        notaryFactory.init();
        log.info("Notary factory initialized successfully");
    }

    protected final boolean doAuthentication(final Credentials credentials) throws AuthenticationException {

        final X509CertificateCredentials x509Credentials = (X509CertificateCredentials) credentials;
        final X509Certificate[] certificates = x509Credentials.getCertificates();

        if (certificates.length != 1) {
            return false;
        }
        final X509Certificate certificate = certificates[0];

        try {
            if (log.isDebugEnabled()) {
                log.debug("--examining cert["
                        + certificate.getSerialNumber().toString() + "] "
                        + certificate.getSubjectDN() + "\"" + " from issuer \""
                        + certificate.getIssuerDN().getName() + "\"");
            }

            // check basic validity of the current certificate
            certificate.checkValidity();
            log.debug("certificate is valid");

            if (ocspEnabled) {
                notaryFactory.checkCertificate(certificate);
                // If OCSP response is successful, then no exception is thrown
                log.debug("certificate OCSP confirmation succeeded");
            }

            // If certificate has been revoked, then the following error is logged:
            // ERROR [ee.sk.digidoc.factory.BouncyCastleNotaryFactory] - <Certificate has been revoked!>
            // And the following exception is thrown:
            // DigiDocException.code: 88
            // DigiDocException.message: Certificate has been revoked!

            // If certificate is unknown, then the following error is logged:
            // ERROR [ee.sk.digidoc.factory.BouncyCastleNotaryFactory] - <Certificate status is unknown!>
            // And the following exception is thrown:
            // DigiDocException.code: 88
            // DigiDocException.message: Certificate status is unknown!

            // If OCSP server refuses request (for example there is no access allowed from this IP, or PKCS12 (juurdepääsutõend) is expired),
            // then the following error is logged:
            // ERROR [ee.sk.digidoc.factory.BouncyCastleNotaryFactory] - <The server could not authenticate you!>
            // And the following exception is thrown:
            // DigiDocException.code: 69
            // DigiDocException.message: OCSP response unsuccessfull!

            // If connection to OCSP server fails, then the following error is logged:
            // ERROR [ee.sk.digidoc.DigiDocException] - <java.net.ConnectException: Connection refused>
            // And the following exception is thrown:
            // DigiDocException.code: 65
            // DigiDocException.message: ERROR: 65java.net.ConnectException; nested exception is:
            //      java.net.ConnectException: Connection refused

            // If connection to OCSP server fails, then the following error is logged:
            // ERROR [ee.sk.digidoc.DigiDocException] - <java.net.ConnectException: Connection timed out>
            // And the following exception is thrown:
            // DigiDocException.code: 65
            // DigiDocException.message: ERROR: 65java.net.ConnectException; nested exception is:
            //      java.net.ConnectException: Connection timed out

            if (log.isInfoEnabled()) {
                log.info("authentication OK; SSL client authentication data meets criteria for cert[" + certificate.getSerialNumber().toString() + "]");
            }
            x509Credentials.setCertificate(certificate);
            return true;

        } catch (final CertificateExpiredException e) {
            log.warn("authentication failed; certficiate expired [" + certificate.toString() + "]");
        } catch (final CertificateNotYetValidException e) {
            log.warn("authentication failed; certficate not yet valid [" + certificate.toString() + "]");
        } catch (DigiDocException e) {
            log.warn("authentication failed; OCSP certificate confirmation failed\nError code: " + e.getCode() + "\nError message: " + e.getMessage() + "\n["
                    + certificate.toString() + "]");
        }
        if (log.isInfoEnabled()) {
            log.info("authentication failed; SSL client authentication data doesn't meet criteria");
        }
        return false;
    }

    public boolean supports(final Credentials credentials) {
        return credentials != null && X509CertificateCredentials.class.isAssignableFrom(credentials.getClass());
    }

    public void setOcspEnabled(boolean ocspEnabled) {
        this.ocspEnabled = ocspEnabled;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isTest() {
        return test;
    }

    public void setPkcs12Container(String pkcs12Container) {
        this.pkcs12Container = pkcs12Container;
    }

    public void setPkcs12Password(String pkcs12Password) {
        this.pkcs12Password = pkcs12Password;
    }

    public void setPkcs12CertSerial(String pkcs12CertSerial) {
        this.pkcs12CertSerial = pkcs12CertSerial;
    }

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

}
