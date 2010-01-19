package ee.webmedia.alfresco.orgstructure.amr;


/**
 * Used to notify {@link AMRAuthenticationFilter} about exception that might occur during authentication 
 * 
 * @author Ats Uiboupin
 */
public class AMRAuthenticationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AMRAuthenticationException(String msgId, Throwable cause) {
        super(msgId, cause);
    }
}
