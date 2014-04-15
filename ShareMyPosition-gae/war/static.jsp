<%@page import="net.sylvek.sharemyposition.server.UpdatePositionServletImpl.Cache"%>
<%@page import="com.google.appengine.api.memcache.MemcacheService"%>
<%@page import="java.util.logging.Level"%>
<%@page import="com.google.appengine.api.memcache.ErrorHandlers"%>
<%@page import="com.google.appengine.api.memcache.MemcacheServiceFactory"%>
<%@page import="net.sylvek.sharemyposition.server.UpdatePositionServletImpl"%>
<%@page import="java.lang.Boolean"%>
<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=0;" />
<meta name="apple-mobile-web-app-capable" content="no" />
<link rel="icon" type="image/png" href="icon.png" />
<link rel="apple-touch-icon" href="icon.png" />
<link rel="shortcut icon" href="icon.png">
<link type="text/css" rel="stylesheet" href="client.css">
<title>share my position</title>
<%
    String pos = request.getParameter(UpdatePositionServletImpl.PARAMETER_POSITION);
	final String isTracked = request.getParameter(UpdatePositionServletImpl.PARAMETER_TRACKED);
	final String uuid = request.getParameter(UpdatePositionServletImpl.PARAMETER_UUID);
	long lastTime = -1L;
	String unit = "seconds";
	
	if(Boolean.parseBoolean(isTracked)) {
	    final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
	    syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
	    final Cache cache = Cache.from(syncCache.get(uuid));
	    if(cache.position != null) {
	        pos = cache.position;
	        lastTime = (System.currentTimeMillis() - cache.uptime) / 1000L;
	        if (lastTime > 60) {
	            lastTime = lastTime / 60;
	            unit = "minutes";
	        }
	    }
	}
%>
<% if (Boolean.parseBoolean(isTracked)) { 
    // redirect to dynamic page
	response.sendRedirect("dynamic.jsp?uuid=" + uuid + "&pos=" + pos);
%>
	<meta http-equiv="refresh" content="10;URL=/static.jsp?pos=<%=pos%>&tracked=true&uuid=<%=uuid%>">
<% } %>
<script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-46087713-1']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>
</head>
<body onload="window.scrollTo(0, 1)">
<script type="text/javascript"><!--
google_ad_client = "ca-pub-7256263753683362";
/* ma position */
google_ad_slot = "6102448287";
google_ad_width = 320;
google_ad_height = 50;
//-->
</script>
<script type="text/javascript" src="//pagead2.googlesyndication.com/pagead/show_ads.js"></script>
<div class="title"><span>My position</span><br />
<button onclick="window.location='index.html'" class="button">click here to share your own position</button>
<br />
<img src="http://staticmap.openstreetmap.de/staticmap.php?center=<%=pos%>&zoom=15&size=320x240&markers=<%=pos%>,ol-marker-blue" alt="i am here" />
<br />	
<a href="http://www.openstreetmap.org/?mlat=<%=pos.substring(0, pos.indexOf(",")) %>&mlon=<%=pos.substring(pos.indexOf(",") + 1) %>#map=15/<%=pos.replace(",", "/")%>">click here to open OpenStreetMap</a>
<% if (Boolean.parseBoolean(isTracked) && lastTime > 0) { %>
	<br />refresh every 10 seconds<br />(last update from <%=lastTime %> <%=unit %>)
<% } %>
</div>
</body>
</html>

