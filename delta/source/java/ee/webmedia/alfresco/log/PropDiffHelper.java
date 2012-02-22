package ee.webmedia.alfresco.log;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Helper class for working with node properties for logging: retrieving the changes or just rendering the properties with values. This helper works over properties under watch,
 * i.e. explicitly defined through {@link #label(QName, String)}.
 * <p>
 * Sample usage:
 * 
 * <pre>
 * String diff = new PropDiffHelper()
 *         .label(prop, &quot;label_to_localize&quot;)
 *         // ... other properties to watch with labels
 *         .diff(mapOfCurrentProperties, mapOfNewProperties);
 * </pre>
 * 
 * @author Martti Tamm
 */
public class PropDiffHelper {

    private final Map<QName, String> propLabels = new HashMap<QName, String>();

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");

    public PropDiffHelper label(QName prop, String label) {
        propLabels.put(prop, label);
        return this;
    }

    public PropDiffHelper watchAccessRights() {
        propLabels.put(DocumentCommonModel.Props.ACCESS_RESTRICTION, "doccom_documentCommonModel.property.doccom_accessRestriction.title");
        propLabels.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, "doccom_documentCommonModel.property.doccom_accessRestrictionReason.title");
        propLabels.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON, "doccom_documentCommonModel.property.doccom_accessRestrictionReason.title");
        propLabels.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, "doccom_documentCommonModel.property.doccom_accessRestrictionBeginDate.title");
        propLabels.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, "doccom_documentCommonModel.property.doccom_accessRestrictionEndDate.title");
        propLabels.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, "doccom_documentCommonModel.property.doccom_accessRestrictionEndDesc.title");
        return this;
    }

    public PropDiffHelper watchUser() {
        propLabels.put(ContentModel.PROP_FIRSTNAME, "cm_contentmodel.property.cm_firstName.title");
        propLabels.put(ContentModel.PROP_LASTNAME, "cm_contentmodel.property.cm_lastName.title");
        propLabels.put(ContentModel.PROP_USERNAME, "user_username");
        propLabels.put(ContentModel.PROP_JOBTITLE, "jobtitle");
        propLabels.put(ContentModel.PROP_SERVICE_RANK, "user_serviceRank");
        propLabels.put(ContentModel.PROP_TELEPHONE, "telephone");
        propLabels.put(ContentModel.PROP_EMAIL, "user_email");
        propLabels.put(ContentModel.PROP_HOMEFOLDER, "homeFolder");
        propLabels.put(ContentModel.PROP_HOMEFOLDER, "user_home_folder");
        propLabels.put(ContentModel.SHOW_EMPTY_TASK_MENU, "user_showEmptyTaskMenu");
        propLabels.put(ContentModel.PROP_RELATED_FUNDS_CENTER, "user_relatedFundsCenter");
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String diff(Map<QName, Serializable> oldProps, Map<QName, Serializable> newProps) {
        StringBuilder sb = new StringBuilder();
        String emptyMsg = label("applog_empty");

        for (QName prop : propLabels.keySet()) {
            Serializable oldValue = oldProps.get(prop);
            Serializable newValue = newProps.get(prop);

            boolean collection = oldValue instanceof Collection || newValue instanceof Collection;
            boolean changed = false;

            if (collection) {
                if (oldValue == null || newValue == null) {
                    changed = true;
                } else {
                    Collection oldColl = (Collection) oldValue;
                    Collection newColl = (Collection) newValue;
                    changed = oldColl.size() != newColl.size() || !newColl.containsAll(oldColl) || !oldColl.containsAll(newColl);
                }
            } else if (newValue != null) {
                changed = oldValue == null || !newValue.equals(oldValue);
            }

            if (!changed) {
                continue;
            }

            String label = label(propLabels.get(prop));

            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(label("applog_prop_change", label, value(oldValue, emptyMsg), value(newValue, emptyMsg)));
        }

        return sb.length() == 0 ? null : sb.toString();
    }

    @SuppressWarnings("rawtypes")
    public String toString(Map<QName, Serializable> props) {
        StringBuilder sb = new StringBuilder();

        for (QName prop : propLabels.keySet()) {
            Serializable value = props.get(prop);
            if (value == null || value instanceof Collection && ((Collection) value).isEmpty()) {
                continue;
            }
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(label(propLabels.get(prop))).append(": ").append(value);
        }

        return sb.toString();
    }

    private static String label(String msg, Object... params) {
        String result = I18NUtil.getMessage(msg, params);
        if (result == null) {
            result = MessageUtil.getMessage(msg, params);
        }
        return result;
    }

    private static String value(Object value, String defaultStr) {
        if (value instanceof Date) {
            return DATE_FORMAT.format((Date) value);
        }
        return value == null ? defaultStr : value.toString();
    }
}
