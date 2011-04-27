package ee.webmedia.alfresco.privilege.web;

import org.alfresco.web.ui.repo.tag.evaluator.PermissionEvaluatorTag;

/**
 * Evaluates permissions on ancestor document
 * 
 * @author Ats Uiboupin
 */
public class DocPermissionEvaluatorTag extends PermissionEvaluatorTag {

    @Override
    public String getComponentType() {
        return DocPermissionEvaluator.class.getCanonicalName();
    }

}
