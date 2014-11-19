<<<<<<< HEAD
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

=======
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

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
