<%@ page import="
    java.util.*,
    dk.kb.license.model.v1.RestrictedIdOutputDto,
    dk.kb.license.api.v1.impl.DsRightsApiServiceImpl"%>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>

<% 
String system=request.getParameter("system");
String idType=request.getParameter("idType");

if (system==null){
    system="dr";
}
if (idType==null){
    idType="ds_id";
    
}
DsRightsApiServiceImpl impl = new DsRightsApiServiceImpl();
List<RestrictedIdOutputDto> list = impl.getAllRestrictedIds(idType,system);
%>

<!DOCTYPE html>
<html>
    <title>List restricted IDS</title>    
    <h1>List restricted IDS </h1>   
<br>
 
    <form id="form">
        <label for="idType">Idtype:</label>
        <select name="idType" id="idType-select" onChange="search()";> 
            <option value="ds_id">ds_id</option>
            <option value="dr_produktions_id">dr_produktions_id</option>
            <option value="egenproduktions_kode">egenproduktions_kode</option>
            <option value="strict_title">strict_title</option>
        </select>
    
        <label for="system">System:</label>
        <select name="system" id="system-select" onChange="search()";> 
            <option value="dr">dr</option>
        </select>
     </form>     
<br>
<br>
     
    <table border='1'>
        <th>idValue</th>
        <th>idType</th>
        <th>platform</th>
        <th>comment</th>
        <th>modifiedBy</th>
        <th>modifiedTimeHuman</th>        
    <% for (RestrictedIdOutputDto dto : list){ %>
       <tr>
           <td><%=dto.getIdValue()%> </td>
           <td><%=dto.getIdType()%></td>
           <td><%=dto.getPlatform()%></td>
           <td><%=dto.getComment()%></td>
           <td><%=dto.getModifiedBy()%></td>
           <td><%=dto.getModifiedTimeHuman()%> </td>      
       </tr>
    <%}%>             
    </table>
        
Results:<%=list.size() %>
</body>


<script>
//Set selected values from request parameters.
idType = document.getElementById('idType-select');
idType.value = '<%=idType%>';

system = document.getElementById('system-select');
system.value = '<%=system%>';
</script>

<script>
function search(){
   document.getElementById("form").submit();	
  }
</script>

</html>

