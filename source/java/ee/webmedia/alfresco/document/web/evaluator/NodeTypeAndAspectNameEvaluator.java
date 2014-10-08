<<<<<<< HEAD
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.config.evaluator.Evaluator;
import org.alfresco.web.config.AspectEvaluator;
import org.alfresco.web.config.NodeTypeEvaluator;
import org.apache.commons.lang.StringUtils;

/**
 * Used for complex evaluation where both node type and aspect name must match for given object.
 * Condition is string, where node type and aspect name are separated with a comma and are given in correct order.
 * 
 * @author Kaarel Jõgeva
 *
 */
public class NodeTypeAndAspectNameEvaluator implements Evaluator {

    @Override
    public boolean applies(Object obj, String condition) {
        String[] split = StringUtils.split(condition, '|');
        if(split.length != 2) {
            return false;
        }
        
        return (new NodeTypeEvaluator()).applies(obj, split[0]) && (new AspectEvaluator()).applies(obj, split[1]);
    }

}
=======
package ee.webmedia.alfresco.document.web.evaluator;

import org.alfresco.config.evaluator.Evaluator;
import org.alfresco.web.config.AspectEvaluator;
import org.alfresco.web.config.NodeTypeEvaluator;
import org.apache.commons.lang.StringUtils;

/**
 * Used for complex evaluation where both node type and aspect name must match for given object.
 * Condition is string, where node type and aspect name are separated with a comma and are given in correct order.
 *
 */
public class NodeTypeAndAspectNameEvaluator implements Evaluator {

    @Override
    public boolean applies(Object obj, String condition) {
        String[] split = StringUtils.split(condition, '|');
        if(split.length != 2) {
            return false;
        }
        
        return (new NodeTypeEvaluator()).applies(obj, split[0]) && (new AspectEvaluator()).applies(obj, split[1]);
    }

}
>>>>>>> develop-5.1
