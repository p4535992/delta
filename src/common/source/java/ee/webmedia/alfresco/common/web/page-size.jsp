<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="page-controls1" styleClass="page-controls" style="font-size:9px">
   <h:outputText value="#{msg.items_per_page}" id="items-txt1" />
   <h:selectOneMenu id="content-pages" value="#{BrowseBean.pageSizeContentStr}" onchange="return applySizeContent(event);">
      <f:selectItem id="item11" itemLabel="10" itemValue="10" />
      <f:selectItem id="item12" itemLabel="20" itemValue="20" />
      <f:selectItem id="item13" itemLabel="50" itemValue="50" />
      <f:selectItem id="item14" itemLabel="75" itemValue="75" />
      <f:selectItem id="item15" itemLabel="100" itemValue="100" />
   </h:selectOneMenu>

   <a:panel id="update-content-size-field" style="display: none">
      <a:actionLink id="content-apply" value="" actionListener="#{BrowseBean.updateContentPageSize}" />
   </a:panel>
</a:panel>
