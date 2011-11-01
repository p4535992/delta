package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getExporterService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.ExcludingExporterCrawlerParameters;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.cmr.view.ReferenceType;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.common.web.WMAdminNodeBrowseBean;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;

/**
 * @author Ats Uiboupin
 */
public class DocTypeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocTypeListDialog.class);

    private List<DocumentType> documentTypes;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        initDocumentTypes();
    }

    private void initDocumentTypes() {
        documentTypes = getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
    }

    public void exportDocTypes(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("DocumentTypes export started");
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        boolean success = false;
        try {
            NodeRef docTypesRootRef = null;
            List<NodeRef> excludedNodes = new ArrayList<NodeRef>();
            { // evaluate docTypesRootRef and exclude DocTypeVersions except latest docTypeVersion
                for (DocumentType documentType : getDocumentAdminService().getDocumentTypes(null)) {
                    NodeRef docTypeParentRef = documentType.getParentNodeRef();
                    if (docTypesRootRef == null) {
                        docTypesRootRef = docTypeParentRef;
                    } else if (!docTypesRootRef.equals(docTypeParentRef)) {
                        throw new RuntimeException("Expected that all exportable docTypes were under same parent node");
                    }
                    Integer latestVersion = documentType.getLatestVersion();
                    for (DocumentTypeVersion documentTypeVersion : documentType.getDocumentTypeVersions()) {
                        if (!documentTypeVersion.getVersionNr().equals(latestVersion)) {
                            excludedNodes.add(documentTypeVersion.getNodeRef());
                        }
                    }
                }
            }
            NodeRef documentTypesRootRef = getDocumentAdminService().getDocumentTypesRoot();
            outputStream = WMAdminNodeBrowseBean.getExportOutStream(response, "documentTypes.xml");
            getExporterService().exportView(outputStream, getDocumentExportParameters(documentTypesRootRef, excludedNodes), null);

            outputStream.flush();
            success = true;
        } catch (IOException e) {
            String msg = "Failed to export documentTypes";
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            context.responseComplete();
            // Erko hack for incorrect view id in the next request
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();
            LOG.info("DocumentTypes export completed " + (success ? "" : "un") + "successfully");
        }
    }

    private ExcludingExporterCrawlerParameters getDocumentExportParameters(NodeRef nodeRef, List<NodeRef> excludedNodeRef) {
        ExcludingExporterCrawlerParameters params = new ExcludingExporterCrawlerParameters();
        params.setExportFrom(new Location(nodeRef));
        params.setCrawlSelf(true);
        params.setCrawlContent(false);
        params.setExcludeNodeRefs(excludedNodeRef);
        params.setReferenceType(ReferenceType.NODEREF);
        params.setCrawlNullProperties(true);
        return params;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    @Override
    public String cancel() {
        documentTypes = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        initDocumentTypes();
    }

    /**
     * Query callback method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param substring Text from the search textbox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchUsedDocTypes(int filterIndex, String substring) {
        return searchUsedDocTypes(substring, false);
    }

    /**
     * Used by the property sheet as a callback.
     */
    public List<SelectItem> getUsedDocTypes(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        return Arrays.asList(searchUsedDocTypes(null, true));
    }

    private SelectItem[] searchUsedDocTypes(String substring, boolean addEmptyItem) {
        final List<DocumentType> usedDocTypes = getDocumentAdminService().getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
        substring = StringUtils.trimToNull(substring);
        substring = (substring != null ? substring.toLowerCase() : null);
        int size = addEmptyItem ? usedDocTypes.size() + 1 : usedDocTypes.size();
        final ArrayList<SelectItem> results = new ArrayList<SelectItem>(size);
        if (addEmptyItem) {
            results.add(new SelectItem("", ""));
        }
        for (DocumentType documentType : usedDocTypes) {
            final String name = documentType.getName();
            if (substring == null || name.toLowerCase().contains(substring)) {
                results.add(new SelectItem(documentType.getDocumentTypeId(), name));
            }
        }
        SelectItem[] resultArray = new SelectItem[results.size()];
        int i = 0;
        for (SelectItem selectItem : results) {
            resultArray[i++] = selectItem;
        }

        Arrays.sort(resultArray, new Comparator<SelectItem>() {
            @Override
            public int compare(SelectItem a, SelectItem b) {
                return a.getLabel().compareTo(b.getLabel());
            }
        });
        return resultArray;
    }

    // START: getters / setters
    /**
     * Used in JSP page to create table rows
     */
    public List<DocumentType> getDocumentTypes() {
        return documentTypes;
    }

    // END: getters / setters

}
