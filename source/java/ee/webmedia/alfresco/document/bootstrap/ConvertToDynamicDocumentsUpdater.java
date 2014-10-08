package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.DOCSPEC_URI;
import static ee.webmedia.alfresco.utils.DynamicTypeUtil.setTypeProps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docconfig.service.PropDefCacheKey;
import ee.webmedia.alfresco.docdynamic.bootstrap.DocumentAccessRestrictionUpdater;
import ee.webmedia.alfresco.docdynamic.bootstrap.DocumentChangedTypePropertiesUpdater;
import ee.webmedia.alfresco.docdynamic.bootstrap.DocumentCompWorkflowSearchPropsUpdater;
import ee.webmedia.alfresco.docdynamic.bootstrap.DocumentPartyPropsUpdater;
import ee.webmedia.alfresco.docdynamic.bootstrap.DocumentUpdater;
import ee.webmedia.alfresco.docdynamic.bootstrap.LogAndDeleteObjectsWithMissingType;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.register.model.RegNrHolder;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.DynamicTypeUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.TreeNode;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.bootstrap.LogAndDeleteNotExistingWorkflowTasks;
import ee.webmedia.alfresco.workflow.bootstrap.TaskUpdater;
import ee.webmedia.alfresco.workflow.bootstrap.Workflow25To313DynamicDocTypeUpdater;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Convert static documents in 2.5 branch to dynamic documents in 3.13 branch.
 */
public class ConvertToDynamicDocumentsUpdater extends AbstractNodeUpdater {

    private static final QName NOT_EDITABLE = QName.createQName(DOCSPEC_URI, "notEditable");
    private static final QName INVOICE_XML = QName.createQName(DOCSPEC_URI, "invoiceXml");

    private static final QName DAILY_ALLOWANCE_FINANCING_SOURCE = QName.createQName(DOCSPEC_URI, "dailyAllowanceFinancingSource");
    private static final QName EXPENSE_DESC = QName.createQName(DOCSPEC_URI, "expenseDesc");
    private static final QName EXPENSE_FINANCING_SOURCE = QName.createQName(DOCSPEC_URI, "expenseFinancingSource");

    /** DocumentCommonModel.Aspects.ACCESS_RIGHTS is needed for series, so it cannot be deleted from model even after this updater has converted all documents */
    private static final Set<QName> ASPECTS_TO_REMOVE = new HashSet<QName>(Arrays.asList(DocumentCommonModel.Aspects.SEND_DESC, DocumentCommonModel.Aspects.OWNER,
            DocumentCommonModel.Aspects.ACCESS_RIGHTS, DocumentCommonModel.Aspects.SIGNER, DocumentCommonModel.Aspects.SIGNER_NAME,
            DocumentCommonModel.Aspects.RECIPIENT, DocumentCommonModel.Aspects.ADDITIONAL_RECIPIENT, DocumentCommonModel.Aspects.EMAIL_DATE_TIME, NOT_EDITABLE,
            DocumentCommonModel.Aspects.COMMON, ContentModel.ASPECT_OWNABLE, PrivilegeModel.Aspects.USER_GROUP_MAPPING));

    private static final Set<QName> CONVERTABLE_CHILD_QNAMES = new HashSet<QName>(Arrays.asList(DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE,
            DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE, DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2,
            DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD, DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2,
            DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV,
            DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE, DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE_V2));

