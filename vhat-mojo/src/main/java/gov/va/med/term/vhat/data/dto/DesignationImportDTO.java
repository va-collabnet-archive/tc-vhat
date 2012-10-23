package gov.va.med.term.vhat.data.dto;

import java.util.ArrayList;
import java.util.List;

public class DesignationImportDTO extends EntityImportDTO
{
    protected String typeName;
    protected String valueOld;
    protected String valueNew;
    protected List<SubsetMembershipImportDTO> subsets;
    protected List<PropertyImportDTO> properties;
    protected String moveFromConceptCode;
    
    
    public DesignationImportDTO(String action, String typeName, String code, String valueOld, String valueNew, Long vuid, boolean active)
    {
        super(action, code, vuid, active);
        this.typeName = typeName;
        this.valueOld = valueOld;
        this.valueNew = valueNew;
    }
    
    public String getCode()
    {
        return code;
    }
    public void setCode(String code)
    {
        this.code = code;
    }
    public String getTypeName()
    {
        return typeName;
    }
    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }
    public String getValueNew()
    {
        return valueNew;
    }
    public void setValueNew(String valueNew)
    {
        this.valueNew = valueNew;
    }
    public String getValueOld()
    {
        return valueOld;
    }
    public void setValueOld(String valueOld)
    {
        this.valueOld = valueOld;
    }

    public List<SubsetMembershipImportDTO> getSubsets()
    {
        return subsets;
    }

    public void setSubsets(List<SubsetMembershipImportDTO> subsets)
    {
        this.subsets = subsets;
    }

    public List<PropertyImportDTO> getProperties()
    {
        return (properties == null ? new ArrayList<PropertyImportDTO>() : properties);
    }

    public void setProperties(List<PropertyImportDTO> properties)
    {
        this.properties = properties;
    }
    
    public void addProperties(List<PropertyImportDTO> propertiesToAdd)
    {
        if (this.properties == null)
        {
            this.properties = new ArrayList<PropertyImportDTO>();
        }
        this.properties.addAll(propertiesToAdd);
    }

    public String getMoveFromConceptCode()
    {
        return moveFromConceptCode;
    }

    public void setMoveFromConceptCode(String moveFromConceptCode)
    {
        this.moveFromConceptCode = moveFromConceptCode;
    }

}
