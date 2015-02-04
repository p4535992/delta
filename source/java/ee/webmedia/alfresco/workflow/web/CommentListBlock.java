package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ValidatingModalLayerComponent;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.Comment;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

public class CommentListBlock extends BaseDialogBean {

    private static final String MODAL_KEY_COMMENT_INDEX_IN_WORKFLOW = "comment-index-in-workflow";
    private static final String MODAL_KEY_COMMENT_ID = "comment-id";

    private static final String MODAL_KEY_COMMENT_TEXT = "compound-workflow-comment-modal-edit";

    public static final String BEAN_NAME = "CommentListBlock";

    private static final long serialVersionUID = 1L;

    private CompoundWorkflow compoundWorkflow;
    private List<Comment> comments = new ArrayList<Comment>();
    private String newComment;

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    public void restored() {
        if (isSavedCompoundWorkflow()) {
            comments = BeanHelper.getWorkflowService().getComments(compoundWorkflow.getNodeRef());
        } else {
            comments = new ArrayList<Comment>();
        }
        newComment = null;
        List<UIComponent> children = ComponentUtil.getChildren(getEditCommentModalContainer());
        children.clear();
        children.add(createEditCommentModalContainer());
    }

    private boolean isSavedCompoundWorkflow() {
        return compoundWorkflow != null && compoundWorkflow.isSaved();
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    public void setup(CompoundWorkflow compoundWorkflow) {
        this.compoundWorkflow = compoundWorkflow;
        restored();
    }

    @Override
    public String cancel() {
        comments = null;
        newComment = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public void addComment(ActionEvent event) {
        if (StringUtils.isNotBlank(newComment)) {
            String userName = AuthenticationUtil.getRunAsUser();
            boolean isSavedCompoundWorkflow = compoundWorkflow.isSaved();
            String compoundWorkflowId = isSavedCompoundWorkflow ? compoundWorkflow.getNodeRef().getId() : null;
            Comment comment = new Comment(compoundWorkflowId, new Date(), userName, BeanHelper.getUserService().getUserFullName(userName), newComment);
            if (isSavedCompoundWorkflow) {
                BeanHelper.getWorkflowService().addCompoundWorkflowComment(comment);
                comments = BeanHelper.getWorkflowService().getComments(compoundWorkflow.getNodeRef());
            } else {
                comment.setIndexInWorkflow(comments.size());
                comments.add(comment);
            }
            newComment = null;
            MessageUtil.addInfoMessage("compoundWorkflow_comment_added_success");
        }
    }

    public void editComment(ActionEvent event) {
        ModalLayerSubmitEvent commentEvent = (ModalLayerSubmitEvent) event;
        String commentText = (String) commentEvent.getSubmittedValue(MODAL_KEY_COMMENT_TEXT);
        if (!compoundWorkflow.isSaved()) {
            String indexInWorkflowStr = (String) commentEvent.getSubmittedValue(MODAL_KEY_COMMENT_INDEX_IN_WORKFLOW);
            Integer indexInWorkflow = Integer.parseInt(indexInWorkflowStr);
            comments.get(indexInWorkflow).setCommentText(commentText);
        } else {
            String commentIdStr = (String) commentEvent.getSubmittedValue(MODAL_KEY_COMMENT_ID);
            Long commentId = Long.parseLong(commentIdStr);
            BeanHelper.getWorkflowService().editCompoundWorkflowComment(commentId, commentText);
            comments = BeanHelper.getWorkflowService().getComments(compoundWorkflow.getNodeRef());
        }
        MessageUtil.addInfoMessage("compoundWorkflow_comment_edited_success");
    }

    /**
     * Getter for JSP.
     */
    public List<Comment> getComments() {
        return comments;
    }

    public String getListTitle() {
        return MessageUtil.getMessage("compoundWorkflow_comment_listTitle");
    }

    public boolean isShowLinkActions() {
        if (compoundWorkflow == null) {
            return false;
        }
        String runAsUser = AuthenticationUtil.getRunAsUser();
        return runAsUser.equals(compoundWorkflow.getOwnerId())
                || BeanHelper.getUserService().isDocumentManager()
                || WorkflowUtil.isOwnerOfInProgressTask(compoundWorkflow, runAsUser);
    }

    private ValidatingModalLayerComponent createEditCommentModalContainer() {
        FacesContext context = FacesContext.getCurrentInstance();
        Application app = context.getApplication();
        ValidatingModalLayerComponent editCommentLayer = (ValidatingModalLayerComponent) app.createComponent(ValidatingModalLayerComponent.class.getCanonicalName());
        editCommentLayer.setId("edit-compound-workflow-comment-layer");
        Map<String, Object> layerAttributes = ComponentUtil.getAttributes(editCommentLayer);
        layerAttributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "compoundWorkflow_comment_edit_modal_title");
        layerAttributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_MSG_KEY, "compoundWorkflow_comment_save");
        layerAttributes.put(ModalLayerComponent.ATTR_STYLE_CLASS, "compound-workflow-comment-modal");

        List<UIComponent> layerChildren = ComponentUtil.getChildren(editCommentLayer);

        TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
        UIInput commentTextInput = (UIInput) textAreaGenerator.generate(context, MODAL_KEY_COMMENT_TEXT);
        Map<String, Object> commentTextAttributes = ComponentUtil.getAttributes(commentTextInput);
        commentTextAttributes.put(ValidatingModalLayerComponent.ATTR_LABEL_KEY, "compoundWorkflow_comment_text");
        commentTextAttributes.put(ValidatingModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
        commentTextAttributes.put("styleClass", "expand19-200 compound-workflow-comment-text-input medium");
        commentTextAttributes.put("style", "height: 50px;");
        commentTextInput.setValue(null);
        layerChildren.add(commentTextInput);

        HtmlInputText indexInWorkflowInput = (HtmlInputText) app.createComponent(HtmlInputText.COMPONENT_TYPE);
        indexInWorkflowInput.setId(MODAL_KEY_COMMENT_INDEX_IN_WORKFLOW);
        Map<String, Object> indexInWorkflowAttributes = ComponentUtil.getAttributes(indexInWorkflowInput);
        indexInWorkflowAttributes.put("styleClass", "hidden compound-workflow-comment-index-in-modal-input");
        layerChildren.add(indexInWorkflowInput);

        HtmlInputText commentIdInput = (HtmlInputText) app.createComponent(HtmlInputText.COMPONENT_TYPE);
        commentIdInput.setId(MODAL_KEY_COMMENT_ID);
        Map<String, Object> commentIdAttributes = ComponentUtil.getAttributes(commentIdInput);
        commentIdAttributes.put("styleClass", "hidden compound-workflow-comment-id-modal-input");
        layerChildren.add(commentIdInput);

        editCommentLayer.setActionListener(app.createMethodBinding("#{CommentListBlock.editComment}", UIActions.ACTION_CLASS_ARGS));

        return editCommentLayer;
    }

    public UIPanel getEditCommentModalContainer() {
        UIPanel editCommentComponent = (UIPanel) getJsfBindingHelper().getComponentBinding(getModalContainerBindingName());
        if (editCommentComponent == null) {
            editCommentComponent = new UIPanel();
            getJsfBindingHelper().addBinding(getModalContainerBindingName(), editCommentComponent);
        }
        return editCommentComponent;
    }

    protected String getModalContainerBindingName() {
        return getBindingName("modalContainer");
    }

    public void setEditCommentModalContainer(UIPanel editCommentModalContainer) {
        getJsfBindingHelper().addBinding(getModalContainerBindingName(), editCommentModalContainer);
    }

    public String getNewComment() {
        return newComment;
    }

    public void setNewComment(String newComment) {
        this.newComment = newComment;
    }

    public boolean isShowEditLinks() {
        // TODO: cache?
        return BeanHelper.getUserService().isDocumentManager();
    }

}
