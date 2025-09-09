<%@ page import="
    dk.kb.license.model.v1.*,
    java.util.*,
    dk.kb.license.model.v1.RestrictedIdOutputDto,
    dk.kb.license.api.v1.impl.DsRightsApiServiceImpl"%>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>

<% 
String systemStr=request.getParameter("system");
String idTypeStr=request.getParameter("idType");

if (systemStr==null){
    systemStr="DRARKIV";
}
if (idTypeStr==null){
    idTypeStr="DS_ID";
    
}
DsRightsApiServiceImpl impl = new DsRightsApiServiceImpl();

PlatformEnumDto platform = PlatformEnumDto.fromValue(systemStr);
IdTypeEnumDto idType = IdTypeEnumDto.fromValue(idTypeStr);

List<RestrictedIdOutputDto> list = impl.getAllRestrictedIds(idType,platform);
%>

<!DOCTYPE html>
<html>
    <title>List restricted IDS</title>    
    <h1>List restricted IDS </h1>   
<br>
 
    <form id="form">
        <label for="idType">Idtype:</label>
        <select name="idType" id="idType-select" onChange="search()";> 
            <option value="DS_ID">dr_id</option>   
            <option value="DR_PRODUCTION_ID">dr_produktions_id</option>
            <option value="OWNPRODUCTION_CODE">egenproduktions_kode</option>
            <option value="STRICT_TITLE">strict_title</option>
        </select>
    
        <label for="system">System:</label>
        <select name="system" id="system-select" onChange="search()";>        
          <option value="DRARKIV">DRARKIV</option>
          <option value="GENERIC">GENERIC</option>                  
        </select>
     </form>     
<br>
<br>
     
    <table border='1'>
        <th>idValue</th>
        <th>idType</th>
        <th>platform</th>
        <th>comment</th>              
    <% for (RestrictedIdOutputDto dto : list){ %>
       <tr>
           <td><%=dto.getIdValue()%> </td>
           <td><%=dto.getIdType()%></td>
           <td><%=dto.getPlatform()%></td>
           <td><%=dto.getComment()%></td>            
       </tr>
    <%}%>             
    </table>
        
Results:<%=list.size() %>
</body>


<script>
//Set selected values from request parameters.
idType = document.getElementById('idType-select');
idType.value = '<%=idTypeStr%>';

system = document.getElementById('system-select');
system.value = '<%=systemStr%>;
</script>

<script>
function search(){
   document.getElementById("form").submit();	
  }
</script>

</html>

