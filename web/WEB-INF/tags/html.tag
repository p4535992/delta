<%@ tag body-content="scriptless" isELIgnored="false" pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tag"%>
 
<!DOCTYPE html>
<html class="no-js">
<head>
<meta charset="utf8">
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate" />
<meta http-equiv="Pragma" content="no-cache" />
<meta http-equiv="Expires" content="0" />
<title>mDelta</title>
<meta name="author" content="Nortal">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<link rel="shortcut icon" href="favicon.ico">
<link rel="stylesheet" href="<c:url value="/mobile/css/main.css" />">
<script src="https://code.jquery.com/jquery-1.10.2.min.js"></script>
<script src='<c:url value="/mobile/js/plugins.js" />'></script>
<script src='<c:url value="/mobile/js/scripts.js" />'></script>
<script src='<c:url value="/mobile/js/mDelta.js" />'></script>
<script>
   document.documentElement.className = document.documentElement.className.replace('no-js', 'js');
   if ('ontouchstart' in window || 'onmsgesturechange' in window)
      document.documentElement.className += ' touch';
</script>
</head>

<body>
    <header>
		<div class="headerwrapper">
			<a id="logo" href='<c:url value="/m" />'>Delta</a>
			<nav>
				<ul class="mainmenu">
					<li><a class="work" href='<c:url value="/m" />'><c:if test="${page.unseenTaskCount gt 0}"><span class="notification">${page.unseenTaskCount}</span></c:if></a></li>
<%-- 					<li><a class="search" href='<c:url value="/m/search" />'>Otsing</a></li> --%>
				</ul>
			</nav>
		</div>
	</header>

	<div id="content-wrap" class="contentwrapper">
        <tag:sidebarMenu entries="${page.menu}" />
        <div class="${empty page.menu ? '' : 'contentmargins'}">
            <h1 id="pageTitle"><c:out value="${page.title}" /></h1>
            <tag:messages messages="${messages}" />

            <jsp:doBody />
        </div>
	</div>

	<footer>
	    <c:out value="${page.footerText}" escapeXml="false" />
        <p class="version"><c:out value="${page.projectVersion}" /></p>
	</footer>
</body>
</html>