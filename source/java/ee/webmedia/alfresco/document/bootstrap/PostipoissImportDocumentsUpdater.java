package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Update documents that are imported from Postipoiss.
 * 
 * @author Kaarel Jõgeva
 */
public class PostipoissImportDocumentsUpdater extends AbstractNodeUpdater {

    private static final String LEAVING_LETTER_NAME = "Lahkumisleht";

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    private GeneralService generalService;

    private boolean enabledCustom;
    private Date importStartDate = null;
    private Date importEndDate = null;

    @Override
    protected void executeInternal() throws Throwable {
        if (!isEnabledCustom()) {
            log.debug("Skipping node updater, because it is disabled" + (isExecuteOnceOnly() ? ". It will not be executed again, because executeOnceOnly=true" : ""));
            return;
        }
        super.executeInternal();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // Date values can't be null, since parser would throw an exception then
        List<String> queryParts = Arrays.asList(
                SearchUtil.generateTypeQuery(DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC, DocumentSubtypeModel.Types.LEAVING_LETTER,
                        DocumentSubtypeModel.Types.TRAINING_APPLICATION, DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD),
                SearchUtil.generateDatePropertyRangeQuery(importStartDate, importEndDate, ContentModel.PROP_CREATED)
                );
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));

        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();

        Pair<Boolean, String[]> result = updateDocument(nodeRef, type, origProps, setProps);

        if (result.getFirst()) {
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(nodeRef, setProps);
        }

        return result.getSecond();
    }

    public Pair<Boolean, String[]> updateDocument(NodeRef nodeRef, QName type, Map<QName, Serializable> origProps, Map<QName, Serializable> setProps) {
        boolean modified = false;

        if (DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC.equals(type) && createdBetween(origProps)) {
            NodeRef childRef = getFirstGrandchild(nodeRef, DocumentSpecificModel.Types.ERRAND_APPLICATION_DOMESTIC_APPLICANT_TYPE,
                    DocumentSpecificModel.Types.ERRANDS_DOMESTIC_TYPE);
            if (childRef != null) {
                String comment = (String) nodeService.getProperty(childRef, DocumentSpecificModel.Props.ERRAND_COMMENT);
                if (StringUtils.isNotBlank(comment)) {
                    setProps.put(DocumentCommonModel.Props.DOC_NAME, comment);
                    modified = true;
                }
            }
        } else if (DocumentSubtypeModel.Types.LEAVING_LETTER.equals(type) && createdBetween(origProps)) {
            setProps.put(DocumentCommonModel.Props.DOC_NAME, LEAVING_LETTER_NAME);
            modified = true;
        } else if (DocumentSubtypeModel.Types.TRAINING_APPLICATION.equals(type) && createdBetween(origProps)
                && StringUtils.isNotBlank((String) origProps.get(DocumentSpecificModel.Props.TRAINING_NAME))) {
            setProps.put(DocumentCommonModel.Props.DOC_NAME, origProps.get(DocumentSpecificModel.Props.TRAINING_NAME));
            modified = true;
        } else if (DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD.equals(type) && createdBetween(origProps)) {
            NodeRef childRef = getFirstGrandchild(nodeRef, DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD,
                    DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE);
            if (childRef != null) {
                Map<QName, Serializable> props = nodeService.getProperties(childRef);
                StringBuffer sb = new StringBuffer();
                if (StringUtils.isNotBlank((String) props.get(DocumentSpecificModel.Props.ERRAND_COUNTRY))) {
                    sb.append((String) props.get(DocumentSpecificModel.Props.ERRAND_COUNTRY));
                }
                if (StringUtils.isNotBlank((String) props.get(DocumentSpecificModel.Props.ERRAND_CITY))) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append((String) props.get(DocumentSpecificModel.Props.ERRAND_CITY));
                }
                if (StringUtils.isNotBlank((String) props.get(DocumentSpecificModel.Props.ERRAND_COMMENT))) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append((String) props.get(DocumentSpecificModel.Props.ERRAND_COMMENT));
                }
                if (sb.length() > 0) {
                    setProps.put(DocumentCommonModel.Props.DOC_NAME, sb.toString());
                    modified = true;
                }
            }
        }

        // Log previous document name. Just in case... :)
        String[] info = modified ? new String[] { "docNameChanged", (String) origProps.get(DocumentCommonModel.Props.DOC_NAME) } : new String[] { "docNamePreserved" };
        return new Pair<Boolean, String[]>(modified, info);
    }

    private boolean createdBetween(Map<QName, Serializable> origProps) {
        final Date created = (Date) origProps.get(ContentModel.PROP_CREATED);
        return importStartDate.before(created) && importEndDate.after(created);
    }

    protected NodeRef getFirstGrandchild(NodeRef nodeRef, QName childType, QName grandchildType) {
        List<ChildAssociationRef> applicants = nodeService.getChildAssocs(nodeRef, new HashSet<QName>(Arrays.asList(childType)));
        NodeRef childRef = null;
        if (!applicants.isEmpty()) {
            childRef = applicants.get(0).getChildRef();

        }
        if (childRef != null) {
            List<ChildAssociationRef> applications = nodeService.getChildAssocs(childRef, new HashSet<QName>(Arrays.asList(grandchildType)));
            childRef = (applications.isEmpty()) ? null : applications.get(0).getChildRef();
        }
        return childRef;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setEnabledCustom(boolean enabledCustom) {
        this.enabledCustom = enabledCustom;
    }

    public boolean isEnabledCustom() {
        return enabledCustom;
    }

    public void setImportStartDate(Date importStartDate) {
        this.importStartDate = importStartDate;
    }

    public void setImportEndDate(Date importEndDate) {
        this.importEndDate = importEndDate;
    }

}
