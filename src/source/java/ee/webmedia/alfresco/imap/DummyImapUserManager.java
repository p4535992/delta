package ee.webmedia.alfresco.imap;

import com.icegreen.greenmail.user.GreenMailUser;
import org.alfresco.repo.imap.AlfrescoImapUser;
import org.alfresco.repo.imap.AlfrescoImapUserManager;
import org.alfresco.repo.security.authentication.AuthenticationException;

/**
 * TODO: should be removed
 *
 * @author Romet Aidla
 */
public class DummyImapUserManager extends AlfrescoImapUserManager {

public boolean test(String userid, String password)
    {
        try
        {
            authenticationService.authenticate(userid, password.toCharArray());
            GreenMailUser user = new AlfrescoImapUser("romet.aidla@webmedia.ee", userid, password);
            addUser(user);
        }
        catch (AuthenticationException ex)
        {
            return false;
        }
        return true;
    }

}
