<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<h:panelGroup id="comment-modal-container" binding="#{CommentListBlock.editCommentModalContainer}" />

<a:panel id="cwf-comment-panel" styleClass="panel-100 with-pager" label="#{CommentListBlock.listTitle}" progressive="true" 	expanded="true" >

		<h:panelGrid id="comment-add-panel-grid" columns="2" columnClasses="vertical-align-middle padding-3,vertical-align-middle padding-3" styleClass="column panel-boder" cellpadding="20" cellspacing="20">
			<h:outputText id="col0-text" value="#{msg.compoundWorkflow_comment}:" styleClass="propertiesLabel" />
			<h:inputTextarea id="name" value="#{CommentListBlock.newComment}" styleClass="expand19-200" style="padding: 10px;" />
			<h:outputText id="col0-dummy-text" value="#" styleClass="hidden" />
			<h:commandButton id="comment-add-button" value="#{msg.compoundWorkflow_add_comment}" type="submit" actionListener="#{CommentListBlock.addComment}" />
		</h:panelGrid>

	<%-- Main List --%>
   <a:richList id="commentList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
      width="100%" value="#{CommentListBlock.comments}" var="r" refreshOnBind="true" styleClass="panel-100" >
      
      <a:column id="col0" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col0-sort" label="#{msg.compoundWorkflow_comment_created}" value="created" styleClass="header" />
         </f:facet>
         <h:outputText  id="col0-text" value="#{r.createdStr}" />     
      </a:column>
      
      <a:column id="col1" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col1-sort" label="#{msg.compoundWorkflow_comment_creatorName}" value="creatorName" styleClass="header" />
         </f:facet>
         <h:outputText  id="col1-text" value="#{r.creatorName}" />
      </a:column>       
      
      <a:column id="col2" primary="true" >
         <f:facet name="header">
            <a:sortLink id="col2-sort" label="#{msg.compoundWorkflow_comment_commentText}" value="commentText" styleClass="header" />
         </f:facet>
         <h:outputText  id="col2-text" value="#{r.commentText}" styleClass="condence150" />
      </a:column>
      
      <a:column id="col-comment-actions" actions="true" styleClass="actions-column2">
            <a:actionLink id="col-actions-edit" value="" onclick="openCommentEditModal('#{r.indexInWorkflow}', '#{r.commentId}', '#{r.commentTextEscapeJs }'); return false;" showLink="false" 
                    image="/images/icons/edit_properties.gif" tooltip="#{msg.compoundWorkflow_comment_edit}" rendered="#{CommentListBlock.showEditLinks or r.showEditLink}" />  
       </a:column>       

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager-cwf-related-url-block" styleClass="pager" />
   </a:richList>

	<f:verbatim>
		<script type="text/javascript">
			function openCommentEditModal(indexInWorkflow, commentId, commentText){
			   var modalElement = $jQ(".compound-workflow-comment-modal").get(0);
			   var commentTextInput = $jQ(".compound-workflow-comment-text-input").get(0);
			   var indexInWorkflowInput = $jQ(".compound-workflow-comment-index-in-modal-input").get(0);
			   var commentIdInput = $jQ(".compound-workflow-comment-id-modal-input").get(0);
			   if (modalElement == undefined || commentTextInput == undefined || indexInWorkflowInput == undefined || commentIdInput == undefined) {
			      return false;
			   }
			   commentTextInput.value = commentText;
			   indexInWorkflowInput.value = indexInWorkflow;
			   commentIdInput.value = commentId;
			   var modalId = modalElement.id;
			   showModal(modalElement.id);
			   initExpanders($jQ("#" + escapeId4JQ(modalId)));
			}
	</script>
	</f:verbatim>

</a:panel>