<%@page import="dk.kb.license.model.v1.AuditLogEntryOutputDto"%>
<%@page pageEncoding="UTF-8"%>
<%@ page import="
     java.util.*,
     java.text.SimpleDateFormat,
     dk.kb.license.storage.*,
     dk.kb.license.Util,    
     dk.kb.license.facade.AuditLogModuleFacade"%>

<%@ include file="check_gui_enabled.jsp" %>

    <%
        String auditId = request.getParameter("auditlogId");   
         AuditLogEntryOutputDto log = AuditLogModuleFacade.getAuditEntryById(Long.parseLong(auditId));
         SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         int i =0;
         String textBefore= log.getTextBefore(); 
         String textAfter= log.getTextAfter();
         if (textBefore==null){
             textBefore ="";
         }
         if (textAfter==null){
             textAfter ="";
         }
         
    %>
   
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <title>Audit log entry</title>
    <script type="text/javascript" src="js/jquery-1.8.3.js"></script>
    <script type="text/javascript" src="js/bootstrap.js"></script>

    <link href="css/bootstrap.min.css" rel="stylesheet" media="screen" />
    <link href="css/licensemodule.css" rel="stylesheet" media="screen" />

</head>
<body>

    <%@ include file="message.jsp" %>
    
<h1>Audit log entry </h1>
    
   <table border="1">
     <tbody>
      <tr>
       <td>Time</td>
       <td><%=dateFormat.format(new Date(log.getModifiedTime()))%></td>      
      </tr>
      <tr>
       <td>User</td>
       <td><%=log.getUserName()%></td>      
    </tr>
      <tr>
       <td>ChangeType</td>
       <td><%=log.getChangeType()%></td>      
    </tr>
      <tr>
       <td>ChangeName</td>
       <td><%=log.getChangeName()%></td>      
    </tr>
    <tr>
       <td>Identifier</td>
       <td><%=log.getIdentifier()%></td>
    </tr>
          <tr>
       <td>ChangeComment</td>
       <td><%=log.getChangeComment()%></td>      
    </tr>

  </tbody>
</table>
<br><br>
Changes:
  <table border="1">
   <thead>
   <tr>
    <td width="50%">Before</td>
    <td width="50%">After</td>
   </tr>
   </thead>
   <tbody>
    <tr>
    <td><%=textBefore.replaceAll("\\n","<br>")%></td>
    <td><%=textAfter.replaceAll("\\n","<br>")%></td>
   </tr>   
  </tbody>  
</html>