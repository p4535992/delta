package ee.webmedia.alfresco.dvk.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

/**
 * Evaluator that returns true if logger ee.webmedia.developer is set to debug, <br>
 * for example using log4j.properties: <code>log4j.logger.ee.webmedia.developer=debug</code>
 * 
 * @author Ats Uiboupin
 */
public class DevelopmentEvaluator extends BaseActionEvaluator {
    private static final org.apache.commons.logging.Log developerLog = org.apache.commons.logging.LogFactory.getLog("ee.webmedia.developer");
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object node) {
        return developerLog.isDebugEnabled();
    }

    @Override
    public boolean evaluate(Node node) {
        return evaluate((Object) node);
    }

}
