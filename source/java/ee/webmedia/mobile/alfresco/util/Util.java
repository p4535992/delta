package ee.webmedia.mobile.alfresco.util;

import java.util.Arrays;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.ui.repo.component.evaluator.PermissionEvaluator;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

@Component
public class Util {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Util.class);

    private Util() {
        // Disable instantiation from outside of this class
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> T[] toArray(T... items) {
        if (items == null || items.length < 1) {
            return (T[]) new Object[0];
        }

        return items;
    }

    public static String translate(MessageSource messages, String translationKey, Object... placeholderValues) {
        if (messages == null) {
            LOG.error("Provided messageSource is null! \n" + Arrays.toString(Thread.currentThread().getStackTrace()));
            return translationKey;
        }

        return messages.getMessage(translationKey, placeholderValues, AppConstants.getDefaultLocale());
    }

    public static boolean documentAllowPermission(NodeRef nodeRef, String allowPermission) {
        return PermissionEvaluator.evaluatePermissions(true, BeanHelper.getGeneralService().getAncestorNodeRefWithType(nodeRef, DocumentCommonModel.Types.DOCUMENT, true, false),
                new String[] { allowPermission }, new String[0]);
    }

    public static boolean documentDenyPermission(NodeRef nodeRef, String denyPermission) {
        return PermissionEvaluator.evaluatePermissions(true, BeanHelper.getGeneralService().getAncestorNodeRefWithType(nodeRef, DocumentCommonModel.Types.DOCUMENT, true, false),
                new String[0], new String[] { denyPermission });
    }
}
