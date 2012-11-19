package ee.webmedia.alfresco.substitute.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getApplicationService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getMenuBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstituteService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

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
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Bean for handling substitution selection.
 * 
 * @author Romet Aidla
 */
public class SubstitutionBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "SubstitutionBean";
    private SubstitutionInfo substitutionInfo = new SubstitutionInfo();
    private boolean forceSubstituteTaskReload = false;
    private Boolean currentStructUnitUser;

    public String getSelectedSubstitution() {
        return substitutionInfo.getSelectedSubstitution();
    }

    public void setSelectedSubstitution(String selectedSubstitution) {
        currentStructUnitUser = null;
        if (StringUtils.isBlank(selectedSubstitution)) {
            substitutionInfo = new SubstitutionInfo();
        } else {
            NodeRef userNodeRef = new NodeRef(selectedSubstitution);
            substitutionInfo = new SubstitutionInfo(getSubstituteService().getSubstitute(userNodeRef));
        }
        setForceSubstituteTaskReload(true);
    }

    public boolean isCurrentStructUnitUser() {
        if (currentStructUnitUser == null) {
            currentStructUnitUser = BeanHelper.getUserService().isCurrentStructUnitUser();
        }
        return currentStructUnitUser;
    }

    public void substitutionSelected(ValueChangeEvent event) {
        String substitutionNodeRef = (String) event.getNewValue();
        setSelectedSubstitution(substitutionNodeRef);
        redirectToHome(getApplicationService().getServerUrl());
    }

    private static void redirectToHome(String serverUrl) {
        FacesContext fc = FacesContext.getCurrentInstance();

        MenuBean.clearViewStack(String.valueOf(MenuBean.MY_TASKS_AND_DOCUMENTS_ID), null);
        getMenuBean().reset();

        WebUtil.navigateTo("myalfresco", fc);
        fc.responseComplete();
        try {
            // todo: find better solution
            String redir = serverUrl +
                    ((HttpServletRequest) fc.getExternalContext().getRequest()).getContextPath() +
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
        for (int i = 0; i < substitutions.size(); i++) {
            Substitute subs = substitutions.get(i);
            if (i == 0) {
                builder.append("<div class=\"message message-red\">");
            }
            builder.append(MessageUtil.getMessage(FacesContext.getCurrentInstance(), "substitution_message",
                    getUserService().getUserFullName(subs.getReplacedPersonUserName()),
                    dateFormat.format(subs.getSubstitutionStartDate()),
                    dateFormat.format(subs.getSubstitutionEndDate())));
            if (i != substitutions.size() - 1) {
                builder.append("<br/>");
            } else {
                builder.append("</div>");
            }
        }
        return builder.toString();
    }

    public String getOnChangeStyleClass() {
        return GeneralSelectorGenerator.ONCHANGE_MARKER_CLASS
                + GeneralSelectorGenerator.ONCHANGE_SCRIPT_START_MARKER
                + "var el = document.getElementById(currElId); el.form.submit();";
    }

    public void setForceSubstituteTaskReload(boolean force) {
        forceSubstituteTaskReload = force;
    }

    public boolean getForceSubstituteTaskReload() {
        return forceSubstituteTaskReload;
    }

    public SubstitutionInfo getSubstitutionInfo() {
        return substitutionInfo;
    }

}
