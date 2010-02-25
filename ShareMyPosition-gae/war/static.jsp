<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=0;" />
<meta name="apple-mobile-web-app-capable" content="no" />
<link rel="icon" type="image/png" href="icon.png" />
<link type="text/css" rel="stylesheet" href="client.css">
<title>share my position</title>
<script type="text/javascript"><!--
addEventListener('load', function() { setTimeout(hideAddressBar, 5000); }, false);
function hideAddressBar() { window.scrollTo(0, 1); } //-->
</script>
<script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-817536-13']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    (document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(ga);
  })();

</script>
</head>
<body>
<%
    String pos = request.getParameter("pos");
	String geocode = request.getParameter("geocode");
	if(geocode != null && request.getHeader("user-agent").contains("Android")) {
		geocode = new String(geocode.getBytes("8859_1"),"UTF8");
	}
%>
<!--<script type="text/javascript">-->
<!--window.googleAfmcRequest = {-->
<!--  client: 'ca-mb-pub-7256263753683362',-->
<!--  ad_type: 'text_image',-->
<!--  output: 'html',-->
<!--  channel: '',-->
<!--  format: '320x50_mb',-->
<!--  oe: 'utf8',-->
<!--  color_border: '336699',-->
<!--  color_bg: 'FFFFFF',-->
<!--  color_link: '0000FF',-->
<!--  color_text: '000000',-->
<!--  color_url: '008000',-->
<!--};-->
<!--//</script> <script type="text/javascript"-->
<!--	src="http://pagead2.googlesyndication.com/pagead/show_afmc_ads.js"></script>-->
<script type="text/javascript">
var admob_vars = {
 pubid: 'a14b53a13a8079d', // publisher id
 bgcolor: '356FA8', // background color (hex)
 text: 'FFFFFF', // font-color (hex)
 test: false // test mode, set to false if non-test mode
};
</script>
<script type="text/javascript" src="http://mm.admob.com/static/iphone/iadmob.js"></script>
<div class="title">
<span>share my position</span><br/>
<a href="index.html">click here to share your own position</a>
</div>
<p>
<%
    if (geocode != null) {
%>
<center><h4><%=geocode%></h4></center>
<center><a href="http://maps.google.fr/maps?geocode=&q=<%=geocode%>"> <%
     }
 %>
<img
	src="http://maps.google.com/staticmap?center=<%=pos%>&amp;markers=<%=pos%>,blue&amp;zoom=15&amp;size=320x220&amp;maptype=roadmap&amp;mobile=true&amp;key=ABQIAAAAAEcqvI14a3gJGb3vVQgfdxQX01MF0MrseK3w-nIw2auEB9UHhBSSWixmPtdizdy6aL9TvAbmLmtuzw&amp;sensor=true"
	alt="i am here" /> <br />
<%
    if (geocode != null) {
%> click on the map to open Google Map<a/></center> <%
     }
 %>
</p>
</body>
</html>
