package ee.webmedia.alfresco.docconfig.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.dvk.service.DecContainerHandler;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * This updater removes all disallowed elements from {@code DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT} property value in {@link Field} and {@link FieldDefinition} nodes
 */
public class RelatedDecElementDefaultValuesUpdater3 extends RelatedDecElementDefaultValuesUpdater2 {

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> relatedOutgoingDecElements = (List<String>) nodeService.getProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT);
        if (CollectionUtils.isNotEmpty(relatedOutgoingDecElements)) {
            List<String> updatedOutgoingDecElements = new ArrayList<String>();
            for (String element : relatedOutgoingDecElements) {
                element = StringUtils.trim(element);
                if (StringUtils.isBlank(element) || DecContainerHandler.isDisallowedOutgoingUserElement(element)) {
                    continue;
                }
                updatedOutgoingDecElements.add(element);
            }
            if (updatedOutgoingDecElements.size() > 0) {
                nodeService.setProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT, (Serializable) updatedOutgoingDecElements);
            } else {
                nodeService.removeProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT);
            }
            boolean updated = CollectionUtils.isEqualCollection(relatedOutgoingDecElements, updatedOutgoingDecElements);
            return new String[] { TextUtil.joinNonBlankStrings(relatedOutgoingDecElements, ", "), updated ? TextUtil.joinNonBlankStrings(updatedOutgoingDecElements, ", ") : "null" };
        }
        return null;
    }

}
