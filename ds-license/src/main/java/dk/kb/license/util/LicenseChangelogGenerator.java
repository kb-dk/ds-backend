package dk.kb.license.util;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.storage.Attribute;
import dk.kb.license.storage.AttributeGroup;
import dk.kb.license.storage.AttributeValue;
import dk.kb.license.storage.License;
import dk.kb.license.storage.LicenseContent;

import dk.kb.license.storage.Presentation;

public class LicenseChangelogGenerator {

    private static String NEWLINE = "\n";
    private static final Logger log = LoggerFactory.getLogger(LicenseChangelogGenerator.class);
    
    public static void main(String[] args) {
    License l1 = createTestLicenseWithAssociations(1);
    License l2 = createTestLicenseWithAssociations(2);
    l2.setLicenseName("DK changed");
    l2.setDescription_dk("DK changed desc");
    l2.setValidTo("01-01-2222");

    AttributeGroup ag2 = l2.getAttributeGroups().get(0);
    Attribute a2 = ag2.getAttributes().get(0);
    a2.setAttributeName("test changed");

    LicenseContent lc2 = l2.getLicenseContents().get(0);
    Presentation presentation = lc2.getPresentations().remove(0);
    
    ChangeDifferenceText changes = getChanges(l1, l2);
     System.out.println("old:");
     System.out.println(changes.getBefore());
     System.out.println("after:");
     System.out.println(changes.getAfter());   
    }
    
    
    /**
     * Will generate a changeText. 
     * Will only add lines where the license has been changed. 
     * The lines are name, valid dates, description, attributegroup(multiline) and licensecontent(multiline)
     * 
     */
    public static ChangeDifferenceText getChanges(License licenseOld, License licenseNew) {
    
        StringBuilder oldText = new StringBuilder();
        StringBuilder newText = new StringBuilder();
        

        String nameOld = getNameText(licenseOld);
        String nameNew = getNameText(licenseNew);
        
        if(!nameOld.equals(nameNew)) {
            oldText.append(nameOld);
            newText.append(nameNew);
        }
        
        String dateOld = getValidDateText(licenseOld);
        String dateNew = getValidDateText(licenseNew);
        
        if(!dateOld.equals(dateNew)) {
            oldText.append(dateOld);
            newText.append(dateNew);
        }
        

        String descriptionOld = getDescriptionText(licenseOld);
        String descriptionNew = getDescriptionText(licenseNew);
        
        if(!descriptionOld.equals(descriptionNew)) {
            oldText.append(descriptionOld);
            newText.append(descriptionNew);
        }
        
        String attributeGroupsOld = getAttributeGroupsText(licenseOld.getAttributeGroups());
        String attributeGroupsNew = getAttributeGroupsText(licenseNew.getAttributeGroups());
        
        if(!attributeGroupsOld.equals(attributeGroupsNew)) {
            oldText.append(attributeGroupsOld);
            newText.append(attributeGroupsNew);
        }
        
        String licenseContentsOld = getLicenseContentsText(licenseOld.getLicenseContents());
        String licenseContentsNew = getLicenseContentsText(licenseNew.getLicenseContents());
        
        if(!licenseContentsOld.equals(licenseContentsNew)) {
            oldText.append(licenseContentsOld);
            newText.append(licenseContentsNew);
        }
        
        ChangeDifferenceText changes = new ChangeDifferenceText(oldText.toString(),newText.toString());
        return changes;
    }
    
    
    
    
    
    private static String getValidDateText(License license) {
        return "Valid:"+license.getValidFrom() +" to "+license.getValidTo()+NEWLINE;
    }
    
    private static String getNameText(License license) {
        return "License name DK/En: "+license.getLicenseName() +" / "+ license.getLicenseName_en()+NEWLINE;    
    }
    
    private static String getDescriptionText(License license) {
        return "License description DK/En:"+license.getDescription_dk() +" / "+ license.getDescription_en() +NEWLINE;
    }
    
    
    
    private static String getAttributeGroupsText(ArrayList<AttributeGroup> groups) {
       StringBuilder b = new StringBuilder();
       b.append("Attributes:"+NEWLINE);
       for (AttributeGroup g: groups) {
           b.append(getAttributeGroupText(g));           
       }
        
       return b.toString();
    }
    
