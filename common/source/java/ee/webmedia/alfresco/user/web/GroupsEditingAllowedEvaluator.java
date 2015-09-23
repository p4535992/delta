package ee.webmedia.alfresco.user.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class GroupsEditingAllowedEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        return BeanHelper.getApplicationConstantsBean().isGroupsEditingAllowed();
    }

}
