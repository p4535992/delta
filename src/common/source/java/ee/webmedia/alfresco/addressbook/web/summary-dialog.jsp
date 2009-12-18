<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<r:propertySheetGrid id="node-props" value="#{DialogManager.bean.summary}" columns="1" externalConfig="true" mode="view" />
<f:verbatim>
<script type="text/javascript">
window.onload = function()
{
   document.getElementById("dialog:finish-button").disabled = false;
   document.getElementById("dialog:cancel-button").focus();
}
</script>
</f:verbatim>
