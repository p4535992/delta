package ee.webmedia.alfresco.common.ajax;

<<<<<<< HEAD
=======
import static org.apache.myfaces.shared_impl.renderkit.ViewSequenceUtils.getCurrentSequence;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.Application;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.event.PhaseId;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.webdav.WebDAVHelper;
import org.alfresco.service.cmr.repository.NodeRef;
<<<<<<< HEAD
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.data.UIRichList;
=======
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.StringUtils;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;
import org.apache.myfaces.shared_impl.util.RestoreStateUtils;
import org.apache.myfaces.shared_impl.util.StateUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
<<<<<<< HEAD
import ee.webmedia.alfresco.document.lock.service.DocLockService;
=======
import ee.webmedia.alfresco.document.service.DocLockService;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import flexjson.JSONSerializer;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 * @author Romet Aidla
 */
public class AjaxBean implements Serializable {
=======
public class AjaxBean implements Serializable {
    public static final String AJAX_REQUEST_PARAM = "ajaxRequest";

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private static final long serialVersionUID = 1L;

    public static final String COMPONENT_CLIENT_ID_PARAM = "componentClientId";
    protected static final String VIEW_NAME_PARAM = "viewName";
    private static final Pattern DATA_CONTAINER_ROW_PATTERN = Pattern.compile(NamingContainer.SEPARATOR_CHAR + "\\d+" + NamingContainer.SEPARATOR_CHAR);

