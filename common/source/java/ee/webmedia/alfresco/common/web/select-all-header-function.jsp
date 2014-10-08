<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript">

$jQ(document).ready(function(){
   $jQ(".selectAllHeader").change(function() {
      $jQ(".headerSelectable").prop('checked',$jQ(this).prop('checked'));
   });
});

</script>
</f:verbatim>
=======
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
<script type="text/javascript">

$jQ(document).ready(function(){
   $jQ(".selectAllHeader").change(function() {
      $jQ(".headerSelectable").prop('checked',$jQ(this).prop('checked'));
   });
});

</script>
</f:verbatim>
>>>>>>> develop-5.1
