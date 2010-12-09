package ee.webmedia.alfresco.substitute.web;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.BaseServlet;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import ee.webmedia.alfresco.substitute.service.SubstituteService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Bean for handling substitution selection.
 *
 * @author Romet Aidla
 */
public class SubstitutionBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "SubstitutionBean";
    private transient SubstituteService substituteService;
    private transient ApplicationService applicationService;
    private transient UserService userService;
    private transient MenuService menuService;
    private SubstitutionInfo substitutionInfo = new SubstitutionInfo();
    private boolean forceSubstituteTaskReload = false;

    public String getSelectedSubstitution() {
        return substitutionInfo.getSelectedSubstitution();
    }    

    public void setSelectedSubstitution(String selectedSubstitution) {
        if (StringUtils.isBlank(selectedSubstitution)) {
            substitutionInfo = new SubstitutionInfo();
        }
        else {
            NodeRef userNodeRef = new NodeRef(selectedSubstitution);
            substitutionInfo = new SubstitutionInfo(getSubstituteService().getSubstitute(userNodeRef));
        }
        setSubstitutionInfo(substitutionInfo);    
        setForceSubstituteTaskReload(true);
    }

    public void substitutionSelected(ValueChangeEvent event) {
        String substitutionNodeRef = (String) event.getNewValue();
        setSelectedSubstitution(substitutionNodeRef);
        redirectToHome(getApplicationService().getServerUrl());
    }


    private static void redirectToHome(String serverUrl) {
        FacesContext fc = FacesContext.getCurrentInstance();
        
        MenuBean.clearViewStack(String.valueOf(MenuBean.MY_TASKS_AND_DOCUMENTS_ID), null);
        MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(fc, MenuBean.BEAN_NAME);   
        menuBean.reset(); 
        
        fc.getApplication().getNavigationHandler().handleNavigation(fc, null, "myalfresco");
        fc.responseComplete();
        try {
            //todo: find better solution
            String redir =  serverUrl +  
            ((HttpServletRequest)fc.getExternalContext().getRequest()).getContextPath() +
            BaseServlet.FACES_SERVLET + fc.getViewRoot().getViewId();
            fc.getExternalContext().redirect(redir);
            fc.responseComplete();
        } catch (IOException ioe) {
            throw new RuntimeException("Redirecting failed", ioe);
        }        
    }

    public List<SelectItem> getActiveSubstitutions() {
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        List<Substitute> substitutions = getSubstituteService().findActiveSubstitutionDuties(Application.getCurrentUser(FacesContext.getCurrentInstance()).getUserName());
        for (Substitute substitution : substitutions) {
            selectItems.add(new SelectItem(substitution.getNodeRef().toString(),
                    getUserService().getUserFullName(substitution.getReplacedPersonUserName())));
        }
        return selectItems;
    }

    public String getSubstitutionMessages() {
        List<Substitute> substitutions = getSubstituteService().findActiveSubstitutionDuties(AuthenticationUtil.getRunAsUser());
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        StringBuilder builder = new StringBuilder();
        for (Substitute subs : substitutions) {
            builder.append("<div class=\"message message-red\">");
            builder.append(MessageUtil.getMessage(FacesContext.getCurrentInstance(), "substitution_message",
                    getUserService().getUserFullName(subs.getReplacedPersonUserName()),
                    dateFormat.format(subs.getSubstitutionStartDate()),
                    dateFormat.format(subs.getSubstitutionEndDate())));
            builder.append("</div>");
        }
        return builder.toString();
    }

    protected SubstituteService getSubstituteService() {
        if (substituteService == null) {
            this.substituteService = (SubstituteService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), SubstituteService.BEAN_NAME);
        }
        return substituteService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            this.userService = (UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME);
        }
        return userService;
    }

    protected MenuService getMenuService() {
        if (menuService == null) {
            this.menuService = (MenuService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuService.BEAN_NAME); 
        }
        return menuService;
    }
    
    public String getOnChangeStyleClass(){
        return GeneralSelectorGenerator.ONCHANGE_MARKER_CLASS 
                + GeneralSelectorGenerator.ONCHANGE_SCRIPT_START_MARKER 
                + "var el = document.getElementById(currElId); el.form.submit();";
    }
    
    public void setForceSubstituteTaskReload(boolean force) {
        forceSubstituteTaskReload = force;
    }
    
    public boolean getForceSubstituteTaskReload(){
        return forceSubstituteTaskReload;
    }

    private void setSubstitutionInfo(SubstitutionInfo substitutionInfo) {
        this.substitutionInfo = substitutionInfo;
    }
    
    public SubstitutionInfo getSubstitutionInfo() {
        return substitutionInfo;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public ApplicationService getApplicationService() {
        return applicationService;
    }     

}
