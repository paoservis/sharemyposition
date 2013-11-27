<%@page import="java.util.Iterator"%>
<%@page import="java.util.concurrent.ConcurrentHashMap"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<!DOCTYPE html>
<%
    String pos = request.getParameter("pos");
    String uuid = request.getParameter("uuid");

    Map<String, String> map = (Map<String, String>) request.getSession().getServletContext().getAttribute("map");
    Map<String, Long> uptime = (Map<String, Long>) request.getSession().getServletContext().getAttribute("uptime");
    if (map == null) {
        response.getWriter().write("map is null");
        map = new ConcurrentHashMap<String, String>();
        request.getSession().getServletContext().setAttribute("map", map);
    }
    if (uptime == null) {
        uptime = new ConcurrentHashMap<String, Long>();
        request.getSession().getServletContext().setAttribute("uptime", uptime);
    }
    if (uuid != null && pos != null) {
        response.getWriter().write(" - put " + uuid + ":" + pos);
        map.put(uuid, pos);
        uptime.put(uuid, System.currentTimeMillis());
    }

    synchronized (uptime) {
        final Iterator<String> keys = uptime.keySet().iterator();
        while (keys.hasNext()) {
            final String key = keys.next();
            final Long time = uptime.get(key);
            if (time != null && time < (System.currentTimeMillis() - 3600000)) {
                keys.remove();
                map.remove(key);
            }
        }
    }
%>
</html>