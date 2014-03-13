package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDynamicTypeDetailsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getExporterService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.ExcludingExporterCrawlerParameters;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.cmr.view.ReferenceType;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.common.web.WMAdminNodeBrowseBean;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Base dialog for list of {@link DynamicType}s
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
            // hack for incorrect view id in the next request
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
    // END: getters / setters

}