    // ------------------------------------------------------------------------------
    // AJAX handler methods

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void isFileLocked() throws IOException {
<<<<<<< HEAD
=======
        // FIXME XXX This method isn't called when opening WebDAV file in IE! CL 161673
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String path = params.get("path");
<<<<<<< HEAD
        Assert.hasLength(path, "path was not found in request");
=======
        if (StringUtils.isBlank(path)) {
            // Assert.hasLength(path, "path was not found in request");
            // FIXME this may happen, when session has expired, but why?
            return;
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        String[] parts = path.split(WebDAVHelper.PathSeperator);
        String id = parts[parts.length - 2];
        String filename = parts[parts.length - 1];
        NodeRef docRef = BeanHelper.getGeneralService().getExistingNodeRefAllStores(id);
        ResponseWriter out = context.getResponseWriter();
        if (docRef == null) {
            out.write("DOCUMENT_DELETED");
            return;
        }
        NodeRef fileRef = BeanHelper.getFileFolderService().searchSimple(docRef, filename);
        if (fileRef == null) {
            out.write("FILE_DELETED");
            return;
        }

        // If user cannot edit the document, then output nothing
        if (Boolean.FALSE.equals(PrivilegeUtil.additionalDocumentFileWritePermission(docRef, BeanHelper.getNodeService()))) {
            return;
        }

        String lockOwner = null;
        DocLockService docLockService = BeanHelper.getDocLockService();
        boolean generated = BeanHelper.getFileService().isFileGenerated(fileRef);
        lockOwner = docLockService.getLockOwnerIfLocked(generated ? docRef : fileRef);

        if (lockOwner != null) {
            out.write(BeanHelper.getUserService().getUserFullName(lockOwner));
            return;
        }
<<<<<<< HEAD

        // We mustn't lock the node now, since this will block MS Word session from locking it!
        out.write("NOT_LOCKED");
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void getDownloadUrl() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String path = params.get("url");
        Assert.hasLength(path, "url was not found in request");
        ResponseWriter out = context.getResponseWriter();
        if (!path.contains("/webdav/")) {
            out.write(path);
            return;
        }

        String[] parts = path.split(WebDAVHelper.PathSeperator);
        String id = parts[parts.length - 2];
        String filename = parts[parts.length - 1];
        NodeRef docRef = BeanHelper.getGeneralService().getExistingNodeRefAllStores(id);
        if (docRef == null) {
            out.write("DOCUMENT_DELETED");
            return;
        }
        NodeRef fileRef = BeanHelper.getFileFolderService().searchSimple(docRef, filename);
        String requestContextPath = context.getExternalContext().getRequestContextPath();
        String generateDownloadURL = DownloadContentServlet.generateDownloadURL(fileRef, filename);
        out.write(requestContextPath + generateDownloadURL);
=======
        if (generated) {
            docLockService.setLockIfFree(docRef); // Lock the document. File is locked by WebDAV client
        }
        out.write("NOT_LOCKED");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void submit() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String componentClientId = getParam(params, COMPONENT_CLIENT_ID_PARAM);
        String viewName = getParam(params, VIEW_NAME_PARAM);

        Utils.setRequestValidationDisabled(context);
<<<<<<< HEAD

        // Phase 1: Restore view
        UIViewRoot viewRoot = restoreViewRoot(context, viewName);
=======
        ResponseWriter out = context.getResponseWriter();

        // Phase 1: Restore view
        UIViewRoot viewRoot = restoreViewRoot(context, viewName);
        if (viewRoot == null) {
            return;
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        setupDataContainer(context, viewRoot, componentClientId);
        UIComponent component = ComponentUtil.findChildComponentById(context, viewRoot, componentClientId, true);
        Assert.notNull(component, String.format("Component with clientId=%s was not found", componentClientId));
        UIForm form = Utils.getParentForm(context, component);

        // The following is copied from RestoreStateUtils#recursivelyHandleComponentReferencesAndSetValid
        ValueBinding binding = component.getValueBinding("binding");
        if (binding != null && !binding.isReadOnly(context)) {
            binding.setValue(context, component);
        }
        if (component instanceof UIInput) {
            ((UIInput) component).setValid(true);
        }
        RestoreStateUtils.recursivelyHandleComponentReferencesAndSetValid(context, component);

        // If we wish to re-render a container and not the element itself, then we need to execute phases on the container
        UIComponent renderedContainer = getRenderedContainer(context, viewRoot);
        if (renderedContainer == null) {
            renderedContainer = component; // If this is null, just render the component itself.
        }

        // Phase 2: Apply request values; process events
        execute(context, viewRoot, renderedContainer, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                component.processDecodes(context);
                viewRoot._broadcastForPhase(PhaseId.APPLY_REQUEST_VALUES);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Phase 3: Process validations; process events
        execute(context, viewRoot, renderedContainer, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                component.processValidators(context);
                viewRoot._broadcastForPhase(PhaseId.PROCESS_VALIDATIONS);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Phase 4: Update model values; process events
        execute(context, viewRoot, renderedContainer, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                component.processUpdates(context);
                viewRoot._broadcastForPhase(PhaseId.UPDATE_MODEL_VALUES);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Execute callbacks
        executeCallback(context, componentClientId, component);

        // Phase 5: Invoke application; process events
        execute(context, viewRoot, renderedContainer, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                viewRoot._broadcastForPhase(PhaseId.INVOKE_APPLICATION);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Phase 6: Render response
        Utils.encodeRecursive(context, renderedContainer);

        String viewState = saveView(context, viewRoot);
<<<<<<< HEAD
        ResponseWriter out = context.getResponseWriter();
        out.write("VIEWSTATE:" + viewState);
=======
        writeViewState(out, viewState);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        @SuppressWarnings("unchecked")
        Set<String> formHiddenInputs = (Set<String>) context.getExternalContext().getRequestMap().get(
                HtmlFormRendererBase.getHiddenCommandInputsSetName(context, form));
        if (formHiddenInputs == null) {
            formHiddenInputs = Collections.<String> emptySet();
        }
        String jsonHiddenInputNames = new JSONSerializer().serialize(formHiddenInputs);
        out.write("HIDDEN_INPUT_NAMES_JSON:" + jsonHiddenInputNames);
    }

<<<<<<< HEAD
=======
    public static void writeViewState(ResponseWriter out, String viewState) throws IOException {
        out.write("VIEWSTATE:" + viewState);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private void setupDataContainer(FacesContext context, UIViewRoot viewRoot, String componentClientId) {
        Matcher matcher = DATA_CONTAINER_ROW_PATTERN.matcher(componentClientId);
        if (!matcher.find()) {
            return;
        }

        String dataContainerClientId = componentClientId.substring(0, matcher.start());
        UIComponent dataContainer = ComponentUtil.findChildComponentById(context, viewRoot, dataContainerClientId, false);
        if (dataContainer instanceof UIRichList) {
            String indexString = componentClientId.substring(matcher.start() + 1, matcher.end() - 1);
            int index = Integer.parseInt(indexString);
            ((UIRichList) dataContainer).setRowIndex(index);
        }
    }

    protected String getParam(FacesContext context, String paramKey) {
        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        return getParam(params, paramKey);
    }

    protected String getParam(Map<String, String> params, String paramKey) {
<<<<<<< HEAD
        return getParam(params, paramKey, false);
    }

    protected String getParam(Map<String, String> params, String paramKey, boolean canBeEmpty) {
        String param = params.get(paramKey);
        if (!canBeEmpty) {
            Assert.hasLength(param, paramKey + " was not found in request");
        }
=======
        String param = params.get(paramKey);
        Assert.hasLength(param, paramKey + " was not found in request");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        Assert.isTrue(!"undefined".equals(param), paramKey + " was found in request, but with undefined value");
        return param;
    }

    protected UIComponent getRenderedContainer(FacesContext context, UIViewRoot viewRoot) {
        return null;
    }

    private boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component, PhaseExecutor executor) {
        boolean skipFurtherProcessing = false;

        if (context.getResponseComplete()) {
            return true;
        }
        if (context.getRenderResponse()) {
            skipFurtherProcessing = true;
        }

        if (executor.execute(context, viewRoot, component)) {
            return true;
        }

        if (context.getResponseComplete() || context.getRenderResponse()) {
            skipFurtherProcessing = true;
        }
        return skipFurtherProcessing;
    }

    @SuppressWarnings("unused")
    protected void executeCallback(FacesContext context, String componentClientId, UIComponent component) {
        // sub-classes can override if they wish to set metadata dynamically
    }

    private interface PhaseExecutor {
        boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component);
    }

<<<<<<< HEAD
    protected String saveView(FacesContext fc, UIViewRoot viewRoot) {
=======
    public static String saveView(FacesContext fc, UIViewRoot viewRoot) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        StateManager stateManager = fc.getApplication().getStateManager();
        stateManager.saveSerializedView(fc);
        return createViewState(fc, viewRoot);
    }

<<<<<<< HEAD
    protected UIViewRoot restoreViewRoot(FacesContext fc, String viewName) {
        Application application = fc.getApplication();
        ViewHandler viewHandler = application.getViewHandler();

        // Because there is no javax.faces.ViewState request parameter, last view state is restored.
        UIViewRoot viewRoot = viewHandler.restoreView(fc, viewName);
        fc.setViewRoot(viewRoot);
        return viewRoot;
    }

    private String createViewState(FacesContext fc, UIViewRoot viewRoot) {
        // See HtmlResponseStateManager#writeState for meaning of different objects in saved state array.
        // TREE_PARAM is set to null, so that last view state will be restored during next form submit
        // (not the view state that was used to generate the form).
        // It creates dependency to implementation of HtmlResponseStateManager, but no better solution was found.
        Object[] savedState = new Object[3];
=======
    public static UIViewRoot restoreViewRoot(FacesContext fc, String viewName) throws IOException {
        UIViewRoot viewRoot = null;
        fc.getExternalContext().getRequestMap().put(AJAX_REQUEST_PARAM, Boolean.TRUE);
        try {
            Application application = fc.getApplication();
            ViewHandler viewHandler = application.getViewHandler();
            viewRoot = viewHandler.restoreView(fc, viewName);
            if (viewRoot != null) {
                fc.setViewRoot(viewRoot);
            } else {
                writeViewStateError(fc, null);
            }
        } catch (AjaxIllegalViewStateException e) {
            writeViewStateError(fc, e);
        }
        return viewRoot;
    }

    private static void writeViewStateError(FacesContext fc, AjaxIllegalViewStateException e) throws IOException {
        ResponseWriter out = fc.getResponseWriter();
        String currentViewId = e.getCurrentViewId();
        String currentUrl = BeanHelper.getDocumentTemplateService().getServerUrl() + "/faces" + (currentViewId != null ? currentViewId : "");
        fc.getResponseWriter().write("ERROR_VIEW_STATE_CHANGED:" + new JSONSerializer().serialize(currentUrl));
    }

    private static String createViewState(FacesContext fc, UIViewRoot viewRoot) {
        // See HtmlResponseStateManager#writeState for meaning of different objects in saved state array.
        // It creates dependency to implementation of HtmlResponseStateManager, but no better solution was found.
        Object[] savedState = new Object[3];
        Integer currentSequence = getCurrentSequence(fc);
        savedState[0] = currentSequence != null ? currentSequence.toString() : null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        savedState[2] = viewRoot.getViewId();
        return StateUtils.construct(savedState, fc.getExternalContext());
    }
}
