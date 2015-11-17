package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getBulkLoadNodeService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.richlist.LazyListDataProvider;
import ee.webmedia.alfresco.common.richlist.PageLoadCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.model.SimpleFile;
import ee.webmedia.alfresco.document.file.model.SimpleFileWithOrder;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.WebUtil;

public class DocumentListDataProvider extends LazyListDataProvider<NodeRef, Document> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Set<QName> propsToLoad;

    private final Map<String, QName> PREFIX_STRING_TO_QNAME = new HashMap<String, QName>();

    private static final Map<String, Set<QName>> SORT_COLUMN_TO_REQUIRED_PROPS;
    private static final Set<QName> NATURAL_ORDER_PROPS = new HashSet<QName>(Arrays.asList(DocumentCommonModel.Props.REG_DATE_TIME, DocumentCommonModel.Props.REG_NUMBER));

    private final Map<String, String> documentTypeToNameMap = new HashMap<String, String>();

    static {
        Map<String, Set<QName>> tmp = new HashMap<String, Set<QName>>();
        tmp.put(DocumentSearchResultsDialog.SENDER_COL, new HashSet<QName>(Arrays.asList(DocumentSpecificModel.Props.SENDER_DETAILS_NAME,
                DocumentSpecificModel.Props.SECOND_PARTY_NAME, DocumentSpecificModel.Props.THIRD_PARTY_NAME, DocumentSpecificModel.Props.PARTY_NAME,
                DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME, Props.OBJECT_TYPE_ID)));
        tmp.put(DocumentSearchResultsDialog.DOCUMENT_TYPE_NAME_COL, Collections.singleton(Props.OBJECT_TYPE_ID));
        tmp.put(DocumentSearchResultsDialog.SEND_MODE_COL, new HashSet<QName>(
                Arrays.asList(DocumentSpecificModel.Props.TRANSMITTAL_MODE, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE)));
        tmp.put(DocumentSearchResultsDialog.SEND_INFO_RECIPIENT_COL, Collections.singleton(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RECIPIENT));
        tmp.put(DocumentSearchResultsDialog.SEND_INFO_SEND_DATE_TIME_COL, Collections.singleton(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_SEND_DATE_TIME));
        tmp.put(DocumentSearchResultsDialog.SEND_INFO_RESOLUTION_COL, Collections.singleton(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RESOLUTION));
        tmp.put(DocumentSearchResultsDialog.HIERARCHICAL_KEYWORDS_COL,
                new HashSet<QName>(Arrays.asList(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL, DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL)));
        tmp.put("ownerOrgStructUnit", Collections.singleton(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT));
        tmp.put(DocumentSearchResultsDialog.DOC_NAME_COL, new HashSet<QName>(Arrays.asList(DocumentCommonModel.Props.DOC_NAME, CaseModel.Props.TITLE)));
        SORT_COLUMN_TO_REQUIRED_PROPS = Collections.unmodifiableMap(tmp);
    }

    protected DocumentListDataProvider() {
    }

    public DocumentListDataProvider(List<NodeRef> documents) {
        super(documents, getCallback(getDocumentFileLoadCallback()));
    }

    public DocumentListDataProvider(List<NodeRef> documents, boolean naturalOrder) {
        this(documents, null, naturalOrder, null);
    }

    public DocumentListDataProvider(List<NodeRef> documents, boolean naturalOrder, Set<QName> propsToLoad) {
        this(documents, null, naturalOrder, propsToLoad);
    }

    public DocumentListDataProvider(List<NodeRef> documents, final PageLoadCallback<NodeRef, Document> pageLoadCallback, boolean naturalOrder, Set<QName> propsToLoad) {
        super(documents, getCallback(pageLoadCallback));
        if (naturalOrder) {
            orderInitial();
        } else {
            orderInitial(true, ContentModel.PROP_CREATED);
        }
        this.propsToLoad = propsToLoad;
    }

    private static PageLoadCallback<NodeRef, Document> getCallback(final PageLoadCallback<NodeRef, Document> pageLoadCallback) {
        PageLoadCallback<NodeRef, Document> callback;
        if (pageLoadCallback != null) {
            callback = new PageLoadCallback<NodeRef, Document>() {

                private final PageLoadCallback loadFilesCallback = getDocumentFileLoadCallback();

                @Override
                public void doWithPageItems(Map<NodeRef, Document> loadedRows) {
                    pageLoadCallback.doWithPageItems(loadedRows);
                    loadFilesCallback.doWithPageItems(loadedRows);

                }
            };
        } else {
            callback = getDocumentFileLoadCallback();
        }
        return callback;
    }

    public DocumentListDataProvider(List<NodeRef> documents, PageLoadCallback<NodeRef, Document> pageLoadCallback) {
        super(documents, pageLoadCallback);
    }

    @Override
    protected Map<NodeRef, Document> loadData(List<NodeRef> documentsToLoad) {
        return getBulkLoadNodeService().loadDocuments(documentsToLoad, propsToLoad);
    }

    private static PageLoadCallback<NodeRef, Document> getDocumentFileLoadCallback() {
        return new PageLoadCallback<NodeRef, Document>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void doWithPageItems(Map<NodeRef, Document> loadedRows) {
                List<NodeRef> documentsToLoadFiles = new ArrayList<NodeRef>();
                for (Map.Entry<NodeRef, Document> entry : loadedRows.entrySet()) {
                    if (!entry.getValue().filesLoaded()) {
                        documentsToLoadFiles.add(entry.getKey());
                    }
                }
                if (!documentsToLoadFiles.isEmpty()) {
                    Map<NodeRef, List<SimpleFileWithOrder>> documentsFiles = BeanHelper.getBulkLoadNodeService().loadActiveFilesWithOrder(documentsToLoadFiles);
                    for (Map.Entry<NodeRef, Document> entry : loadedRows.entrySet()) {
                        NodeRef docRef = entry.getKey();
                        List <SimpleFile> files = new ArrayList<SimpleFile>();
                        List<SimpleFileWithOrder> filesWithOrder = documentsFiles.get(docRef);
                    	Collections.sort(filesWithOrder, Collections.reverseOrder(new Comparator<SimpleFileWithOrder>() {
                    	    public int compare(SimpleFileWithOrder s1, SimpleFileWithOrder s2){
                    	    	Long o1 = s1.getFileOrderInList();
                    	    	Long o2 = s2.getFileOrderInList();
                    	    	return o1==null?Integer.MAX_VALUE:o2==null?Integer.MIN_VALUE:o2.compareTo(o1);
                    	    }
                		}));
                        files.addAll(filesWithOrder);
                        entry.getValue().setFiles(files);
                    }
                }
            }
        };
    }

    @Override
    protected void resetObjectKeyOrder(List<Document> orderedRows) {
        objectKeys.clear();
        for (Document document : orderedRows) {
            objectKeys.add(document.getNodeRef());
        }
    }

    @Override
    protected boolean loadOrderFromDb(String column, boolean descending) {
        if (WebUtil.exceedsLimit(objectKeys, null)) {
            return checkAndSetOrderedList(null);
        }
        List<NodeRef> orderedList = new ArrayList<>();
        if (SORT_COLUMN_TO_REQUIRED_PROPS.containsKey(column)) {
            Set<QName> sortPropsToLoad = SORT_COLUMN_TO_REQUIRED_PROPS.get(column);
            Map<NodeRef, Document> documents = getBulkLoadNodeService().loadDocuments(objectKeys, sortPropsToLoad);
            if (DocumentSearchResultsDialog.DOCUMENT_TYPE_NAME_COL.equals(column)) {
                for (Document document : documents.values()) {
                    String typeId = document.getObjectTypeId();
                    String typeName = documentTypeToNameMap.get(typeId);
                    if (typeName == null) {
                        typeName = BeanHelper.getDocumentAdminService().getDocumentTypeName(typeId);
                        documentTypeToNameMap.put(typeId, typeName);
                    }
                    document.setDocumentTypeName(typeName);
                }

            }
            sortAndFillOrderedList(orderedList, column, documents, descending);
        } else if (DocumentSearchResultsDialog.CASE_LABEL_COL.equals(column)) {
            orderedList = getBulkLoadNodeService().orderByDocumentCaseLabel(objectKeys, descending);
        } else if (DocumentSearchResultsDialog.VOLUME_LABEL_COL.equals(column)) {
            orderedList = getBulkLoadNodeService().orderByDocumentVolumeLabel(objectKeys, descending);
        } else if (DocumentSearchResultsDialog.SERIES_LABEL_COL.equals(column)) {
            orderedList = getBulkLoadNodeService().orderByDocumentSeriesLabel(objectKeys, descending);
        } else if (DocumentSearchResultsDialog.FUNCTION_LABEL_COL.equals(column)) {
            orderedList = getBulkLoadNodeService().orderByDocumentFunctionLabel(objectKeys, descending);
        } else if (DocumentSearchResultsDialog.WORKFLOW_STATE_COL.equals(column)) {
            orderedList = getBulkLoadNodeService().orderByDocumentWorkflowStates(objectKeys, descending);
        } else {
            String propToLoad = column;
            if (propToLoad.contains(";")) {
                propToLoad = propToLoad.substring(propToLoad.indexOf(";") + 1);
            }
            if (!propToLoad.contains(":")) {
                propToLoad = DocumentDynamicModel.PREFIX + propToLoad;
            }
            QName prop = PREFIX_STRING_TO_QNAME.get(propToLoad);
            if (prop == null && !PREFIX_STRING_TO_QNAME.containsKey(propToLoad)) {
                prop = QName.createQName(propToLoad, BeanHelper.getNamespaceService());
                PREFIX_STRING_TO_QNAME.put(propToLoad, prop);
            }
            sortAndFillOrderedList(orderedList, column, getBulkLoadNodeService().loadDocuments(objectKeys, Collections.singleton(prop)), descending);
        }
        return checkAndSetOrderedList(orderedList);
    }

    @Override
    protected NodeRef getKeyFromValue(Document document) {
        return document.getNodeRef();
    }

    /**
     * This method should be called only once when data is loaded, as it may impact performance on large lists.
     * It accepts only certain combinations of properties (see BulkLoadNodeServiceImpl.orderByInitialProperties for details
     */
    public void orderInitial(boolean descending, QName prop) {
        List<NodeRef> orderedList = new ArrayList<NodeRef>();
        Map<NodeRef, Document> documents = getBulkLoadNodeService().loadDocuments(objectKeys, Collections.singleton(prop));
        sortAndFillOrderedList(orderedList, prop.getLocalName(), documents, descending);
        checkAndSetOrderedList(orderedList);
    }

    public void orderInitial(boolean descending, Comparator comparator, QName... props) {
        List<NodeRef> orderedList = new ArrayList<NodeRef>();
        Map<NodeRef, Document> documents = getBulkLoadNodeService().loadDocuments(objectKeys, new HashSet<QName>(Arrays.asList(props)));
        List<Document> orderedDocuments = new ArrayList<Document>(documents.values());
        Collections.sort(orderedDocuments, comparator);
        for (Document orderedDocument : orderedDocuments) {
            orderedList.add(orderedDocument.getNodeRef());
        }
        checkAndSetOrderedList(orderedList);
    }

    public boolean orderInitial() {
        Map<NodeRef, Document> documents = getBulkLoadNodeService().loadDocuments(objectKeys, NATURAL_ORDER_PROPS);
        ArrayList<Document> orderedDocs = new ArrayList<Document>(documents.values());
        Collections.sort(orderedDocs);
        List<NodeRef> orderedDocRefs = new ArrayList<NodeRef>();
        for (Document document : orderedDocs) {
            orderedDocRefs.add(document.getNodeRef());
        }
        return checkAndSetOrderedList(orderedDocRefs);
    }

    public void setPropsToLoad(Set<QName> propsToLoad) {
        this.propsToLoad = propsToLoad;
    }

}
