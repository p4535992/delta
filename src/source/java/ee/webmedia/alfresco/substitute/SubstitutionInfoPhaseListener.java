package ee.webmedia.alfresco.substitute;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
       SubstitutionBean subBean = (SubstitutionBean) FacesHelper.getManagedBean(event.getFacesContext(), SubstitutionBean.BEAN_NAME);
       SubstitutionInfo subInfo = subBean.getSubstitutionInfo();
       SubstitutionInfoHolder.setSubstitutionInfo(subInfo);
       if (subInfo.isSubstituting()) {
           String substitutionUserName = subInfo.getSubstitution().getReplacedPersonUserName();
           if (log.isDebugEnabled()) log.debug("Set RunAsUser to " + substitutionUserName);
           AuthenticationUtil.setRunAsUser(substitutionUserName);
       }
   }

   public PhaseId getPhaseId()
   {
      return PhaseId.RESTORE_VIEW;
   }
}
