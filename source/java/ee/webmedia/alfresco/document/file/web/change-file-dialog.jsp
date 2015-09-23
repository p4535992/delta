<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>


<jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/document/file/web/file-block.jsp" />


<a:panel styleClass="column panel-100" id="file-change" label="#{msg.file_change_blockname}">

   <a:richList id="fileChangeList" viewMode="details" value="#{ChangeFileDialog.files}" var="r" 
      rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt">

      <a:column id="fileOrder">
         <f:facet name="header">
            <h:outputText id="col0-header" value="#{msg.file_order}" styleClass="header" />
         </f:facet>
         <h:inputText id="order" value="#{r.fileOrderInList}" styleClass="tiny" >
         </h:inputText>
      </a:column>

      <a:column id="fileName">
         <f:facet name="header">
            <h:outputText id="col1-header" value="#{msg.name}" styleClass="header" />
         </f:facet>
         <h:inputTextarea id="name" value="#{r.fileNameWithoutExtension}" styleClass="focus expand19-200" />
      </a:column>

   </a:richList>

</a:panel>
