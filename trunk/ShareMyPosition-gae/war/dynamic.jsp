<%@page import="net.sylvek.sharemyposition.server.UpdatePositionServletImpl"%>
<%@page import="java.lang.Boolean"%>
<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%
	final String pos = request.getParameter(UpdatePositionServletImpl.PARAMETER_POSITION);
	final String uuid = request.getParameter(UpdatePositionServletImpl.PARAMETER_UUID);
	
	if(pos == null) {
	    response.sendError(400, "pos parameter is mandatory");
	    return;
	}
	
	if(uuid == null) {
	    response.sendError(400, "uuid parameter is mandatory");
	    return;
	}

	String latitude = pos.substring(0, pos.indexOf(","));
	String longitude = pos.substring(pos.indexOf(",") + 1);
%>
<!DOCTYPE html>
<html ng-app="sharemyposition">
	<head>
		<title>Share My Position</title>
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
		<script type="text/javascript" src="angular-1.2.16.min.js"></script>
		<link rel="stylesheet" href="leaflet-0.7.2.css" />
		<script src="leaflet-0.7.2.js"></script>
		<script src="angular-leaflet-directive.min.js"></script>
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
		<style>
			body {
			    padding: 0;
			    margin: 0;
			}
			html, body, #map {
			    height: 100%;
			    width: 100%;
			}
		</style>
	</head>
	<body ng-controller='MapController' data-ng-init="init()">
		<leaflet id="map" defaults="defaults" center="center" markers="markers" paths="paths"></leaflet>
 		<script type="text/javascript">
 			
 			var url = "http://smp-next.appspot.com/service/retrieve?uuid=<%=uuid%>";
 			var beginLongitude = <%=longitude%>;
 			var beginLatitude = <%=latitude%>;
 			
 			var app = angular.module("sharemyposition", ["leaflet-directive"]);
		 	app.controller("MapController", [ "$scope", "$interval", "$http", "leafletData", function($scope, $interval, $http, leafletData) {
		 		
		 		$scope.init = function() {
		 			window.scrollTo(0, 1);
		 			$scope.fetchPosition();
			 		$interval(function() {$scope.fetchPosition()}, 5000);
		 		}
		 		
		 		$scope.fetchPosition = function() {
		 			$http.get(url).success(function(response) {
		 				var newPosition = { lat: response.latitude, lng: response.longitude };
			 			$scope.paths["p1"].latlngs.push(newPosition);
			 			leafletData.getMap().then(function(map) {
			 				//map.fitWorld();
		                    map.panTo(newPosition);
			 			});
				 	});
		 		}
		 	    
		 		angular.extend($scope, {
					defaults: {
					    scrollWheelZoom: false
					},
					center: {
						lat: beginLatitude,
	                    lng: beginLongitude,
					    zoom: 15
					},
					paths: {
			            p1: {
			                color: '#0066FF',
			                weight: 4,
			                latlngs: [{ lat: beginLatitude, lng: beginLongitude }],
			            }
			        },
					markers: {
			            begin: {
			            	lat: beginLatitude,
			                lng: beginLongitude,
			                icon: {
			                    iconUrl: 'pin.png',
			                    iconSize: [32, 32],
			                    iconAnchor: [0, 0],
			                    popupAnchor: [0, 0],
			                    shadowSize: [0, 0],
			                    shadowAnchor: [0, 0]
			                }
			            }
			        }
		 	    });
		 	}]);
		</script>
	</body>
</html>