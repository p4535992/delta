package ee.webmedia.alfresco.substitute;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.SessionContext;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * Phase listener that sets {@link org.alfresco.repo.security.authentication.AuthenticationUtil#setRunAsUser(String)} to selected substitution,
 * if substitution is selected.
 *
 * @author Romet Aidla
 */
public class SubstitutionInfoPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SubstitutionInfoPhaseListener.class);

    public void afterPhase(PhaseEvent event)
   {
      // do nothing
   }

   public void beforePhase(PhaseEvent event)
   {
       setRunAsSubstitution();
   }
   
   public static void setRunAsSubstitution() {
       SessionContext sessionContext = (SessionContext) FacesContextUtils.getRequiredWebApplicationContext( //
               FacesContext.getCurrentInstance()).getBean(SessionContext.BEAN_NAME);        
       SubstitutionInfo subInfo = sessionContext.getSubstitutionInfo();
       if (subInfo.isSubstituting()) {
           String substitutionUserName = subInfo.getSubstitution().getReplacedPersonUserName();
           AuthenticationUtil.setRunAsUser(substitutionUserName);
       }
       else{
           AuthenticationUtil.setRunAsUser(AuthenticationUtil.getFullyAuthenticatedUser());
       }
   }    

   public PhaseId getPhaseId()
   {
      return PhaseId.RESTORE_VIEW;
   }
}
