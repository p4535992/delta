<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<a:panel id="page-controls1" styleClass="page-controls" style="font-size:9px">
   <h:outputText value="#{msg.items_per_page}" id="items-txt1" />
   <h:selectOneMenu id="selPageSize" value="#{BrowseBean.pageSizeContentStr}" styleClass="selectWithOnchangeEvent====var el = document.getElementById(currElId); var link = jQuery(el).parent().find('a'); link.click();" >
      <f:selectItem itemLabel="10" itemValue="10" />
      <f:selectItem itemLabel="20" itemValue="20" />
      <f:selectItem itemLabel="50" itemValue="50" />
      <f:selectItem itemLabel="75" itemValue="75" />
      <f:selectItem itemLabel="100" itemValue="100" />
   </h:selectOneMenu>

   <a:panel id="update-content-size-field" style="display: none">
      <!-- There should be only one link here for JQuery to click -->
      <a:actionLink id="updateContentPageSize" value="" actionListener="#{BrowseBean.updateContentPageSize}" />
   </a:panel>
</a:panel>
