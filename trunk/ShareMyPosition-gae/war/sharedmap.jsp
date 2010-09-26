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
	src="http://maps.google.com/maps/api/staticmap?markers=color:blue|label:A|<%=pos%>&zoom=15&mobile=true&size=<%=size%>&maptype=roadmap&sensor=true"
	alt="i am here" /></div>
</body>
</html>