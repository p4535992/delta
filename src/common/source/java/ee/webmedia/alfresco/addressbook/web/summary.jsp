<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<r:propertySheetGrid id="node-props" value="#{WizardManager.bean.summary}" columns="1" externalConfig="true" mode="view" labelStyleClass="propertiesLabel" />
<f:verbatim>
<script type="text/javascript">
window.onload = function()
{
   document.getElementById("wizard:finish-button").focus();
}
</script>
</f:verbatim>
