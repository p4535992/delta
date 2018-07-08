package ee.smit.alfresco.visual;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class VisualServiceImpl implements VisualService {
    private static final Log log = LogFactory.getLog(VisualServiceImpl.class);

    private List<String> usernameList = new ArrayList<>();

    private String userName;


    public Boolean isVisualUserName(){
        log.trace("Check, if user is Visual...");
        Boolean isVisualUser = false;
        log.trace("isVisualUser: " + isVisualUser);

        try{

            userName = AuthenticationUtil.getRunAsUser();

            log.debug("Active USERNAME: " + userName);

            if(usernameList != null){
                log.trace("VisualUsernameList is not empty.");
                for(String visualUsername: usernameList){
                    log.trace("Visual username: " + visualUsername);
                    if(visualUsername.equals(userName)){
                        log.trace("Found match:: set true... and break...");
                        isVisualUser = true;
                        break;
                    }
                }
            }


        } catch(Exception e){
            log.error("Can't get active username: " + e.getMessage(), e);
        }

        log.trace("isVisualUser: " + isVisualUser);
        return isVisualUser;

    }

    /** GETTERS, SETTERS */
    public void setVisualUsernames(String visualUsernames) {
        try{
            if(visualUsernames != null){
                if(visualUsernames.contains(",")){
                    log.debug("Visual username string contains [,] -- split...");
                    String[] tempUsernames = visualUsernames.split(",");

                    for(String username : tempUsernames){
                        log.debug("VisualUsername: " + username);
                        if(username == null){
                            continue;
                        }
                        String trimmedUser = username.trim();
                        this.usernameList.add(trimmedUser);
                    }

                } else {
                    log.debug("Visual username string is not null: " + visualUsernames);
                    this.usernameList.add(visualUsernames);
                }
            } else {
                log.debug("Visual username string is NULL...");
            }

        }catch(Exception e){
            log.warn("VISUAL USERNAMES SET ERROR: " + e.getMessage(), e);
        }
        log.debug("SET VISUAL USERNAME...DONE!");
    }

    public List<String> getUsernamesList() {
        return usernameList;
    }


}
