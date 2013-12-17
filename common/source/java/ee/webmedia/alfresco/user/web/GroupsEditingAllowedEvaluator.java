package ee.webmedia.alfresco.user.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.user.service.UserService;

public class GroupsEditingAllowedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        UserService userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        return userService.isGroupsEditingAllowed();
    }

}
