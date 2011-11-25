package ee.webmedia.alfresco.docadmin.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docdynamic.web.DialogBlockBean;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComparableTransformer;

/**
 * Shows the list of saved {@link DocumentTypeVersion} under {@link DynamicType}
 * 
 * @author Ats Uiboupin
 */
public class VersionsListBean<D extends DynamicType> implements DialogBlockBean<D> {
    private static final long serialVersionUID = 1L;

    // TODO ALSeadist Ats - ümber nimetada
    private D dynType;
    private List<DocumentTypeVersionListItem> savedVersionsList;
    private final Class<D> dynTypeClass;

    public VersionsListBean(Class<D> dynTypeClass) {
        this.dynTypeClass = dynTypeClass;
    }

    @Override
    public void resetOrInit(D docType) {
        if (dynType != docType) {
            // no need to reinitialize savedVersionsList when closing previous version of DynamicType
            dynType = docType;
            savedVersionsList = null;
        }
    }

    public void viewTypeVersion(ActionEvent event) {
        NodeRef docTypeVersionRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        BeanHelper.getDynamicTypeDetailsDialog(dynTypeClass).init(dynType, docTypeVersionRef);
    }

    public List<DocumentTypeVersionListItem> getSavedVersionsList() {
        if (savedVersionsList == null) {
            ChildrenList<DocumentTypeVersion> documentTypeVersions = dynType.getDocumentTypeVersions();
            if (documentTypeVersions == null) {
                return Collections.emptyList();
            }
            savedVersionsList = new ArrayList<DocumentTypeVersionListItem>(documentTypeVersions.size());
            for (DocumentTypeVersion documentTypeVersion : documentTypeVersions) {
                if (documentTypeVersion.isSaved()) {
                    savedVersionsList.add(new DocumentTypeVersionListItem(documentTypeVersion));
                }
            }
            @SuppressWarnings("unchecked")
            Comparator<DocumentTypeVersionListItem> byVersionComparator = new TransformingComparator(new ComparableTransformer<DocumentTypeVersionListItem>() {
                @Override
                public Comparable<?> tr(DocumentTypeVersionListItem input) {
                    return input.getVersionNr();
                }
            }, new NullComparator());
            Collections.sort(savedVersionsList, byVersionComparator);
        }
        return savedVersionsList;
    }

    /**
     * UI Wrapper for showing extra information in versions list
     * 
     * @author Ats Uiboupin
     */
    public static class DocumentTypeVersionListItem extends DocumentTypeVersion {
        private static final long serialVersionUID = 1L;

        public DocumentTypeVersionListItem(DocumentTypeVersion documentTypeVersion) {
            super(documentTypeVersion.getParent(), documentTypeVersion.getNode());
        }

        public String getCreatorNameAndId() {
            String creatorId = getCreatorId();
            return BeanHelper.getUserService().getUserFullNameAndId(creatorId);
        }
    }

}
