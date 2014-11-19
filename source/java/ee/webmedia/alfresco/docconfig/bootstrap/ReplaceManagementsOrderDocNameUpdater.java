package ee.webmedia.alfresco.docconfig.bootstrap;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;

/**
 * This updater should run only in SIM 3.13 environment to fix erroneous classificator assignment (doc. type managementsOrder field docName should have
 * classificator managementsOrderDocName instead of managementsOrderDocName123) during 2.5 -> 3.13 migration. See cl task 215710 for details.
 */
public class ReplaceManagementsOrderDocNameUpdater extends AbstractNodeUpdater {

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() {
        DocumentType documentType = BeanHelper.getDocumentAdminService().getDocumentType("managementsOrder", null);
        Set<NodeRef> docNameFieldRefs = new HashSet<NodeRef>();
        Set<String> fieldNames = new HashSet<String>(1);
        fieldNames.add("docName");
        if (documentType != null) {
            for (DocumentTypeVersion documentTypeVersion : documentType.getDocumentTypeVersions()) {
                Collection<Field> fields = documentTypeVersion.getFieldsById(fieldNames);
                for (Field field : fields) {
                    docNameFieldRefs.add(field.getNodeRef());
                }
            }
        }
        return docNameFieldRefs;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String classificator = (String) nodeService.getProperty(nodeRef, DocumentAdminModel.Props.CLASSIFICATOR);
        if ("managementsOrderDocName123".equals(classificator)) {
            nodeService.setProperty(nodeRef, DocumentAdminModel.Props.CLASSIFICATOR, "managementsOrderDocName");
            return new String[] { "Updated" };
        }
        return new String[] { "Value is " + classificator + ", not updating" };
    }

}
