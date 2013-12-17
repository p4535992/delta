<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript">
   $jQ("#" + escapeId4JQ("container-content")).live('keydown', function(event) {
      if (event.keyCode == 13) {
          clickFinishButton();
          return false;
      }
   });
</script>
</f:verbatim>

