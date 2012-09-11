package ee.webmedia.alfresco.thesaurus.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;

/**
 * @author Kaarel Jõgeva
 */
public class ThesaurusDeleteEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        throw new RuntimeException("method evaluate(Node node) is unimplemented");
    }

    @Override
    public boolean evaluate(Object obj) {
        if (obj == null) {
            return false;
        }
        return !BeanHelper.getThesaurusService().isThesaurusUsed(((Thesaurus) obj).getName());
    }

}
