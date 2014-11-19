<<<<<<< HEAD
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
=======
package ee.webmedia.alfresco.privilege.web;

import org.alfresco.web.ui.repo.tag.evaluator.PermissionEvaluatorTag;

/**
 * Evaluates permissions on ancestor document
 */
public class DocPermissionEvaluatorTag extends PermissionEvaluatorTag {

    @Override
    public String getComponentType() {
        return DocPermissionEvaluator.class.getCanonicalName();
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
