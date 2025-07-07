<%@ page import="
    java.util.*,
    java.text.SimpleDateFormat,
     dk.kb.license.Util,
     dk.kb.license.model.v1.AuditEntryOutputDto,
     dk.kb.license.storage.AuditLogEntry,
     dk.kb.license.storage.BaseModuleStorage,
     dk.kb.license.facade.LicenseModuleFacade"%>

<%@ include file="check_gui_enabled.jsp" %>

<%
ArrayList<AuditEntryOutputDto> logs = LicenseModuleFacade.getAllAuditLogs();
   SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
%>
  <table class="table table-condensed table-hover">
   <thead>
   <tr>
    <th>Id</th>
    <th>ObjectId</th>
    <th>ModifiedTime</th>
    <th>UserName</th>
    <th>ChangeType</th>
    <th>ChangeName</th>    
    <th>ChangeComment</th>
   </tr>
   </thead>
   <tbody>
<%
for (int i = 0;i<logs.size();i++ ){
    AuditEntryOutputDto current = logs.get(i);
%>
   <tr class="<%=Util.getStyle(i)%>" onclick="window.open( 'showlog.jsp?auditlogId=<%=current.getId()%>','_new');">
      <td><%=current.getId()%></td>
      <td><%=current.getObjectId()%></td>
      <td><%=current.getModifiedTime()%></td>
      <td><%=current.getUserName()%></td>
      <td><%=current.getChangeType()%></td>
      <td><%=current.getChangeName()%></td>
      <td><%=current.getChangeComment()%></td>      
  </tr>
<%}%>
   </tbody>
</table>
  
  
  