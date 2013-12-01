<%@page import="java.util.Map"%>
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
    String pos = request.getParameter("pos");
	String isTracked = request.getParameter("tracked");
	String uuid = request.getParameter("uuid");
	long lastTime = -1L;
	String unit = "seconds";
	
	if(Boolean.parseBoolean(isTracked)) {
	    Map<String, String> map = (Map<String, String>)request.getSession().getServletContext().getAttribute("map");
	    Map<String, Long> uptime = (Map<String, Long>) request.getSession().getServletContext().getAttribute("uptime");
	    if(map != null && map.containsKey(uuid)) {
	        pos = map.get(uuid);
	    }
	    if(uptime != null && uptime.containsKey(uuid)) {
	        lastTime = (System.currentTimeMillis() - uptime.get(uuid)) / 1000L;
	        if (lastTime > 60) {
	            lastTime = lastTime / 60;
	            unit = "minutes";
	        }
	    }
	}
%>
<% if (Boolean.parseBoolean(isTracked)) { %>
	<meta http-equiv="refresh" content="10">
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
<a href="http://maps.google.com/maps?geocode=&q=<%=pos%>">click here to open Google Maps</a>
<br />	
<a href="http://maps.apple.com/?ll=<%=pos%>">click here to open Apple Maps</a>
<br />
<a href="http://www.openstreetmap.org/?mlat=<%=pos.substring(0, pos.indexOf(",")) %>&mlon=<%=pos.substring(pos.indexOf(",") + 1) %>#map=15/<%=pos.replace(",", "/")%>">click here to open OpenStreetMap</a>
<% if (Boolean.parseBoolean(isTracked) && lastTime > 0) { %>
	<br />refresh every 10 seconds<br />(last update from <%=lastTime %> <%=unit %>)
<% } %>
</div>
</body>
</html>

