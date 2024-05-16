<%@ page import="
    java.util.*,
    java.text.SimpleDateFormat,
     dk.kb.license.Util,
     dk.kb.license.storage.AuditLog,
     dk.kb.license.facade.LicenseModuleFacade"%>

<%@ include file="check_gui_enabled.jsp" %>

<%  
   ArrayList<AuditLog> logs = LicenseModuleFacade.getAllAuditLogs();
   SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
%>
  <table class="table table-condensed table-hover">
   <thead>
   <tr>
    <th>Time</th>
    <th>User</th>
    <th>Changetype</th>
   <th>Object</th>
   </tr>
   </thead>
   <tbody>
<% for (int i = 0;i<logs.size();i++ ){
      AuditLog current = logs.get(i);
%>
   <tr class="<%=Util.getStyle(i)%>" onclick="window.open( 'showlog.jsp?auditlogId=<%=current.getMillis()%>','_new');">
      <td><%=dateFormat.format(new Date(current.getMillis()))%></td>
      <td><%=current.getUsername()%></td>
      <td><%=current.getChangeType()%></td>
      <td><%=current.getObjectName()%></td>
  </tr>
<%}%>
   </tbody>
</table>
  
  
  