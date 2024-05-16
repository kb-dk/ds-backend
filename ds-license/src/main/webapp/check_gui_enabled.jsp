<%@ page import="
     dk.kb.license.config.ServiceConfig"%>
     
<% 
if (!ServiceConfig.isAdminGuiEnabled()){ 
    RequestDispatcher dispatcher = request.getRequestDispatcher("admin_gui_disabled.html");
    dispatcher.forward(request, response); 
  }
%>