    private static String getAttributeGroupText(AttributeGroup g) {
        StringBuilder b = new StringBuilder();
        
        for (Attribute a : g.getAttributes()) {
            //Generate comma seperated string of values
            String values= a.getValues().stream().map(Object::toString).collect(Collectors.joining(", "));            
            b.append("Attribute:"+a.getAttributeName() +"=["+values +"]");
            b.append(NEWLINE);
        }        
        
        return b.toString();
    }
    
    
    private static String  getLicenseContentsText(ArrayList<LicenseContent> lcs) {
        StringBuilder b = new StringBuilder();
        b.append("LicenseContents:"+NEWLINE);
        for (LicenseContent lc:lcs) {            
            b.append(getLicenseContentText(lc));           
        }         
        return b.toString();
     }
    
    
    private static String getLicenseContentText(LicenseContent lc) {
        StringBuilder b = new StringBuilder();                
            //Generate comma seperated string of values
            String presentations= lc.getPresentations().stream().map(Object::toString).collect(Collectors.joining(", "));            
            b.append("LicenseContent:"+lc.getName() +"=["+presentations +"]");
            b.append(NEWLINE);
                        
        return b.toString();
    }
    

    // This License is used for most unittests, so it is important to understand the object tree.
    private static License createTestLicenseWithAssociations(long id) {
        License license = new License();
        license.setId(id);
        license.setLicenseName("Dighumlab adgang");
        license.setDescription_dk("info of hvem licensen vedr. og hvad der er adgang til");
        license.setDescription_en("engelsk beskrivelse..");
        license.setValidFrom("27-12-2012");
        license.setValidTo("27-12-2023");

        ArrayList<AttributeGroup> groups = new ArrayList<AttributeGroup>();
        AttributeGroup group1 = new AttributeGroup(1);
        AttributeGroup group2 = new AttributeGroup(2);
        AttributeGroup group3 = new AttributeGroup(3);
        groups.add(group1);
        groups.add(group2);
        groups.add(group3);
        license.setAttributeGroups(groups);

        ArrayList<Attribute> group1_attributes = new ArrayList<Attribute>();
        group1.setAttributes(group1_attributes);
        Attribute group1_attribute1 = new Attribute();
        group1_attributes.add(group1_attribute1);
        group1_attribute1.setAttributeName("wayf.schacHomeOrganization");
        ArrayList<AttributeValue> group1_attribute1_values = new ArrayList<AttributeValue>();
        group1_attribute1.setValues(group1_attribute1_values);
        group1_attribute1_values.add(new AttributeValue("au.dk"));

        Attribute group1_attribute2 = new Attribute();
        group1_attributes.add(group1_attribute2);
        group1_attribute2.setAttributeName("wayf.eduPersonPrimaryAffiliation");
        ArrayList<AttributeValue> group1_attribute2_values = new ArrayList<AttributeValue>();
        group1_attribute2.setValues(group1_attribute2_values);
        group1_attribute2_values.add(new AttributeValue("student"));
        group1_attribute2_values.add(new AttributeValue("staff"));

        ArrayList<Attribute> group2_attributes = new ArrayList<Attribute>();
        group2.setAttributes(group2_attributes);
        Attribute group2_attribute1 = new Attribute();
        group2_attributes.add(group2_attribute1);
        group2_attribute1.setAttributeName("wayf.eduPersonPrimaryAffiliation");
        ArrayList<AttributeValue> group2_attribute1_values = new ArrayList<AttributeValue>();
        group2_attribute1.setValues(group2_attribute1_values);
        group2_attribute1_values.add(new AttributeValue("student"));

        Attribute group2_attribute2 = new Attribute();
        group2_attributes.add(group2_attribute2);
        group2_attribute2.setAttributeName("ip_role_mapper.SBIPRoleMapper");
        ArrayList<AttributeValue> group2_attribute2_values = new ArrayList<AttributeValue>();
        group2_attribute2.setValues(group2_attribute2_values);
        group2_attribute2_values.add(new AttributeValue("in_house"));

        ArrayList<Attribute> group3_attributes = new ArrayList<Attribute>();
        group3.setAttributes(group3_attributes);
        Attribute group3_attribute1 = new Attribute();
        group3_attributes.add(group3_attribute1);
        group3_attribute1.setAttributeName("attribut_store.MediestreamFullAccess");
        ArrayList<AttributeValue> group3_attribute1_values = new ArrayList<AttributeValue>();
        group3_attribute1.setValues(group3_attribute1_values);
        group3_attribute1_values.add(new AttributeValue("yes"));

        ArrayList<LicenseContent> licenseContents = new ArrayList<LicenseContent>();
        LicenseContent licenseContent1 = new LicenseContent();
        LicenseContent licenseContent2 = new LicenseContent();
        licenseContent1.setName("TV2");
        licenseContent2.setName("DR1");  
        licenseContents.add(licenseContent1);
        licenseContents.add(licenseContent2);

        ArrayList<Presentation> presentations1 = new ArrayList<Presentation>();
        ArrayList<Presentation> presentations2 = new ArrayList<Presentation>();
        presentations1.add(new Presentation("Stream"));
        presentations1.add(new Presentation("Download"));
        presentations2.add(new Presentation("Thumbnails"));
        licenseContent1.setPresentations(presentations1);
        licenseContent2.setPresentations(presentations2);

        license.setLicenseContents(licenseContents);

        return license;
    }

}