    private static final Set<QName> PROPS_TO_OMIT = new HashSet<QName>(Arrays.asList(QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableCostManager"),
            QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableApplicantName"), QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableErrandBeginDate"),
            QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableErrandEndDate"), QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableErrandCountry"),
            QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableErrandCounty"), QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableErrandCity"),
            QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchablePartyName"), QName.createQName(DocumentCommonModel.DOCCOM_URI, "searchableSubNodeProperties"),
            PrivilegeModel.Props.GROUP, PrivilegeModel.Props.USER, ContentModel.PROP_OWNER));

    private static final Set<QName> DOCCOM_PROPS_TO_RETAIN = new HashSet<QName>(
            Arrays.asList(DocumentCommonModel.Props.DOCUMENT_IS_IMPORTED, DocumentCommonModel.Props.UPDATE_METADATA_IN_FILES,
                    DocumentCommonModel.Props.FILE_NAMES, DocumentCommonModel.Props.FILE_CONTENTS, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE,
                    DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS,
                    DocumentCommonModel.Props.SEARCHABLE_FUND, DocumentCommonModel.Props.SEARCHABLE_FUNDS_CENTER, DocumentCommonModel.Props.SEARCHABLE_EA_COMMITMENT_ITEM));

    public static final Set<QName> ASPECTS_WITH_CHILD_ASSOCS = new HashSet<QName>(
            Arrays.asList(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V2, DocumentSpecificModel.Aspects.CONTRACT_MV_DETAILS,
                    DocumentSpecificModel.Aspects.TRAINING_APPLICATION, DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2,
                    DocumentSpecificModel.Aspects.ERRAND_ORDER_APPLICANT_ABROAD, DocumentSpecificModel.Aspects.ERRAND_ORDER_APPLICANT_ABROAD_V2,
                    QName.createQName(DOCSPEC_URI, "errandApplicationApplicantDomestic"), DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD,
                    DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2, DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_MV,
                    DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC, DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC_V2));

    private static final Set<String> SINGLE_VALUE_TO_MULTIVALUED = new HashSet<String>(
            Arrays.asList(DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName(), DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE.getLocalName(),
                    DocumentSpecificModel.Props.SUBSTITUTION_END_DATE.getLocalName()));

    private final Map<String, String> vacationTypeValues = new HashMap<String, String>();

    private static final Map<QName, QName> STATIC_TO_DYNAMIC_ASSOC_QNAMES = new HashMap<QName, QName>();
    private static final Map<QName, QName> STATIC_CHILD_TO_GRAND_CHILD = new HashMap<QName, QName>();
    /** Contains types that are not converted to type with same name */
    public static final Map<QName, String> STATIC_TO_DYNAMIC_DOC_TYPE = new HashMap<QName, String>();
    /** Contains properties that are not converted to dynamic namespace property with same name */
    public static final Map<QName, QName> STATIC_TO_DYNAMIC_PROP_QNAME = new HashMap<QName, QName>();

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private DocumentService documentService;
    private PermissionService permissionService;
    private DocumentDynamicService documentDynamicService;
    private DocumentConfigService documentConfigService;
    private WorkflowService workflowService;
    private TransactionService transactionService;

    private DocumentUpdater documentUpdater;
    private DocumentChangedTypePropertiesUpdater documentChangedTypePropertiesUpdater;
    private LogAndDeleteObjectsWithMissingType logAndDeleteObjectsWithMissingType;
    private RegistrationNumberReinventedUpdater registrationNumberReinventedUpdater;
    private ContractPartyAssocUpdater contractPartyAssocUpdater;
    private DocumentCompWorkflowSearchPropsUpdater documentCompWorkflowSearchPropsUpdater;
    private DocumentAccessRestrictionUpdater documentAccessRestrictionUpdater;
    private DocumentPartyPropsUpdater documentPartyPropsUpdater;
    private Workflow25To313DynamicDocTypeUpdater workflow25To313DynamicDocTypeUpdater;
    private TaskUpdater taskUpdater;
    private LogAndDeleteNotExistingWorkflowTasks logAndDeleteNotExistingWorkflowTasks;

    private Collection<QName> docSubTypes;
    private Map<String, DocumentTypeVersion> dynamicDocumentTypes;
    private final Map<String, Map<String, Pair<DynamicPropertyDefinition, Field>>> dynamicDocumentPropsAndFields = new HashMap<String, Map<String, Pair<DynamicPropertyDefinition, Field>>>();
    private final Map<String, Map<QName, Map<String, Pair<DynamicPropertyDefinition, Field>>>> dynamicDocumentNodesPropsAndFields = new HashMap<String, Map<QName, Map<String, Pair<DynamicPropertyDefinition, Field>>>>();
    private final Map<String, TreeNode<QName>> childAssocTypeQNames = new HashMap<String, TreeNode<QName>>();
    private final Map<Pair<String, QName>, TreeNode<QName>> childNodeQNameHierarchies = new HashMap<Pair<String, QName>, TreeNode<QName>>();
    private StoreRef activeStore;
    private Map<String, List<String>> orgStructNameToPath;
    private Collection<QName> contentModelProps;

    private final Set<NodeRef> nodesToDelete = new HashSet<NodeRef>();
    private final Set<NodeRef> nodesToRemoveAspects = new HashSet<NodeRef>();
    private File nodesToDeleteFile;
    private File nodesToRemoveAspectsFile;
    private boolean smitUpdater;

    static {
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE, DocumentChildModel.Assocs.CONTRACT_PARTY);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE, DocumentChildModel.Assocs.APPLICANT_DOMESTIC);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2, DocumentChildModel.Assocs.APPLICANT_DOMESTIC);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD, DocumentChildModel.Assocs.APPLICANT_ABROAD);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2, DocumentChildModel.Assocs.APPLICANT_ABROAD);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV, DocumentChildModel.Assocs.APPLICANT_ABROAD);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE, DocumentChildModel.Assocs.APPLICANT_TRAINING);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE_V2, DocumentChildModel.Assocs.APPLICANT_TRAINING);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE, DocumentChildModel.Assocs.ERRAND_ABROAD);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2, DocumentChildModel.Assocs.ERRAND_ABROAD);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE, DocumentChildModel.Assocs.ERRAND_ABROAD);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE, DocumentChildModel.Assocs.ERRAND_DOMESTIC);
        STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE_V2, DocumentChildModel.Assocs.ERRAND_DOMESTIC);

        STATIC_CHILD_TO_GRAND_CHILD.put(DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE, DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE);
        STATIC_CHILD_TO_GRAND_CHILD.put(DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE_V2, DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE_V2);
        STATIC_CHILD_TO_GRAND_CHILD.put(DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD, DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE);
        STATIC_CHILD_TO_GRAND_CHILD.put(DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2, DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2);
        STATIC_CHILD_TO_GRAND_CHILD.put(DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV, DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE);

        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.CONTRACT_SIM, "contract");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.CONTRACT_SMIT, "contract");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.PERSONELLE_ORDER_SIM, "personelleOrder");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.PERSONELLE_ORDER_SMIT, "personelleOrder");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV, "errandApplication");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.VACATION_ORDER, "vacationApplication");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.VACATION_ORDER_SMIT, "vacationApplication");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.MINISTERS_ORDER, "generalOrder");
        STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.REGULATION, "generalOrder");

        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("sendDesc"), DocumentCommonModel.Props.SEND_DESC_VALUE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.MANAGEMENTS_ORDER_DUE_DATE), DocumentSpecificModel.Props.DUE_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("managementsOrderComplienceDate"), DocumentSpecificModel.Props.COMPLIENCE_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("managementsOrderComplienceNotation"), DocumentSpecificModel.Props.COMPLIENCE_NOTATION);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.ERRAND_SUBSTITUTE_NAME), DocumentSpecificModel.Props.SUBSTITUTE_NAME);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("errandSubstituteBeginDate"), DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("errandSubstituteEndDate"), DocumentSpecificModel.Props.SUBSTITUTION_END_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("expensesFinancingSource"), DocumentSpecificModel.Props.FINANCING_SOURCE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("expectedExpenseTotalSum"), DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp("expenditureItem"), DocumentDynamicModel.Props.COST_CENTER);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE), DocumentSpecificModel.Props.DUE_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE_DESC), DocumentSpecificModel.Props.DUE_DATE_DESC);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE), DocumentSpecificModel.Props.DUE_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE_DESC), DocumentSpecificModel.Props.DUE_DATE_DESC);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_BEGIN_DATES), DocumentDynamicModel.Props.LEAVE_BEGIN_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_END_DATES), DocumentDynamicModel.Props.LEAVE_END_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATES),
                DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATES), DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATES), DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_END_DATES), DocumentSpecificModel.Props.LEAVE_NEW_END_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATES),
                DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHANGE_DAYS), DocumentDynamicModel.Props.LEAVE_CHANGED_DAYS);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATES), DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.COST_ELEMENT), DocumentDynamicModel.Props.COST_CENTER);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_NAME), DocumentSpecificModel.Props.PARTY_NAME);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_SIGNER), DocumentSpecificModel.Props.PARTY_SIGNER);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_EMAIL), DocumentSpecificModel.Props.PARTY_EMAIL);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON), DocumentSpecificModel.Props.PARTY_CONTACT_PERSON);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_NAME), DocumentSpecificModel.Props.PARTY_NAME);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_SIGNER), DocumentSpecificModel.Props.PARTY_SIGNER);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_EMAIL), DocumentSpecificModel.Props.PARTY_EMAIL);
        STATIC_TO_DYNAMIC_PROP_QNAME.put(createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON), DocumentSpecificModel.Props.PARTY_CONTACT_PERSON);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        if (smitUpdater) {
            STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV, DocumentChildModel.Assocs.APPLICANT_ERRAND);
            STATIC_TO_DYNAMIC_ASSOC_QNAMES.put(DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE, DocumentChildModel.Assocs.ERRAND);
            STATIC_TO_DYNAMIC_DOC_TYPE.put(DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV, "errandOrderAbroadSmit");
        }
    }

    private static QName createDocspecProp(String localName) {
        return QName.createQName(DocumentSpecificModel.DOCSPEC_URI, localName);
    }

    private static QName createDocspecProp(QName propName) {
        return createDocspecProp(propName.getLocalName());
    }

    public void addChildTypeField(String docTypeId, QName childTypeQName, Entry<String, Pair<DynamicPropertyDefinition, Field>> fieldEntry) {
        if (!dynamicDocumentNodesPropsAndFields.containsKey(docTypeId)) {
            dynamicDocumentNodesPropsAndFields.put(docTypeId, new HashMap<QName, Map<String, Pair<DynamicPropertyDefinition, Field>>>());
        }
        Map<QName, Map<String, Pair<DynamicPropertyDefinition, Field>>> qnameMap = dynamicDocumentNodesPropsAndFields.get(docTypeId);
        if (!qnameMap.containsKey(childTypeQName)) {
            qnameMap.put(childTypeQName, new HashMap<String, Pair<DynamicPropertyDefinition, Field>>());
        }
        qnameMap.get(childTypeQName).put(fieldEntry.getKey(), fieldEntry.getValue());
    }

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(dictionaryService.getTypes(DocumentSubtypeModel.MODEL_NAME));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected void executeAfterCommit(final File completedFile, final CsvWriterClosure closure) {
        super.executeAfterCommit(completedFile, closure);
        writeNodeRefsToFile(nodesToDeleteFile, nodesToDelete);
        writeNodeRefsToFile(nodesToRemoveAspectsFile, nodesToRemoveAspects);
    }

    public void writeNodeRefsToFile(File file, Set<NodeRef> nodeRefs) {
        try {
            // Mark nodes for deleting
            CsvWriter writer = new CsvWriter(new FileWriter(file, true), CSV_SEPARATOR);
            try {
                for (NodeRef nodeToDelete : nodeRefs) {
                    writer.writeRecord(new String[] { nodeToDelete.toString() });
                }
                nodeRefs.clear();
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing file '" + file + "': " + e.getMessage(), e);
        }
    }

    @Override
    protected void executeUpdater() throws Exception {
        initData();
        super.executeUpdater();
    }

    private void initData() {
        docSubTypes = dictionaryService.getTypes(DocumentSubtypeModel.MODEL_NAME);
        dynamicDocumentTypes = BeanHelper.getDocumentAdminService().getLatestDocTypeVersions();
        for (Map.Entry<String, DocumentTypeVersion> entry : dynamicDocumentTypes.entrySet()) {
            String docTypeId = entry.getKey();
            DocumentTypeVersion docVersion = entry.getValue();
            Map<String, Pair<DynamicPropertyDefinition, Field>> docPropertyDefinitions = documentConfigService.getPropertyDefinitions(new PropDefCacheKey(DocumentType.class,
                    docTypeId, docVersion.getVersionNr()));
            dynamicDocumentPropsAndFields.put(docTypeId, docPropertyDefinitions);
            for (Entry<String, Pair<DynamicPropertyDefinition, Field>> fieldEntry : docPropertyDefinitions.entrySet()) {
                QName[] childTypeHierarchy = fieldEntry.getValue().getFirst().getChildAssocTypeQNameHierarchy();
                if (childTypeHierarchy == null || childTypeHierarchy.length == 0) {
                    addChildTypeField(docTypeId, DocumentCommonModel.Types.DOCUMENT, fieldEntry);
                } else {
                    for (Map.Entry<QName, QName> childTypeQNames : STATIC_TO_DYNAMIC_ASSOC_QNAMES.entrySet()) {
                        QName staticChildQName = childTypeQNames.getKey();
                        QName dynamicChildQName = childTypeQNames.getValue();
                        if (childTypeHierarchy.length == 1 && dynamicChildQName.equals(childTypeHierarchy[0])) {
                            addChildTypeField(docTypeId, dynamicChildQName, fieldEntry);
                        } else if (childTypeHierarchy.length == 2) {
                            if (STATIC_CHILD_TO_GRAND_CHILD.containsKey(staticChildQName)) {
                                QName dynamicGrandChildQName = STATIC_TO_DYNAMIC_ASSOC_QNAMES.get(STATIC_CHILD_TO_GRAND_CHILD.get(staticChildQName));
                                if (dynamicChildQName.equals(childTypeHierarchy[0]) && dynamicGrandChildQName.equals(childTypeHierarchy[1])) {
                                    addChildTypeField(docTypeId, dynamicGrandChildQName, fieldEntry);
                                }
                            }
                        }
                    }
                }
            }
            childAssocTypeQNames.put(docTypeId, documentConfigService.getChildAssocTypeQNameTree(docVersion));
        }
        ASPECTS_TO_REMOVE.addAll(dictionaryService.getAspects(DocumentSubtypeModel.MODEL_NAME));
        ASPECTS_TO_REMOVE.addAll(dictionaryService.getAspects(DocumentSpecificModel.MODEL));
        ASPECTS_TO_REMOVE.removeAll(ASPECTS_WITH_CHILD_ASSOCS);
        activeStore = generalService.getStore();
        documentUpdater.setEnabled(false);
        documentChangedTypePropertiesUpdater.setEnabled(false);
        logAndDeleteObjectsWithMissingType.setEnabled(false);
        registrationNumberReinventedUpdater.setEnabled(false);
        documentPartyPropsUpdater.setEnabled(false);
        contractPartyAssocUpdater.setEnabled(false);
        documentCompWorkflowSearchPropsUpdater.setEnabled(false);
        documentAccessRestrictionUpdater.setEnabled(false);
        taskUpdater.setEnabled(false);
        logAndDeleteNotExistingWorkflowTasks.setEnabled(false);

        for (ClassificatorValue classificatorValue : BeanHelper.getClassificatorService().getAllClassificatorValues("leaveType")) {
            vacationTypeValues.put(classificatorValue.getValueData(), classificatorValue.getValueName());
        }

        if (!workflow25To313DynamicDocTypeUpdater.isOrganizationsSynchronized()) {
            workflow25To313DynamicDocTypeUpdater.synchronizeOrganizations();
        }

        orgStructNameToPath = new HashMap<String, List<String>>();
        fillOrgStructNameToPath(orgStructNameToPath, BeanHelper.getOrganizationStructureService());
        contentModelProps = dictionaryService.getProperties(QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "contentmodel"));
        nodesToDeleteFile = loadNodesToDeleteFile();
        nodesToRemoveAspectsFile = loadNodesToRemoveAspectsFile();
    }

    public static void fillOrgStructNameToPath(Map<String, List<String>> orgStructNameToPath, OrganizationStructureService organizationStructureService) {
        List<OrganizationStructure> orgStructures = organizationStructureService.getAllOrganizationStructures();
        if (orgStructures != null) {
            for (OrganizationStructure orgStructure : orgStructures) {
                orgStructNameToPath.put(orgStructure.getName(), orgStructure.getOrganizationPath());
            }
        }
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName staticDocType = nodeService.getType(nodeRef);
        if (!docSubTypes.contains(staticDocType)) {
            return new String[] { "skip_wrong_type", staticDocType.toPrefixString(namespaceService) };
        }
        WmNode document = new WmNode(nodeRef, staticDocType);
        if (documentService.isDraft(nodeRef)) {
            return logAndMarkForDelete(nodeRef, staticDocType, document, "mark_for_delete_draft");
        }
        String dynamicDocTypeId;
        if (STATIC_TO_DYNAMIC_DOC_TYPE.containsKey(staticDocType)) {
            dynamicDocTypeId = STATIC_TO_DYNAMIC_DOC_TYPE.get(staticDocType);
        } else {
            dynamicDocTypeId = staticDocType.getLocalName();
        }
        if (!dynamicDocumentTypes.containsKey(dynamicDocTypeId)) {
            return new String[] { "delete_missing_type", staticDocType.toPrefixString(namespaceService) };
        }

        nodeService.setType(nodeRef, DocumentCommonModel.Types.DOCUMENT);

        Set<QName> documentAspects = document.getAspects();
        Set<QName> originalDocumentAspects = new HashSet<QName>(documentAspects);
        Map<QName, Serializable> documentProperties = RepoUtil.toQNameProperties(document.getProperties());
        Map<QName, Serializable> newDocumentProperties = new HashMap<QName, Serializable>();

        DocumentTypeVersion documentTypeVersion = dynamicDocumentTypes.get(dynamicDocTypeId);
        setTypeProps(DocAdminUtil.getDocTypeIdAndVersionNr(documentTypeVersion), newDocumentProperties);

        List<String> unknownDocspecProperties = new ArrayList<String>();
        List<String> propConversionErrors = new ArrayList<String>();
        if (!dynamicDocumentNodesPropsAndFields.containsKey(dynamicDocTypeId)) {
            throw new RuntimeException("Doc type has no fields defined: " + dynamicDocTypeId);
        }
        Map<String, Pair<DynamicPropertyDefinition, Field>> nodeTypeFields = dynamicDocumentNodesPropsAndFields.get(dynamicDocTypeId).get(DocumentCommonModel.Types.DOCUMENT);
        boolean isVacationOrderV1 = originalDocumentAspects.contains(DocumentSpecificModel.Aspects.VACATION_ORDER_COMMON);
        convertVacationOrderV1ToV2(documentProperties, isVacationOrderV1);
        boolean whoseNameToComment = DocumentSubtypeModel.Types.VACATION_APPLICATION.equals(staticDocType) || DocumentSubtypeModel.Types.VACATION_ORDER.equals(staticDocType)
                || DocumentSubtypeModel.Types.VACATION_ORDER_SMIT.equals(staticDocType);
        addConvertedProperties(documentProperties, newDocumentProperties, unknownDocspecProperties, propConversionErrors, nodeTypeFields,
                originalDocumentAspects, whoseNameToComment);

        if (StringUtils.isBlank((String) newDocumentProperties.get(DocumentCommonModel.Props.STORAGE_TYPE))) {
            newDocumentProperties.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());
        }

        // DocumentAccessRestrictionUpdater functionality
        clearAccessRestrictionHiddenFields(newDocumentProperties);

        // Fix 2.5 branch bug that archived document's parent properties are not updated
        if (!activeStore.equals(document.getNodeRef().getStoreRef())) {
            Map<QName, NodeRef> documentParents = documentService.getDocumentParents(nodeRef);
            for (Map.Entry<QName, NodeRef> documentParent : documentParents.entrySet()) {
                if (CaseModel.Types.CASE.equals(documentParent.getKey())) {
                    newDocumentProperties.put(DocumentCommonModel.Props.CASE, documentParent.getValue());
                } else if (VolumeModel.Types.VOLUME.equals(documentParent.getKey())) {
                    newDocumentProperties.put(DocumentCommonModel.Props.VOLUME, documentParent.getValue());
                } else if (SeriesModel.Types.SERIES.equals(documentParent.getKey())) {
                    newDocumentProperties.put(DocumentCommonModel.Props.SERIES, documentParent.getValue());
                } else if (FunctionsModel.Types.FUNCTION.equals(documentParent.getKey())) {
                    newDocumentProperties.put(DocumentCommonModel.Props.FUNCTION, documentParent.getValue());
                }
            }
        }

        // DocumentUpdater functionality
        if (!document.hasAspect(DocumentUpdater.EMAIL_DATE_TIME)) {
            // documentUpdater collects document reg numbers that are later used in DocumentRegNumbersUpdater, that functionality must remain
            documentUpdater.addParentRegNumber(nodeRef, (String) newDocumentProperties.get(DocumentCommonModel.Props.REG_NUMBER));
            newDocumentProperties.put(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, workflowService.hasAllFinishedCompoundWorkflows(nodeRef));
            documentUpdater.updateMetadataInFiles(newDocumentProperties, newDocumentProperties);
            if (documentUpdater.getFileEncodingUpdater().getDocumentsToUpdate().contains(nodeRef)) {
                documentUpdater.updateFileContentsProp(nodeRef, newDocumentProperties);
            }
        }

        // DocumentCompWorkflowSearchPropsUpdater functionality
        newDocumentProperties.put(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, workflowService.hasStartedCompoundWorkflows(nodeRef));

        // RegistrationNumberReinvented functionality
        String regNr = (String) newDocumentProperties.get(DocumentCommonModel.Props.REG_NUMBER);
        if (StringUtils.isNotBlank(regNr)) {
            RegNrHolder holder = new RegNrHolder(regNr);
            if (StringUtils.isNotBlank(holder.getShortRegNrWithoutIndividualizingNr())) {
                newDocumentProperties.put(DocumentCommonModel.Props.SHORT_REG_NUMBER, holder.getShortRegNrWithoutIndividualizingNr());
            }
            if (holder.getIndividualizingNr() != null && holder.getIndividualizingNr().intValue() >= 1) {
                newDocumentProperties.put(DocumentCommonModel.Props.INDIVIDUAL_NUMBER, holder.getIndividualizingNr().intValue() + "");
            }
        }

        // no default values are set, this should be correct
        TreeNode<QName> childAssocTypeQNameTree = childAssocTypeQNames.get(dynamicDocTypeId);
        List<TreeNode<QName>> childAssocTypeQNameChildren = childAssocTypeQNameTree.getChildren();
        List<Pair<QName, WmNode>> requiredChildren = documentDynamicService.createChildNodesHierarchy(document, childAssocTypeQNameChildren, null);

        List<Pair<Pair<NodeRef, QName>, Node>> updatableChildNodes = new ArrayList<Pair<Pair<NodeRef, QName>, Node>>();
        boolean isErrandV1 = originalDocumentAspects.contains(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)
                || originalDocumentAspects.contains(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_MV)
                || originalDocumentAspects.contains(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC);
        Map<QName, Serializable> errandPropsToAdd = getErrandV1ChildProps(documentProperties, isErrandV1);
        if (originalDocumentAspects.contains(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1)
                || (DocumentSubtypeModel.Types.CONTRACT_SMIT.equals(staticDocType) && documentProperties.containsKey(DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON))) {
            // contract v1 doesn't have convertable children, but we add contract parties from document data
            Pair<Map<QName, Serializable>, Map<QName, Serializable>> secondAndThirdPartyProps = convertContractV1ToV2(documentProperties);
            QName dynamicAssocQName = DocumentChildModel.Assocs.CONTRACT_PARTY;
            Iterator<Node> existingDynamicChildNodes = getUnmodifiableChildList(document, dynamicAssocQName).iterator();
            QName[] hierarchy = new QName[] { dynamicAssocQName };
            Map<String, Pair<DynamicPropertyDefinition, Field>> childNodeTypeFields = dynamicDocumentNodesPropsAndFields.get(dynamicDocTypeId).get(dynamicAssocQName);
            Map<QName, Serializable> existingChildProps = secondAndThirdPartyProps.getFirst();
            Node dynamicChildNode = getOrCreateDynamicChildNode(document, dynamicDocTypeId, childAssocTypeQNameTree, dynamicAssocQName, existingDynamicChildNodes, hierarchy);
            fillDynamicChildNodeProps(originalDocumentAspects, unknownDocspecProperties, propConversionErrors, childNodeTypeFields, updatableChildNodes, existingChildProps,
                    dynamicChildNode, nodeRef, dynamicAssocQName);
            existingChildProps = secondAndThirdPartyProps.getSecond();
            dynamicChildNode = getOrCreateDynamicChildNode(document, dynamicDocTypeId, childAssocTypeQNameTree, dynamicAssocQName, existingDynamicChildNodes, hierarchy);
            fillDynamicChildNodeProps(originalDocumentAspects, unknownDocspecProperties, propConversionErrors, childNodeTypeFields, updatableChildNodes, existingChildProps,
                    dynamicChildNode, nodeRef, dynamicAssocQName);
        }
        List<NodeRef> deletedChildRefs = new ArrayList<NodeRef>();
        for (QName staticChildTypeQName : CONVERTABLE_CHILD_QNAMES) {
            List<ChildAssociationRef> childrenToConvert = null;
            childrenToConvert = nodeService.getChildAssocs(nodeRef, Collections.singleton(staticChildTypeQName));
            if (childrenToConvert.size() == 0) {
                continue;
            }
            QName dynamicAssocQName = STATIC_TO_DYNAMIC_ASSOC_QNAMES.get(staticChildTypeQName);
            TreeNode<QName> childAssocHierarchyNode = getRootOfType(childAssocTypeQNameChildren, dynamicAssocQName);
            if (childAssocHierarchyNode == null) {
                throw new RuntimeException("Cannot convert type " + dynamicDocTypeId + ": no child nodes of type " + dynamicAssocQName
                        + " are allowed on dynamic doc type " + dynamicDocTypeId + " (needed to convert static child nodes of type " + staticChildTypeQName
                        + "). Document nodeRef=" + nodeRef);
            }
            Iterator<Node> existingDynamicChildNodes = getUnmodifiableChildList(document, dynamicAssocQName).iterator();
            QName[] hierarchy = new QName[] { dynamicAssocQName };
            Map<String, Pair<DynamicPropertyDefinition, Field>> childNodeTypeFields = dynamicDocumentNodesPropsAndFields.get(dynamicDocTypeId).get(dynamicAssocQName);
            for (ChildAssociationRef childAssoc : childrenToConvert) {
                NodeRef childRef = childAssoc.getChildRef();
                Map<QName, Serializable> existingChildProps = RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(childRef), dictionaryService);
                Node dynamicChildNode = getOrCreateDynamicChildNode(document, dynamicDocTypeId, childAssocTypeQNameTree, dynamicAssocQName, existingDynamicChildNodes,
                        hierarchy);
                fillDynamicChildNodeProps(originalDocumentAspects, unknownDocspecProperties, propConversionErrors, childNodeTypeFields, updatableChildNodes,
                        existingChildProps,
                        dynamicChildNode, nodeRef, dynamicAssocQName);
                QName staticGrandChildQName = STATIC_CHILD_TO_GRAND_CHILD.get(staticChildTypeQName);
                if (staticGrandChildQName != null) {
                    List<ChildAssociationRef> grandChildrenToConvert = nodeService.getChildAssocs(childRef, Collections.singleton(staticGrandChildQName));
                    QName dynamicGrandChildAssocQName = STATIC_TO_DYNAMIC_ASSOC_QNAMES.get(staticGrandChildQName);
                    if (getRootOfType(childAssocHierarchyNode.getChildren(), dynamicGrandChildAssocQName) == null) {
                        throw new RuntimeException("Cannot convert type " + dynamicDocTypeId + ": no grandchild nodes of type " + dynamicGrandChildAssocQName
                                + " are allowed on dynamic type (needed to convert static child nodes of type " + staticGrandChildQName + "). Document nodeRef=" + nodeRef);
                    }
                    Iterator<Node> existingDynamicGrandChildNodes = getUnmodifiableChildList(dynamicChildNode, dynamicGrandChildAssocQName).iterator();
                    QName[] grandChildHierarchy = new QName[] { dynamicAssocQName, dynamicGrandChildAssocQName };
                    Map<String, Pair<DynamicPropertyDefinition, Field>> grandChildNodeTypeFields = dynamicDocumentNodesPropsAndFields.get(dynamicDocTypeId).get(
                            dynamicGrandChildAssocQName);
                    for (ChildAssociationRef grandChildAssoc : grandChildrenToConvert) {
                        Map<QName, Serializable> existingGrandChildProps = RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(grandChildAssoc.getChildRef()),
                                dictionaryService);
                        if (isErrandV1
                                && (DocumentChildModel.Assocs.ERRAND_ABROAD.equals(dynamicGrandChildAssocQName)
                                        || DocumentChildModel.Assocs.ERRAND_DOMESTIC.equals(dynamicGrandChildAssocQName) || DocumentChildModel.Assocs.ERRAND
                                        .equals(dynamicGrandChildAssocQName))) {
                            existingGrandChildProps.putAll(errandPropsToAdd);
                        }
                        Node dynamicGrandChildNode = getOrCreateDynamicChildNode(dynamicChildNode, dynamicDocTypeId, childAssocTypeQNameTree, dynamicGrandChildAssocQName,
                                existingDynamicGrandChildNodes, grandChildHierarchy);
                        fillDynamicChildNodeProps(originalDocumentAspects, unknownDocspecProperties, propConversionErrors, grandChildNodeTypeFields, updatableChildNodes,
                                existingGrandChildProps, dynamicGrandChildNode, dynamicChildNode.getNodeRef(), dynamicGrandChildAssocQName);
                    }
                }
                deletedChildRefs.add(childRef);
                logAndMarkForDelete(childRef, staticDocType, document, "mark_for_delete_child_node");
            }
        }

        documentDynamicService.updateSearchableChildNodeProps(document, null, childAssocTypeQNameChildren, dynamicDocumentPropsAndFields.get(dynamicDocTypeId));

        // move notEditable and invoiceXml aspects and properties to doccom namespace
        if (documentAspects.contains(NOT_EDITABLE)) {
            documentAspects.add(DocumentCommonModel.Aspects.NOT_EDITABLE);
            newDocumentProperties.put(DocumentCommonModel.Props.NOT_EDITABLE, documentProperties.get(NOT_EDITABLE));
        }
        if (documentAspects.contains(INVOICE_XML)) {
            documentAspects.add(DocumentCommonModel.Aspects.INVOICE_XML);
            newDocumentProperties.put(DocumentCommonModel.Props.INVOICE_XML, documentProperties.get(INVOICE_XML));
        }
        documentAspects.removeAll(ASPECTS_TO_REMOVE);
        if (CollectionUtils.containsAny(documentAspects, ASPECTS_WITH_CHILD_ASSOCS)) {
            nodesToRemoveAspects.add(nodeRef);
        }
        documentAspects.add(DocumentAdminModel.Aspects.OBJECT);

        // handle aspects after child nodes are retrieved from repo, because removing aspects may
        // also remove child nodes defined in these aspects
        for (QName originalAspect : originalDocumentAspects) {
            if (!documentAspects.contains(originalAspect)) {
                nodeService.removeAspect(nodeRef, originalAspect);
            }
        }
        for (QName newAspect : documentAspects) {
            if (!originalDocumentAspects.contains(newAspect)) {
                nodeService.addAspect(nodeRef, newAspect, null);
            }
        }

        // overwrite all existing properties with new ones
        nodeService.setProperties(nodeRef, newDocumentProperties);
        Map<NodeRef, NodeRef> unsavedToSavedNodeRef = new HashMap<NodeRef, NodeRef>();
        for (Pair<Pair<NodeRef, QName>, Node> parentAndNode : updatableChildNodes) {
            Node node = parentAndNode.getSecond();
            DynamicTypeUtil.setTypePropsStringMap(DocAdminUtil.getDocTypeIdAndVersionNr(documentTypeVersion), node.getProperties());
            if (RepoUtil.isSaved(node)) {
                nodeService.setProperties(node.getNodeRef(), RepoUtil.toQNameProperties(node.getProperties()));
            } else {
                QName assocQName = parentAndNode.getFirst().getSecond();
                NodeRef unsavedNodeRef = node.getNodeRef();
                NodeRef parentNodeRef = parentAndNode.getFirst().getFirst();
                if (RepoUtil.isUnsaved(parentNodeRef)) {
                    parentNodeRef = unsavedToSavedNodeRef.get(parentNodeRef);
                }
                NodeRef savedNodeRef = nodeService.createNode(parentNodeRef, assocQName, assocQName, assocQName,
                        RepoUtil.toQNameProperties(node.getProperties())).getChildRef();
                unsavedToSavedNodeRef.put(unsavedNodeRef, savedNodeRef);
            }
        }
        checkAndSaveRequiredChildren(document, requiredChildren, unsavedToSavedNodeRef, deletedChildRefs);
        permissionService.setInheritParentPermissions(nodeRef, true);
        return new String[] { "updated_to_dynamic", staticDocType.toPrefixString(namespaceService), TextUtil.joinNonBlankStringsWithComma(unknownDocspecProperties),
                TextUtil.joinNonBlankStringsWithComma(propConversionErrors) };
    }

    // If some required children have not been filled with original document's data and thus are not saved,
    // save empty nodes as these nodes are required by model.
    // Use deletedNodeRefs to avoid nodeService.exists queries for each processed node.
    private void checkAndSaveRequiredChildren(Node document, List<Pair<QName, WmNode>> requiredChildren, Map<NodeRef, NodeRef> unsavedToSavedNodeRef, List<NodeRef> deletedNodeRefs) {
        if (deletedNodeRefs.contains(document.getNodeRef())) {
            return;
        }
        QName nodeType = document.getType();
        if (!docSubTypes.contains(nodeType) && !STATIC_TO_DYNAMIC_ASSOC_QNAMES.containsValue(nodeType)) {
            return;
        }
        for (Map.Entry<String, List<Node>> entry : document.getAllChildAssociationsByAssocType().entrySet()) {
            for (Node childNode : entry.getValue()) {
                for (Pair<QName, WmNode> requiredChild : requiredChildren) {
                    WmNode requiredChildNode = requiredChild.getSecond();
                    NodeRef requiredChildNodeRef = requiredChildNode.getNodeRef();
                    if (RepoUtil.isUnsaved(requiredChildNode) && requiredChildNodeRef.equals(childNode.getNodeRef()) && !unsavedToSavedNodeRef.containsKey(requiredChildNodeRef)) {
                        QName assocQName = requiredChild.getFirst();
                        NodeRef parentNodeRef = document.getNodeRef();
                        if (RepoUtil.isUnsaved(parentNodeRef)) {
                            parentNodeRef = unsavedToSavedNodeRef.get(parentNodeRef);
                        }
                        NodeRef savedNodeRef = nodeService.createNode(parentNodeRef, assocQName, assocQName, assocQName,
                                RepoUtil.toQNameProperties(requiredChildNode.getProperties())).getChildRef();
                        unsavedToSavedNodeRef.put(requiredChildNodeRef, savedNodeRef);
                        if (childNode instanceof WmNode) {
                            ((WmNode) childNode).updateNodeRef(savedNodeRef);
                        }
                    }
                }
                checkAndSaveRequiredChildren(childNode, requiredChildren, unsavedToSavedNodeRef, deletedNodeRefs);
            }
        }

    }

    private void clearAccessRestrictionHiddenFields(Map<QName, Serializable> newDocumentProperties) {
        String accessRestriction = (String) newDocumentProperties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (AccessRestriction.OPEN.equals(accessRestriction) || AccessRestriction.INTERNAL.equals(accessRestriction)) {
            newDocumentProperties.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, null);
            newDocumentProperties.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, null);
            newDocumentProperties.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, null);
            newDocumentProperties.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, null);
        }
    }

    private List<Node> getUnmodifiableChildList(Node dynamicChildNode, QName dynamicAssocQName) {
        List<Node> allChildAssociations = dynamicChildNode.getAllChildAssociations(dynamicAssocQName);
        if (allChildAssociations == null) {
            allChildAssociations = new ArrayList<Node>();
        }
        return Collections.unmodifiableList(new ArrayList<Node>(allChildAssociations));
    }

    private Map<QName, Serializable> getErrandV1ChildProps(Map<QName, Serializable> documentProperties, boolean isErrandV1) {
        Map<QName, Serializable> errandPropsToAdd = new HashMap<QName, Serializable>();
        if (isErrandV1) {
            addPropIfExists(documentProperties, errandPropsToAdd, "eventName");
            addPropIfExists(documentProperties, errandPropsToAdd, "travelPurpose");
            addPropIfExists(documentProperties, errandPropsToAdd, "eventOrganizer");
            addPropIfExists(documentProperties, errandPropsToAdd, "reportDueDate");
            addPropIfExists(documentProperties, errandPropsToAdd, DocumentSpecificModel.Props.EVENT_BEGIN_DATE.getLocalName());
            addPropIfExists(documentProperties, errandPropsToAdd, DocumentSpecificModel.Props.EVENT_END_DATE.getLocalName());
        }
        return errandPropsToAdd;
    }

    private Pair<Map<QName, Serializable>, Map<QName, Serializable>> convertContractV1ToV2(Map<QName, Serializable> documentProperties) {
        Map<QName, Serializable> secondPartyProps = new HashMap<QName, Serializable>();
        addPropToMap(documentProperties, secondPartyProps, createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON));
        addPropToMap(documentProperties, secondPartyProps, createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_EMAIL));
        addPropToMap(documentProperties, secondPartyProps, createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_NAME));
        addPropToMap(documentProperties, secondPartyProps, createDocspecProp(DocumentSpecificModel.Props.SECOND_PARTY_SIGNER));

        Map<QName, Serializable> thirdPartyProps = new HashMap<QName, Serializable>();
        addPropToMap(documentProperties, thirdPartyProps, createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON));
        addPropToMap(documentProperties, thirdPartyProps, createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_EMAIL));
        addPropToMap(documentProperties, thirdPartyProps, createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_NAME));
        addPropToMap(documentProperties, thirdPartyProps, createDocspecProp(DocumentSpecificModel.Props.THIRD_PARTY_SIGNER));

        return Pair.newInstance(secondPartyProps, thirdPartyProps);
    }

    private void convertVacationOrderV1ToV2(Map<QName, Serializable> documentProperties, boolean isVacationOrderV1) {
        List<String> leaveTypes = new ArrayList<String>();
        List<Date> leaveBeginDates = new ArrayList<Date>();
        List<Date> leaveEndDates = new ArrayList<Date>();
        List<Integer> leaveDays = new ArrayList<Integer>();
        if (isVacationOrderV1) {
            // vacation order v1 -> v2 conversion
            addVacation(documentProperties, leaveTypes, leaveBeginDates, leaveEndDates, leaveDays, createDocspecProp(DocumentSpecificModel.Props.LEAVE_ANNUAL),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_ANNUAL_BEGIN_DATE), createDocspecProp(DocumentSpecificModel.Props.LEAVE_ANNUAL_END_DATE),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_ANNUAL_DAYS));
            addVacation(documentProperties, leaveTypes, leaveBeginDates, leaveEndDates, leaveDays, createDocspecProp(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_BEGIN_DATE), createDocspecProp(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_END_DATE),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_WITHOUT_PAY_DAYS));
            addVacation(documentProperties, leaveTypes, leaveBeginDates, leaveEndDates, leaveDays, createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHILD),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHILD_BEGIN_DATE), createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHILD_END_DATE),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHILD_DAYS));
            addVacation(documentProperties, leaveTypes, leaveBeginDates, leaveEndDates, leaveDays, createDocspecProp(DocumentSpecificModel.Props.LEAVE_STUDY),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_STUDY_BEGIN_DATE), createDocspecProp(DocumentSpecificModel.Props.LEAVE_STUDY_END_DATE),
                    createDocspecProp(DocumentSpecificModel.Props.LEAVE_STUDY_DAYS));
            documentProperties.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_TYPE), (Serializable) leaveTypes);
            documentProperties.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_BEGIN_DATES), (Serializable) leaveBeginDates);
            documentProperties.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_END_DATES), (Serializable) leaveEndDates);
            documentProperties.put(createDocspecProp(DocumentSpecificModel.Props.LEAVE_DAYS), (Serializable) leaveDays);

            if (Boolean.TRUE.equals(documentProperties.get(createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL)))) {
                convertPropsToList(documentProperties, createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_DAYS));
                removeAllFromMap(documentProperties, createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL));
            } else {
                removeAllFromMap(documentProperties, createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_BEGIN_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_CANCEL_DAYS));
            }
            if (Boolean.TRUE.equals(documentProperties.get(createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHANGE)))) {
                convertPropsToList(documentProperties, createDocspecProp(DocumentSpecificModel.Props.LEAVE_INITIAL_BEGIN_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE), createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_END_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_DAYS));
                removeAllFromMap(documentProperties, createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHANGE));
            } else {
                removeAllFromMap(documentProperties, createDocspecProp(DocumentSpecificModel.Props.LEAVE_CHANGE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_INITIAL_END_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_BEGIN_DATE), createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_END_DATE),
                        createDocspecProp(DocumentSpecificModel.Props.LEAVE_NEW_DAYS));
            }
        }
    }

    private void removeAllFromMap(Map<QName, Serializable> documentProperties, QName... propertyQNames) {
        for (QName propertyQName : propertyQNames) {
            documentProperties.remove(propertyQName);
        }
    }

    private TreeNode<QName> getRootOfType(List<TreeNode<QName>> childAssocTypeQNameChildren, QName nodeType) {
        if (childAssocTypeQNameChildren == null || nodeType == null) {
            return null;
        }
        for (TreeNode<QName> treeNode : childAssocTypeQNameChildren) {
            if (treeNode != null && nodeType.equals(treeNode.getData())) {
                return treeNode;
            }
        }
        return null;
    }

    private void fillDynamicChildNodeProps(Set<QName> originalDocumentAspects, List<String> unknownDocspecProperties, List<String> propConversionErrors,
            Map<String, Pair<DynamicPropertyDefinition, Field>> nodeTypeFields, List<Pair<Pair<NodeRef, QName>, Node>> updatableChildNodes,
            Map<QName, Serializable> existingChildProps, Node dynamicChildNode, NodeRef parentRef, QName assocQName) {
        Map<QName, Serializable> newChildProps = RepoUtil.toQNameProperties(dynamicChildNode.getProperties(), true);
        addConvertedProperties(existingChildProps, newChildProps, unknownDocspecProperties, propConversionErrors,
                nodeTypeFields, originalDocumentAspects, false);
        dynamicChildNode.getProperties().putAll(RepoUtil.toStringProperties(newChildProps));
        updatableChildNodes.add(new Pair<Pair<NodeRef, QName>, Node>(new Pair<NodeRef, QName>(parentRef, assocQName), dynamicChildNode));
    }

    private void addPropToMap(Map<QName, Serializable> documentProperties, Map<QName, Serializable> secondPartyProps, QName docspecProp) {
        secondPartyProps.put(docspecProp, documentProperties.get(docspecProp));
    }

    private void convertPropsToList(Map<QName, Serializable> documentProperties, QName... properties) {
        for (QName propName : properties) {
            Serializable propValue = documentProperties.get(propName);
            if (!(propValue instanceof List)) {
                List list = new ArrayList();
                list.add(propValue);
                documentProperties.put(propName, (Serializable) list);
            }
        }
    }

    private void addVacation(Map<QName, Serializable> documentProperties, List<String> vacationTypes, List<Date> vacationBeginDates, List<Date> vacationEndDates,
            List<Integer> vacationDays, QName typeProp, QName beginDateProp, QName endDateProp, QName daysProp) {
        typeProp = createDocspecProp(typeProp);
        beginDateProp = createDocspecProp(beginDateProp);
        endDateProp = createDocspecProp(endDateProp);
        daysProp = createDocspecProp(daysProp);
        if (Boolean.TRUE.equals(documentProperties.get(typeProp))) {
            vacationTypes.add(getVacationTypeDescription(typeProp.getLocalName()));
            vacationBeginDates.add((Date) documentProperties.get(beginDateProp));
            vacationEndDates.add((Date) documentProperties.get(endDateProp));
            vacationDays.add((Integer) documentProperties.get(daysProp));
        }
        documentProperties.remove(typeProp);
        documentProperties.remove(daysProp);
        documentProperties.remove(beginDateProp);
        documentProperties.remove(endDateProp);
    }

    private String getVacationTypeDescription(String vacationType) {
        if (StringUtils.isNotBlank(vacationType) && vacationTypeValues.containsKey(vacationType)) {
            vacationType = vacationTypeValues.get(vacationType);
        }
        return vacationType;
    }

    private void addPropIfExists(Map<QName, Serializable> documentProperties, Map<QName, Serializable> errandPropsToAdd, String propName) {
        QName propName1 = createDocspecProp(propName);
        if (documentProperties.containsKey(propName1)) {
            addPropToMap(documentProperties, errandPropsToAdd, propName1);
        }
    }

    private void addConvertedProperties(Map<QName, Serializable> originalProperties, Map<QName, Serializable> newProperties,
            List<String> unknownDocspecProperties, List<String> propConversionErrors,
            Map<String, Pair<DynamicPropertyDefinition, Field>> nodeTypeFields, Set<QName> staticDocumentAspects, boolean whoseNameToComment) {
        Pair<Serializable, Serializable> dailyAllowance = null;
        Pair<Serializable, Serializable> expenses = null;
        boolean isTrainingApplicationV1 = staticDocumentAspects.contains(DocumentSpecificModel.Aspects.TRAINING_APPLICATION);
        boolean isErrandDomesticApplicationV1 = staticDocumentAspects.contains(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC);
        boolean isErrandAbroadApplicationV1 = staticDocumentAspects.contains(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)
                || (staticDocumentAspects.contains(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_MV) && !smitUpdater);
        boolean isContractSim = staticDocumentAspects.contains(DocumentSpecificModel.Aspects.CONTRACT_SIM_DETAILS);
        // properties that need additional processing
        Map<QName, String> collectedProps = new HashMap<QName, String>();
        for (Map.Entry<QName, Serializable> entry : originalProperties.entrySet()) {
            QName propName = entry.getKey();
            Serializable propValue = entry.getValue();
            String namespaceURI = propName.getNamespaceURI();
            boolean isTransientProp = RepoUtil.TRANSIENT_PROPS_NAMESPACE.equals(namespaceURI);
            if (PROPS_TO_OMIT.contains(propName) || isTransientProp || isUndefinedContentModelProp(propName)) {
                if (isTransientProp) {
                    unknownDocspecProperties.add(propToStr(propName));
                }
                continue;
            } else if (DOCCOM_PROPS_TO_RETAIN.contains(propName)) {
                newProperties.put(propName, propValue);
            } else if (DocumentSpecificModel.DOCSPEC_URI.equals(namespaceURI) || DocumentCommonModel.DOCCOM_URI.equals(namespaceURI)) {
                if (isTrainingApplicationV1 || isErrandDomesticApplicationV1 || isErrandAbroadApplicationV1) {
                    if ((isTrainingApplicationV1 || isErrandAbroadApplicationV1) && createDocspecProp(DocumentSpecificModel.Props.DAILY_ALLOWANCE_CATERING_COUNT).equals(propName)) {
                        dailyAllowance = getSerializablePair(dailyAllowance);
                        dailyAllowance.setFirst(propValue);
                        continue;
                    } else if ((isTrainingApplicationV1 || isErrandAbroadApplicationV1) && DAILY_ALLOWANCE_FINANCING_SOURCE.equals(propName)) {
                        dailyAllowance = getSerializablePair(dailyAllowance);
                        dailyAllowance.setSecond(propValue);
                        continue;
                    } else if (EXPENSE_DESC.equals(propName)) {
                        expenses = getSerializablePair(expenses);
                        expenses.setFirst(propValue);
                        continue;
                    } else if (EXPENSE_FINANCING_SOURCE.equals(propName)) {
                        expenses = getSerializablePair(expenses);
                        expenses.setSecond(propValue);
                        continue;
                    } else if (isErrandDomesticApplicationV1 && createDocspecProp(DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName()).equals(propName)
                            && StringUtils.isNotBlank((String) propValue)) {
                        collectedProps.put(DocumentSpecificModel.Props.ERRAND_SUBSTITUTE_NAME, (String) propValue);
                        continue;
                    }
                } else if (isContractSim && DocumentSpecificModel.Props.PAYMENT_ANNOTATION.equals(propName) && StringUtils.isNotBlank((String) propValue)) {
                    collectedProps.put(DocumentCommonModel.Props.COMMENT, (String) propValue);
                    continue;
                } else if (whoseNameToComment && createDocspecProp("whoseName").equals(propName) && (propValue == null || (propValue instanceof String))) {
                    collectedProps.put(DocumentCommonModel.Props.COMMENT, MessageUtil.getMessage("document_convert_whose_name", (String) propValue));
                    continue;
                }
                String dynamicPropLocalName;
                if (STATIC_TO_DYNAMIC_PROP_QNAME.containsKey(propName)) {
                    dynamicPropLocalName = STATIC_TO_DYNAMIC_PROP_QNAME.get(propName).getLocalName();
                } else {
                    dynamicPropLocalName = propName.getLocalName();
                }
                if (!nodeTypeFields.containsKey(dynamicPropLocalName)) {
                    unknownDocspecProperties.add(propToStr(propName));
                    continue;
                }
                if (createDocspecProp(DocumentSpecificModel.Props.LEAVE_TYPE).equals(createDocspecProp(propName))) {
                    if (propValue instanceof List && !((List) propValue).isEmpty()) {
                        List<String> typeDescriptions = new ArrayList<String>();
                        for (String typeCode : (List<String>) propValue) {
                            typeDescriptions.add(getVacationTypeDescription(typeCode));
                        }
                        propValue = (Serializable) typeDescriptions;
                    }
                }
                String propConversionError = addConvertedProp(nodeTypeFields, newProperties, dynamicPropLocalName, propValue);
                if (propConversionError != null) {
                    propConversionErrors.add(propConversionError);
                }
            } else {
                // properties from namespaces other than doccom, docspec, temp are preserved
                newProperties.put(propName, propValue);
            }
        }
        if (dailyAllowance != null) {
            collectedProps.put(DocumentSpecificModel.Props.FINANCING_SOURCE, pairOfListsToString(dailyAllowance, "document_25_to_313_daily_catering"));
        }
        if (expenses != null) {
            String financingSource = collectedProps.get(DocumentSpecificModel.Props.FINANCING_SOURCE);
            String expensesStr = pairOfListsToString(expenses, "document_25_to_313_expenses");
            collectedProps.put(DocumentSpecificModel.Props.FINANCING_SOURCE, TextUtil.joinNonBlankStrings(Arrays.asList(financingSource, expensesStr), "\n"));
        }
        for (Map.Entry<QName, String> entry : collectedProps.entrySet()) {
            QName propName = entry.getKey();
            if (nodeTypeFields.containsKey(propName.getLocalName())) {
                String existingValue = newProperties.get(propName) != null ? newProperties.get(propName).toString() : null;
                newProperties.put(propName, TextUtil.joinNonBlankStrings(Arrays.asList(existingValue, entry.getValue()), "\n"));
            } else {
                unknownDocspecProperties.add(propToStr(propName));
            }
        }
    }

    private boolean isUndefinedContentModelProp(QName propName) {
        if (!NamespaceService.CONTENT_MODEL_1_0_URI.equals(propName.getNamespaceURI()) || contentModelProps.contains(propName)) {
            return false;
        }
        return true;
    }

    private String propToStr(QName propName) {
        return propName.toPrefixString(namespaceService);
    }

    private String pairOfListsToString(Pair<Serializable, Serializable> dailyAllowance, String messageKey) {
        List cateringCoundList = getList(dailyAllowance.getFirst());
        List financeSourceList = getList(dailyAllowance.getSecond());
        int maxListSize = Math.max(cateringCoundList.size(), financeSourceList.size());
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < maxListSize; i++) {
            sb.append(MessageUtil.getMessage(messageKey, i < cateringCoundList.size() ? cateringCoundList.get(i) : null,
                    i < financeSourceList.size() ? financeSourceList.get(i) : null));
            if (i < maxListSize - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private List getList(Serializable cateringCount) {
        if (cateringCount == null || !(cateringCount instanceof List)) {
            // shouldn't happen; just in case
            List cateringCoundList = new ArrayList<String>();
            if (cateringCount != null) {
                cateringCoundList.add(cateringCount.toString());
            }
            return cateringCoundList;
        }
        return (List) cateringCount;
    }

    private Pair<Serializable, Serializable> getSerializablePair(Pair<Serializable, Serializable> dailyAllowanceCateringCountValues) {
        if (dailyAllowanceCateringCountValues == null) {
            dailyAllowanceCateringCountValues = new Pair<Serializable, Serializable>(null, null);
        }
        return dailyAllowanceCateringCountValues;
    }

    public List<NodeRef> getConvertableChildRefs(NodeRef nodeRef) {
        List<NodeRef> allChangeableChildRefs = new ArrayList<NodeRef>();
        for (QName staticChildTypeQName : CONVERTABLE_CHILD_QNAMES) {
            List<ChildAssociationRef> childrenToConvert = nodeService.getChildAssocs(nodeRef, Collections.singleton(staticChildTypeQName));
            for (ChildAssociationRef childAssoc : childrenToConvert) {
                NodeRef childRef = childAssoc.getChildRef();
                QName staticGrandChildQName = STATIC_CHILD_TO_GRAND_CHILD.get(staticChildTypeQName);
                if (staticGrandChildQName != null) {
                    List<ChildAssociationRef> grandChildrenToConvert = nodeService.getChildAssocs(childRef, Collections.singleton(staticGrandChildQName));
                    for (ChildAssociationRef grandChildAssoc : grandChildrenToConvert) {
                        allChangeableChildRefs.add(grandChildAssoc.getChildRef());
                    }
                }
                allChangeableChildRefs.add(childRef);
            }
        }
        return allChangeableChildRefs;
    }

    public String[] logAndMarkForDelete(final NodeRef nodeRef, QName docType, WmNode document, String action) {
        nodeService.addAspect(nodeRef, DocumentCommonModel.Aspects.DELETE_PERMANENT, null);
        nodesToDelete.add(nodeRef);
        return new String[] { action, docType.toPrefixString(namespaceService) };
    }

    public File loadNodesToDeleteFile() {
        return new File(inputFolder, getBaseFileName() + "MarkedForDelete.csv");
    }

    public File loadNodesToRemoveAspectsFile() {
        return new File(inputFolder, getBaseFileName() + "MarkedForRemoveAspects.csv");
    }

    public Node getOrCreateDynamicChildNode(Node document, String docTypeId, TreeNode<QName> childAssocTypeQNameTree, QName dynamicAssocQName,
            Iterator<Node> existingDynamicChildNodes, QName[] hierarchy) {
        Node dynamicChildNode;
        if (existingDynamicChildNodes.hasNext()) {
            dynamicChildNode = existingDynamicChildNodes.next();
        } else {
            Pair<String, QName> subnodeHierarchyKey = new Pair<String, QName>(docTypeId, dynamicAssocQName);
            TreeNode<QName> subnodeHierarchy = childNodeQNameHierarchies.get(subnodeHierarchyKey);
            if (subnodeHierarchy == null) {
                subnodeHierarchy = documentDynamicService.getChildNodeQNameHierarchy(hierarchy, childAssocTypeQNameTree);
                childNodeQNameHierarchies.put(subnodeHierarchyKey, subnodeHierarchy);
            }
            List<Pair<QName, WmNode>> dynamicChildNodes = documentDynamicService.createChildNodesHierarchy(document, Collections.singletonList(subnodeHierarchy), null);
            dynamicChildNode = dynamicChildNodes.get(0).getSecond();
        }
        return dynamicChildNode;
    }

    // no check for field grouping is added here, i.e. we convert regarding given property name only
    // and assume that in destination document type this field belongs to correct field group (if necessary)
    private String addConvertedProp(Map<String, Pair<DynamicPropertyDefinition, Field>> docTypeFields, Map<QName, Serializable> newDocumentProperties, String dynamicPropLocalName,
            Serializable propValue) {
        String actionLog = null;
        Field docTypeField = docTypeFields.get(dynamicPropLocalName).getSecond();
        if (docTypeField != null && FieldType.STRUCT_UNIT == docTypeField.getFieldTypeEnum()) {
            if (propValue != null) {
                // It should always be String value, but just in case handle other values,
                // as these cases shouldn't break entire process
                BaseObject group = docTypeField.getParent();
                boolean isUserTableGroup = (group instanceof FieldGroup) && SystematicFieldGroupNames.USERS_TABLE.equals(((FieldGroup) group).getName())
                        && ((FieldGroup) group).isSystematic();
                if (propValue instanceof String) {
                    propValue = (Serializable) getStructUnitPath((String) propValue, orgStructNameToPath);
                } else if (propValue instanceof List) {
                    List list = (List) propValue;
                    if (!list.isEmpty()) {
                        if (list.size() > 1 || isUserTableGroup) {
                            List<List<String>> convertedValue = new ArrayList<List<String>>();
                            for (Object object : list) {
                                if (object != null && !(object instanceof String)) {
                                    actionLog = getStructUnitErrorLog(propValue, dynamicPropLocalName);
                                }
                                convertedValue.add(object != null ? getStructUnitPath(object.toString(), orgStructNameToPath) : null);
                            }
                            propValue = (Serializable) convertedValue;
                        } else {
                            Object object = list.get(0);
                            if (object != null && !(object instanceof String)) {
                                actionLog = getStructUnitErrorLog(propValue, dynamicPropLocalName);
                            }
                            propValue = (Serializable) (object != null ? (Serializable) getStructUnitPath(object.toString(), orgStructNameToPath) : list);
                        }
                    }
                } else {
                    actionLog = getStructUnitErrorLog(propValue, dynamicPropLocalName);
                    log.error(actionLog);
                    List<String> structUnitList = new ArrayList<String>();
                    structUnitList.add(propValue.toString());
                    propValue = (Serializable) structUnitList;
                }
            }
        }
        if (SINGLE_VALUE_TO_MULTIVALUED.contains(dynamicPropLocalName) && propValue != null && !(propValue instanceof Collection)) {
            List multivaluedPropValue = new ArrayList();
            multivaluedPropValue.add(propValue);
            propValue = (Serializable) multivaluedPropValue;
        }
        newDocumentProperties.put(QName.createQName(DocumentDynamicModel.URI, dynamicPropLocalName), propValue);
        return actionLog;
    }

    public static List<String> getStructUnitPath(String propValue, Map<String, List<String>> orgStructNameToPath) {
        if (StringUtils.isBlank(propValue)) {
            return new ArrayList<String>();
        }
        List<String> orgStructPath = orgStructNameToPath.get(propValue);
        if (orgStructPath != null && !orgStructPath.isEmpty()) {
            return orgStructPath;
        }
        List<String> orgStruct = new ArrayList<String>();
        orgStruct.add(propValue);
        return orgStruct;
    }

    public String getStructUnitErrorLog(Serializable propValue, String dynamicPropLocalName) {
        return "Converting: class=" + propValue.getClass() + ", property=" + dynamicPropLocalName;
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "action", "unknownProperties", "conversionErrors" };
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    public void setDocumentUpdater(DocumentUpdater documentUpdater) {
        this.documentUpdater = documentUpdater;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDocumentChangedTypePropertiesUpdater(DocumentChangedTypePropertiesUpdater documentChangedTypePropertiesUpdater) {
        this.documentChangedTypePropertiesUpdater = documentChangedTypePropertiesUpdater;
    }

    public void setLogAndDeleteObjectsWithMissingType(LogAndDeleteObjectsWithMissingType logAndDeleteObjectsWithMissingType) {
        this.logAndDeleteObjectsWithMissingType = logAndDeleteObjectsWithMissingType;
    }

    public void setContractPartyAssocUpdater(ContractPartyAssocUpdater contractPartyAssocUpdater) {
        this.contractPartyAssocUpdater = contractPartyAssocUpdater;
    }

    public void setRegistrationNumberReinventedUpdater(RegistrationNumberReinventedUpdater registrationNumberReinventedUpdater) {
        this.registrationNumberReinventedUpdater = registrationNumberReinventedUpdater;
    }

    public void setDocumentPartyPropsUpdater(DocumentPartyPropsUpdater documentPartyPropsUpdater) {
        this.documentPartyPropsUpdater = documentPartyPropsUpdater;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setDocumentCompWorkflowSearchPropsUpdater(DocumentCompWorkflowSearchPropsUpdater documentCompWorkflowSearchPropsUpdater) {
        this.documentCompWorkflowSearchPropsUpdater = documentCompWorkflowSearchPropsUpdater;
    }

    public void setDocumentAccessRestrictionUpdater(DocumentAccessRestrictionUpdater documentAccessRestrictionUpdater) {
        this.documentAccessRestrictionUpdater = documentAccessRestrictionUpdater;
    }

    public void setTaskUpdater(TaskUpdater taskUpdater) {
        this.taskUpdater = taskUpdater;
    }

    public void setLogAndDeleteNotExistingWorkflowTasks(LogAndDeleteNotExistingWorkflowTasks logAndDeleteNotExistingWorkflowTasks) {
        this.logAndDeleteNotExistingWorkflowTasks = logAndDeleteNotExistingWorkflowTasks;
    }

    public void setWorkflow25To313DynamicDocTypeUpdater(Workflow25To313DynamicDocTypeUpdater workflow25To313DynamicDocTypeUpdater) {
        this.workflow25To313DynamicDocTypeUpdater = workflow25To313DynamicDocTypeUpdater;
    }

    public void setSmitUpdater(boolean smitUpdater) {
        this.smitUpdater = smitUpdater;
    }

}
