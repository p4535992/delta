<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
<%@ taglib uri="/WEB-INF/wm.tld" prefix="wm" %>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<a:panel id="substitute-panel" styleClass="panel-100 with-pager" label="#{msg.substitute_list}" progressive="true">

    <a:richList id="substituteList" viewMode="details" pageSize="#{BrowseBean.pageSizeContent}" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt"
                width="100%" value="#{DialogManager.bean.substitutes}" var="r" initialSortColumn="substitutionStartDate" initialSortDescending="true" refreshOnBind="true">

        <a:column id="substituteNameCol">
            <f:facet name="header">
                <a:sortLink id="substituteNameSort" label="#{msg.substitute_name}" value="substituteName" styleClass="header" />
            </f:facet>
            <wm:search value="#{r.substituteName}"
                       dataMultiValued="false"
                       dataMandatory="true"
                       pickerCallback="#{UserListDialog.searchUsers}"
                       setterCallback="#{DialogManager.bean.setPersonToSubstitute}"
                       dialogTitleId="users_search_title"
                       editable="false"
                       readonly="#{r.readOnly}"
                    />
        </a:column>

        <a:column id="substitutionStartDateCol">
            <f:facet name="header">
                <a:sortLink id="substitutionStartDateSort" label="#{msg.substitute_startdate}" value="substitutionStartDate" styleClass="header" />
            </f:facet>
            <h:inputText id="substitutionStartDateInput" value="#{r.substitutionStartDate}" styleClass="date" rendered="#{!r.readOnly}">
                <f:converter converterId="ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter"/>
            </h:inputText>
            <h:outputText id="substitutionStartDateOutput" value="#{r.substitutionStartDate}" rendered="#{r.readOnly}">
                <f:converter converterId="ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter"/>
            </h:outputText>
        </a:column>

        <a:column id="substitutionEndDateCol">
            <f:facet name="header">
                <a:sortLink id="substitutionEndDateSort" label="#{msg.substitute_enddate}" value="substitutionEndDate" styleClass="header" />                
            </f:facet>
            <h:inputText id="substitutionEndDateInput" value="#{r.substitutionEndDate}" styleClass="date" rendered="#{!r.readOnly}">
                <f:converter converterId="ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter"/>
            </h:inputText>
            <h:outputText id="substitutionEndDateOutput" value="#{r.substitutionEndDate}" rendered="#{r.readOnly}">
                <f:converter converterId="ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter"/>
            </h:outputText>
        </a:column>

        <a:column id="actionsCol" actions="true" styleClass="actions-column">
            <a:actionLink id="deleteLink" value="#{msg.substitute_remove}" image="/images/icons/delete.gif" showLink="false"
                          tooltip="#{msg.substitute_remove}"
                          actionListener="#{DialogManager.bean.deleteSubstitute}" rendered="#{!r.readOnly}">
                <f:param name="nodeRef" value="#{r.nodeRef}"/>
            </a:actionLink>
            <a:actionLink id="emailLink" value="#{msg.substitute_instruct}" href="mailto:#{DialogManager.bean.emailAddress[r.substituteId]}?subject=#{DialogManager.bean.emailSubject}" image="/images/icons/email_users.gif"
                          tooltip="#{msg.substitute_instruct}" showLink="false"/>
        </a:column>

        <jsp:include page="/WEB-INF/classes/ee/webmedia/alfresco/common/web/page-size.jsp"/>
        <a:dataPager id="pager1" styleClass="pager"/>

    </a:richList>

</a:panel>