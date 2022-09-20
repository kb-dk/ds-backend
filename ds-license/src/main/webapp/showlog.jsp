<%@page pageEncoding="UTF-8"%>
<%@ page import="
     java.util.*,
     java.text.SimpleDateFormat,
     dk.kb.license.storage.*,
     dk.kb.license.Util,    
     dk.kb.license.facade.LicenseModuleFacade"%>

    <%
     String auditId = request.getParameter("auditlogId");   
     AuditLog log = LicenseModuleFacade.getAuditLog(Long.parseLong(auditId));
     SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
     int i =0;
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
    
   <table class="table table-condensed table-hover">
     <tbody>
      <tr class="<%=Util.getStyle(i++)%>">
       <td>Time</td>
       <td><%=dateFormat.format(new Date(log.getMillis()))%></td>      
      </tr>
    <tr class="<%=Util.getStyle(i++)%>">
       <td>User</td>
       <td><%=log.getUsername()%></td>      
    </tr>
   <tr class="<%=Util.getStyle(i++)%>">
       <td>Change type</td>
       <td><%=log.getChangeType()%></td>      
    </tr>
   <tr class="<%=Util.getStyle(i++)%>">
       <td>Object name</td>
       <td><%=log.getObjectName()%></td>      
    </tr>

  </tbody>
</table>

Changes:
  <table class="table table-condensed table-hover">
   <thead>
   <tr>
    <td width="50%">Before</td>
    <td width="50%">After</td>
   </tr>
   </thead>
   <tbody>
    <tr>
    <td><%=log.getTextBefore().replaceAll("\\n","<br>")%></td>
    <td><%=log.getTextAfter().replaceAll("\\n","<br>")%></td>
   </tr>   
  </tbody>  
</html>