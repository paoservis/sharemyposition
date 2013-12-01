<%
String pos = request.getParameter("pos");
String ua = request.getHeader("User-Agent");
String mapLink = "/static.jsp?pos=" + pos;
if(ua.contains("Android")) {
    mapLink = "geo:" + pos + "?q=" + pos;
}
if(ua.contains("Apple-") || ua.contains("Mac OS X")) {
    mapLink = "http://maps.apple.com/?ll=" + pos;
}
if(ua.contains("Windows Phone")) {
    mapLink = "maps:" + pos;
}
response.sendRedirect(mapLink);
%>