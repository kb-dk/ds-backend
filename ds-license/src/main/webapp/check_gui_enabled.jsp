<%@ page import="
     dk.kb.license.config.ServiceConfig"%>
<% 
if (!ServiceConfig.isAdminGuiEnabled()){     
 %>
    <%@ include file="admin_gui_disabled.html" %>
 <%     
 return;
}
//Check if user is logged in.

if (request.getSession().getAttribute("oauth_user") == null){
%> 
    <%@ include file="login_keycloak.jsp" %>
<%
 return;
}
%>








