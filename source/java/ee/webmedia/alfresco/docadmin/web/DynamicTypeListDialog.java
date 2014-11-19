package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDynamicTypeDetailsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getExporterService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
<<<<<<< HEAD
import java.util.Arrays;
import java.util.Comparator;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
<<<<<<< HEAD
import javax.faces.model.SelectItem;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.ExcludingExporterCrawlerParameters;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.cmr.view.ReferenceType;
import org.alfresco.web.bean.dialog.BaseDialogBean;
<<<<<<< HEAD
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
=======
import org.apache.commons.io.IOUtils;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.common.web.WMAdminNodeBrowseBean;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Base dialog for list of {@link DynamicType}s
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public abstract class DynamicTypeListDialog<T extends DynamicType> extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DynamicTypeListDialog.class);

    private final Class<T> typeClass;
    protected List<T> types;

    protected DynamicTypeListDialog(Class<T> typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        initDocumentTypes();
    }

    private void initDocumentTypes() {
        types = getDocumentAdminService().getTypes(typeClass, DocumentAdminService.DONT_INCLUDE_CHILDREN);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    @Override
    public String cancel() {
        types = null;
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

    public void showDetails(ActionEvent event) {
        getDynamicTypeDetailsDialog(typeClass).init(ActionUtil.getParam(event, "nodeRef", NodeRef.class));
    }

<<<<<<< HEAD
    protected SelectItem[] searchUsedTypes(PickerSearchParams params, boolean addEmptyItem) {
        String substring = params == null ? null : params.getSearchString();
        final List<? extends DynamicType> usedTypes = loadUsedTypes();
        substring = StringUtils.trimToNull(substring);
        substring = (substring != null ? substring.toLowerCase() : null);
        int size = addEmptyItem ? usedTypes.size() + 1 : usedTypes.size();
        final ArrayList<SelectItem> results = new ArrayList<SelectItem>(size);
        if (addEmptyItem) {
            results.add(new SelectItem("", ""));
        }
        for (DynamicType type : usedTypes) {
            final String name = type.getName();
            if (substring == null || name.toLowerCase().contains(substring)) {
                results.add(new SelectItem(type.getId(), name));
            }
            if (params != null && results.size() == (params.getLimit() + (addEmptyItem ? 1 : 0))) {
                break;
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

    protected abstract List<? extends DynamicType> loadUsedTypes();

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    // START: Export related stuff
    public void export(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("DocumentTypes export started");
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        boolean success = false;
        try {
            List<NodeRef> excludedNodes = new ArrayList<NodeRef>();
            { // evaluate docTypesRootRef and exclude DocTypeVersions except latest docTypeVersion
                for (T documentType : getDocumentAdminService().getTypes(typeClass, DocumentAdminService.DOC_TYPE_WITHOUT_OLDER_DT_VERSION_CHILDREN)) {
                    Integer latestVersion = documentType.getLatestVersion();
                    for (DocumentTypeVersion documentTypeVersion : documentType.getDocumentTypeVersions()) {
                        if (!documentTypeVersion.getVersionNr().equals(latestVersion)) {
                            excludedNodes.add(documentTypeVersion.getNodeRef());
                        }
                    }
                }
            }

            NodeRef documentTypesRootRef = getDocumentAdminService().getDynamicTypesRoot(typeClass);
            outputStream = WMAdminNodeBrowseBean.getExportOutStream(response, getExportFileName());
            getExporterService().exportView(outputStream, getDynamicTypesExportParameters(documentTypesRootRef, excludedNodes), null);

            outputStream.flush();
            success = true;
        } catch (IOException e) {
            String msg = "Failed to export types";
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            context.responseComplete();
<<<<<<< HEAD
            // Erko hack for incorrect view id in the next request
=======
            // hack for incorrect view id in the next request
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();
            LOG.info("DocumentTypes export completed " + (success ? "" : "un") + "successfully");
        }
    }

    protected abstract String getExportFileName();

    private ExcludingExporterCrawlerParameters getDynamicTypesExportParameters(NodeRef nodeRef, List<NodeRef> excludedNodeRef) {
        ExcludingExporterCrawlerParameters params = new ExcludingExporterCrawlerParameters();
        params.setExportFrom(new Location(nodeRef));
        params.setCrawlSelf(true);
        params.setCrawlContent(false);
        params.setExcludeNodeRefs(excludedNodeRef);
        params.setReferenceType(ReferenceType.NODEREF);
        params.setCrawlNullProperties(true);
        return params;
    }

    // END: Export related stuff

    // START: getters / setters
    /**
     * Used in JSP page to create table rows
     */
    public List<T> getTypes() {
        return types;
    }
<<<<<<< HEAD

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    // END: getters / setters

}
