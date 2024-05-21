<%@ page import="
     dk.kb.license.config.ServiceConfig"%>
<% 
if (!ServiceConfig.isAdminGuiEnabled()){     
 %>
    <%@ include file="admin_gui_disabled.html" %>
 <%     
 return;
}%>

