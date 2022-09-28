<%
ArrayList<GroupType> configuredGroups = LicenseCache.getConfiguredLicenseGroupTypes();
 ArrayList<PresentationType> configuredLicenseTypes = LicenseCache.getConfiguredLicenseTypes();

 //variable 'license' is already known here, from the page that included this page
 ArrayList<LicenseContent> groupsForLicense =  license.getLicenseContents();

int countPakker=0;
int countKlausulering=0;
%>

<h2>Pakker</h2>
<table class="table table-condensed table-hover">
   <thead>
   <tr>
    <th class="domsGroups">Pakke</th>
    <th class="allowedGroups">Tiladte pr&aelig;sentationsgrupper</th>
   </tr>
   </thead>
   <tbody>
   <%
for (int i=0;i<configuredGroups.size();i++){
  String currentGroupKey = configuredGroups.get(i).getKey();
  String currentGroupName = configuredGroups.get(i).getValue_dk();
  if (configuredGroups.get(i).isDenyGroup()){
      continue;
  }
%>
<tr class="<%=Util.getStyle(countPakker++)%>">
  <td  class="domsGroups">
    <label class="checkbox">
      <input type="checkbox" <%if(Util.groupsContainGroupName(groupsForLicense, currentGroupKey)){out.println("checked");}%> name="domsGruppe_<%=i%>"> <%=currentGroupName%>
    </label>
  </td>
  <td class="allowedGroups">
   <%
   for (int j=0;j<configuredLicenseTypes.size();j++){
   String presentationTypeKey=configuredLicenseTypes.get(j).getKey();
    String presentationTypeName=configuredLicenseTypes.get(j).getValue_dk();
   %>
  <label class="checkbox">
      <input type="checkbox"  <%if(Util.groupsContainsGroupWithLicense(groupsForLicense, currentGroupKey,presentationTypeKey)){out.println("checked");}%> name="domsGruppe_<%=i%>_license_<%=j%>"> <%=presentationTypeName%>
    </label>
   <%}%>
 </td>
</tr>
<%}%>
</tbody>
</table>

<h2>Klausuleringer</h2>
<table class="table table-condensed table-hover">
   <thead>
   <tr>
    <th class="domsGroups">Klausulering</th>
    <th class="allowedGroups">Tiladte pr&aelig;sentationsgrupper</th>
   </tr>
   </thead>
   <tbody>
   <%
for (int i=0;i<configuredGroups.size();i++){
  String currentGroupKey = configuredGroups.get(i).getKey();
  String currentGroupName = configuredGroups.get(i).getValue_dk();
 
  if (!configuredGroups.get(i).isDenyGroup()){
      continue;
  }

%>
<tr class="<%=Util.getStyle(countKlausulering++)%>">
  <td  class="domsGroups">
    <label class="checkbox">
      <input type="checkbox" <%if(Util.groupsContainGroupName(groupsForLicense, currentGroupKey)){out.println("checked");}%> name="domsGruppe_<%=i%>"> <%=currentGroupName%> 
    </label>
  </td>
  <td class="allowedGroups">
   <%
   for (int j=0;j<configuredLicenseTypes.size();j++){
   String presentationTypeKey=configuredLicenseTypes.get(j).getKey();
    String presentationTypeName=configuredLicenseTypes.get(j).getValue_dk();
   %>
  <label class="checkbox">
      <input type="checkbox"  <%if(Util.groupsContainsGroupWithLicense(groupsForLicense, currentGroupKey,presentationTypeKey)){out.println("checked");}%> name="domsGruppe_<%=i%>_license_<%=j%>"> <%=presentationTypeName%>
    </label>
   <%}%>
 </td>
</tr>
<%}%>
</tbody>
</table>

<input class="btn btn-primary" type="button" value="Gem" onclick="javascript: save();"/>
<a class="btn btn-small" href="configuration.jsp">Fortryd</a>
<% if (license.getId() != 0){%>
  <input class="btn btn-primary btn-delete" type="button" value="Slet hele licensen" onclick="javascript: confirmDelete('Delete license:<%=license.getLicenseName()%>');"/>
<%}%>