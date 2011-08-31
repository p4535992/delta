<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="thesaurus-keyword-search" styleClass="panel-100" label="#{msg.thesaurus_search}" progressive="true" rendered="#{!ThesaurusDetailsDialog.new}">
   <h:inputText id="search-text" styleClass="admin-user-search-input width250" value="#{ThesaurusDetailsDialog.keywordFilter}" size="35" maxlength="1024" />
   <h:commandButton id="search-btn" value="#{msg.search}" action="#{ThesaurusDetailsDialog.filterKeywords}" style="margin-left: 5px;"/>
   <h:commandButton value="#{msg.show_all}" action="#{ThesaurusDetailsDialog.showAll}" style="margin-left: 5px;"/>
</a:panel>

<a:panel id="thesaurus-details-panel" styleClass="panel-100" label="#{msg.thesaurus_data}" progressive="true">
   
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
      <h:inputText value="#{ThesaurusDetailsDialog.thesaurus.name}" rendered="#{ThesaurusDetailsDialog.new}" />

      <h:outputText value="#{msg.thesaurus_description}" />
      <h:inputTextarea styleClass="expand19-200" value="#{ThesaurusDetailsDialog.thesaurus.description}" />
   </h:panelGrid>
   
   <h:dataTable binding="#{ThesaurusDetailsDialog.keywordTable}" id="keywords" value="#{ThesaurusDetailsDialog.keywords}" var="keyword" width="100%" rendered="#{!ThesaurusDetailsDialog.new}" rowClasses="recordSetRow,recordSetRowAlt">
      <h:column>
         <f:facet name="header">
            <h:outputText value="#{msg.thesaurus_keyword_level_1}" />
         </f:facet>
         <f:verbatim><span class="suggest-wrapper"></f:verbatim>
            <h:inputText id="level1" styleClass="suggest" value="#{keyword.keywordLevel1}" />
         <f:verbatim></span>
         <script type="text/javascript">
			addAutocompleter("dialog:dialog-body:keywords:</f:verbatim><h:outputText value="#{ThesaurusDetailsDialog.keywordTable.rowIndex}" /><f:verbatim>:level1", l1Keywords);        
         </script>
         </f:verbatim>
      </h:column>
      
      <h:column>
         <f:facet name="header">
            <h:outputText value="#{msg.thesaurus_keyword_level_2}" />
         </f:facet>
         <h:inputTextarea id="level2" styleClass="expand19-200 width250" value="#{keyword.keywordLevel2 }" />
      </h:column>
      
      <h:column>
         <f:facet name="header">
            <h:outputText value="&nbsp;" escape="false" />
         </f:facet>
         <a:actionLink actionListener="#{ThesaurusDetailsDialog.removeKeyword}" image="/images/icons/delete.gif" value="#{msg.remove}" showLink="false" />
      </h:column>
   </h:dataTable>
   

</a:panel>