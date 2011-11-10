package ee.webmedia.alfresco.docconfig.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.module.ModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.common.bootstrap.ImporterModuleComponent;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * @author Alar Kvell
 */
public class SystematicDocumentTypesBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SystematicDocumentTypesBootstrap.class);

    private GeneralService generalService;
    private DocumentAdminService documentAdminService;
    private ImporterBootstrap importer;

    @Override
    protected void executeInternal() throws Throwable {
        executeInternalImpl();
    }

    public void executeInternalImpl() {
        Map<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentTypes = new HashMap<String, Pair<String, Pair<Set<String>, Set<QName>>>>();

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.INCOMING_LETTER.getId(), "Sissetulev kiri",
                new String[] {
                        SystematicFieldGroupNames.SENDER_NAME_AND_EMAIL,
                        SystematicFieldGroupNames.SENDER_REG_NUMBER_AND_DATE,
                        SystematicFieldGroupNames.COMPLIENCE
        },
                new QName[] {
                        DocumentSpecificModel.Props.TRANSMITTAL_MODE,
                        DocumentSpecificModel.Props.DUE_DATE,
        });

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.OUTGOING_LETTER.getId(), "Väljaminev kiri",
                new String[] {
                        SystematicFieldGroupNames.SENDER_REG_NUMBER_AND_DATE,
                        SystematicFieldGroupNames.SIGNER,
                        SystematicFieldGroupNames.RECIPIENTS,
                        SystematicFieldGroupNames.ADDITIONAL_RECIPIENTS
         },
                null);

        // addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.INVOICE.getId(), "Arve", ..., ...);

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.TRAINING_APPLICATION.getId(), "Koolitustaotlus", null, null);

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.CONTRACT.getId(), "Leping", null, null);

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.VACATION_APPLICATION.getId(), "Puhkuse taotlus",
                new String[] { SystematicFieldGroupNames.SUBSTITUTE }, null);

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.ERRAND_ORDER_ABROAD.getId(), "Välislähetuse korraldus", null, null);

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.REPORT.getId(), "Aruanne", null, null);

        createSystematicDocumentTypes(systematicDocumentTypes);
    }

    private void addSystematicDocumentType(Map<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentTypes, String documentTypeId, String documentTypeName,
            String[] fieldGroupDefinitionNamesArray, QName[] fieldDefinitionIdsArray) {
        Set<String> fieldGroupDefinitionNames = fieldGroupDefinitionNamesArray == null ? new HashSet<String>() :
                new HashSet<String>(Arrays.asList(fieldGroupDefinitionNamesArray));
        Set<QName> fieldDefinitionIds = fieldDefinitionIdsArray == null ? new HashSet<QName>() :
                new HashSet<QName>(Arrays.asList(fieldDefinitionIdsArray));
        systematicDocumentTypes.put(
                documentTypeId,
                new Pair<String, Pair<Set<String>, Set<QName>>>(documentTypeName,
                        new Pair<Set<String>, Set<QName>>(fieldGroupDefinitionNames, fieldDefinitionIds)));
    }

    private void createSystematicDocumentTypes(Map<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentTypes) {

        systematicDocumentTypes.keySet().retainAll(documentAdminService.getNonExistingDocumentTypes(systematicDocumentTypes.keySet()));
        if (systematicDocumentTypes.isEmpty()) {
            LOG.info("All systematic document types are already imported");
            return;
        }
        LOG.info("Following systematic document types are not yet imported: " + systematicDocumentTypes.keySet());

        LOG.info("Importing systematicFieldDefinitions and systematicFieldDefinitions into temporary locations");
        NodeRef rootRef = serviceRegistry.getNodeService().getRootNode(generalService.getStore());
        NodeRef fieldGroupDefinitionsTmp = deleteAndCreateNode(rootRef, DocumentAdminModel.Assocs.FIELD_GROUP_DEFINITIONS_TMP, DocumentAdminModel.Types.FIELD_GROUP_DEFINITIONS);
        NodeRef fieldDefinitionsTmp = deleteAndCreateNode(rootRef, DocumentAdminModel.Assocs.FIELD_DEFINITIONS_TMP, DocumentAdminModel.Types.FIELD_DEFINITIONS);

        List<Properties> views = new ArrayList<Properties>();
        for (ModuleComponent component : getDependsOn()) {
            if (!(component instanceof ImporterModuleComponent)) {
                continue;
            }
            List<Properties> bootstrapViews = ((ImporterModuleComponent) component).getBootstrapViews();
            for (Properties properties : bootstrapViews) {
                String path = properties.getProperty("path");
                if (DocumentAdminModel.Repo.FIELD_DEFINITIONS_SPACE.equals(path)) {
                    path = DocumentAdminModel.Repo.FIELD_DEFINITIONS_TMP_SPACE;
                } else if (DocumentAdminModel.Repo.FIELD_GROUP_DEFINITIONS_SPACE.equals(path)) {
                    path = DocumentAdminModel.Repo.FIELD_GROUP_DEFINITIONS_TMP_SPACE;
                } else if (!DocumentAdminModel.Repo.FIELD_DEFINITIONS_TMP_SPACE.equals(path) && !DocumentAdminModel.Repo.FIELD_GROUP_DEFINITIONS_TMP_SPACE.equals(path)) {
                    continue;
                }
                properties.setProperty("path", path);
            }
            views.addAll(bootstrapViews);
        }
        importer.setBootstrapViews(views);
        importer.setUseExistingStore(true);
        importer.bootstrap();

        documentAdminService.createSystematicDocumentTypes(systematicDocumentTypes, fieldGroupDefinitionsTmp, fieldDefinitionsTmp);

        LOG.info("Deleting temporary locations for systematicFieldDefinitions and systematicFieldDefinitions");
        serviceRegistry.getNodeService().deleteNode(fieldGroupDefinitionsTmp);
        serviceRegistry.getNodeService().deleteNode(fieldDefinitionsTmp);
    }

    private NodeRef deleteAndCreateNode(NodeRef parentRef, QName assocQName, QName typeQName) {
        NodeRef nodeRef = generalService.getChildByAssocName(parentRef, assocQName);
        if (nodeRef != null) {
            serviceRegistry.getNodeService().deleteNode(nodeRef);
        }
        return serviceRegistry.getNodeService().createNode(parentRef, ContentModel.ASSOC_CHILDREN, assocQName, typeQName).getChildRef();
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setImporter(ImporterBootstrap importer) {
        this.importer = importer;
    }

}
