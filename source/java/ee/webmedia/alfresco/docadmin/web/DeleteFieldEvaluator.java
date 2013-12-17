package ee.webmedia.alfresco.docadmin.web;

import java.util.List;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;

/**
 * @author Riina Tens
 */
public class DeleteFieldEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        return evaluate((FieldDefinition) obj);
    }

    private boolean evaluate(FieldDefinition fieldDefinition) {
        return isEmpty(fieldDefinition.getDocTypes()) && isEmpty(fieldDefinition.getVolTypes()) && !fieldDefinition.isSystematic() && !isUsedOnDocTypeVersion(fieldDefinition);
    }

    private boolean isUsedOnDocTypeVersion(FieldDefinition fieldDefinition) {
        String fieldId = fieldDefinition.getFieldId();
        if (StringUtils.isBlank(fieldId)) {
            // shouldn't happen
            return false;
        }
        return BeanHelper.getDocumentSearchService().isFieldByOriginalIdExists(fieldId);
    }

    private boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }

}
