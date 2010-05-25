<!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<meta name="viewport"
	content="width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=0;" />
</head>
<body style="margin: 0; padding: 0">
<%
    final String pos = request.getParameter("pos");
	final String size = request.getParameter("size");
%>
<div style="margin: 0; padding: 0; overflow: hidden"><img
	src="http://maps.google.com/staticmap?markers=<%=pos%>,blue&amp;zoom=15&amp;mobile=true&amp;size=<%=size%>&amp;maptype=roadmap&amp;mobile=true&amp;key=ABQIAAAAAEcqvI14a3gJGb3vVQgfdxQX01MF0MrseK3w-nIw2auEB9UHhBSSWixmPtdizdy6aL9TvAbmLmtuzw&amp;sensor=true"
	alt="i am here" /></div>
</body>
</html>