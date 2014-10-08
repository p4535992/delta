<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="thesaurus-keyword-search" styleClass="panel-100" label="#{msg.thesaurus_search}" progressive="true" rendered="#{!ThesaurusDetailsDialog.new}">
   <h:inputText id="search-text" styleClass="width250 focus" value="#{ThesaurusDetailsDialog.keywordFilter}" size="35" maxlength="1024" />
   <h:commandButton id="search-btn" value="#{msg.search}" action="#{ThesaurusDetailsDialog.filterKeywords}" style="margin-left: 5px;" styleClass="specificAction" />
   <h:commandButton value="#{msg.show_all}" action="#{ThesaurusDetailsDialog.showAll}" style="margin-left: 5px;"/>
</a:panel>
<a:panel id="thesaurus-details-panel" styleClass="panel-100 with-pager" label="#{msg.thesaurus_data}" progressive="true">
   
   <f:verbatim></span>
         <script type="text/javascript">
         var l1Keywords = <c:out value="${ThesaurusDetailsDialog.level1Keywords}" escapeXml="false" />;        
         </script>
   </f:verbatim>
   <h:panelGrid columns="2" columnClasses="propertiesLabel,">
      <h:panelGroup rendered="#{ThesaurusDetailsDialog.new}">
         <f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
         <h:outputText value="#{msg.thesaurus_name}"  />
      </h:panelGroup>
      <h:inputText value="#{ThesaurusDetailsDialog.thesaurus.name}" rendered="#{ThesaurusDetailsDialog.new}" styleClass="focus" />

      <h:outputText value="#{msg.thesaurus_description}" />
      <h:inputTextarea styleClass="expand19-200" value="#{ThesaurusDetailsDialog.thesaurus.description}" />
   </h:panelGrid>
   
   <a:richList id="keywords" refreshOnBind="true" value="#{ThesaurusDetailsDialog.keywords}" var="keyword" width="100%" rendered="#{!ThesaurusDetailsDialog.new}" altRowStyleClass="recordSetRowAlt" rowStyleClass="recordSetRow" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}">
      <a:column>
      <f:facet name="header">
            <h:outputText value="#{msg.thesaurus_keyword_level_1}" />
         </f:facet>
         <f:verbatim><span class="suggest-wrapper"></f:verbatim>
            <h:inputText id="level1" styleClass="suggest" value="#{keyword.keywordLevel1}" />
         <f:verbatim></span>
         <script type="text/javascript">
         addAutocompleter("dialog:dialog-body:keywords:</f:verbatim><h:outputText value="#{ThesaurusDetailsDialog.andIncreaseRowIndex}" /><f:verbatim>:level1", l1Keywords);        
         </script>
         </f:verbatim>
      </a:column>
      
      <a:column>
         <f:facet name="header">
            <h:outputText value="#{msg.thesaurus_keyword_level_2}" />
         </f:facet>
         <h:inputTextarea id="level2" styleClass="expand19-200 width250" value="#{keyword.keywordLevel2 }" />
      </a:column>
      
      <a:column actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink actionListener="#{ThesaurusDetailsDialog.removeKeyword}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false">
            <f:param name="nodeRef" value="#{keyword.nodeRef}"/>
            <f:param name="keywordLevel1" value="#{keyword.keywordLevel1}"/>
            <f:param name="keywordLevel2" value="#{keyword.keywordLevel2}"/>
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>
   
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="thesaurus-keyword-search" styleClass="panel-100" label="#{msg.thesaurus_search}" progressive="true" rendered="#{!ThesaurusDetailsDialog.new}">
   <h:inputText id="search-text" styleClass="width250 focus" value="#{ThesaurusDetailsDialog.keywordFilter}" size="35" maxlength="1024" />
   <h:commandButton id="search-btn" value="#{msg.search}" action="#{ThesaurusDetailsDialog.filterKeywords}" style="margin-left: 5px;" styleClass="specificAction" />
   <h:commandButton value="#{msg.show_all}" action="#{ThesaurusDetailsDialog.showAll}" style="margin-left: 5px;"/>
</a:panel>
<a:panel id="thesaurus-details-panel" styleClass="panel-100 with-pager" label="#{msg.thesaurus_data}" progressive="true">
   
   <f:verbatim></span>
         <script type="text/javascript">
         var l1Keywords = <c:out value="${ThesaurusDetailsDialog.level1Keywords}" escapeXml="false" />;        
         </script>
   </f:verbatim>
   <h:panelGrid columns="2" columnClasses="propertiesLabel,">
      <h:panelGroup rendered="#{ThesaurusDetailsDialog.new}">
         <f:verbatim><span class="red">*&nbsp;</span></f:verbatim>
         <h:outputText value="#{msg.thesaurus_name}"  />
      </h:panelGroup>
      <h:inputText value="#{ThesaurusDetailsDialog.thesaurus.name}" rendered="#{ThesaurusDetailsDialog.new}" styleClass="focus" />

      <h:outputText value="#{msg.thesaurus_description}" />
      <h:inputTextarea styleClass="expand19-200" value="#{ThesaurusDetailsDialog.thesaurus.description}" />
   </h:panelGrid>
   
   <a:richList id="keywords" refreshOnBind="true" value="#{ThesaurusDetailsDialog.keywords}" var="keyword" width="100%" rendered="#{!ThesaurusDetailsDialog.new}" altRowStyleClass="recordSetRowAlt" rowStyleClass="recordSetRow" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}">
      <a:column>
      <f:facet name="header">
            <h:outputText value="#{msg.thesaurus_keyword_level_1}" />
         </f:facet>
         <f:verbatim><span class="suggest-wrapper"></f:verbatim>
            <h:inputText id="level1" styleClass="suggest" value="#{keyword.keywordLevel1}" />
         <f:verbatim></span>
         <script type="text/javascript">
         addAutocompleter("dialog:dialog-body:keywords:</f:verbatim><h:outputText value="#{ThesaurusDetailsDialog.andIncreaseRowIndex}" /><f:verbatim>:level1", l1Keywords);        
         </script>
         </f:verbatim>
      </a:column>
      
      <a:column>
         <f:facet name="header">
            <h:outputText value="#{msg.thesaurus_keyword_level_2}" />
         </f:facet>
         <h:inputTextarea id="level2" styleClass="expand19-200 width250" value="#{keyword.keywordLevel2 }" />
      </a:column>
      
      <a:column actions="true" styleClass="actions-column">
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink actionListener="#{ThesaurusDetailsDialog.removeKeyword}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false">
            <f:param name="nodeRef" value="#{keyword.nodeRef}"/>
            <f:param name="keywordLevel1" value="#{keyword.keywordLevel1}"/>
            <f:param name="keywordLevel2" value="#{keyword.keywordLevel2}"/>
         </a:actionLink>
      </a:column>

      <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp" />
      <a:dataPager id="pager1" styleClass="pager" />

   </a:richList>
   
>>>>>>> develop-5.1
</a:panel>