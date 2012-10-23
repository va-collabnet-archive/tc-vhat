package gov.va.med.term.vhat.data.dto;


import java.util.ArrayList;
import java.util.List;

public class ConceptImportDTO extends EntityImportDTO
{
    protected List<DesignationImportDTO> designations;
    protected List<PropertyImportDTO> properties;
    protected String name;
    
    public ConceptImportDTO(String action, String name, String code, Long vuid, boolean active)
    {
        super(action, code, vuid, active);
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    public List<DesignationImportDTO> getDesignations()
    {
        return designations;
    }
    public void setDesignations(List<DesignationImportDTO> designations)
    {
        this.designations = designations;
    }
    public List<PropertyImportDTO> getProperties()
    {
        return (properties == null ? new ArrayList<PropertyImportDTO>() : properties);
    }
    public void setProperties(List<PropertyImportDTO> properties)
    {
        this.properties = properties;
    }
}
