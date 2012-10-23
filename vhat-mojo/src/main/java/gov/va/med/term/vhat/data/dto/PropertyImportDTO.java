package gov.va.med.term.vhat.data.dto;

public class PropertyImportDTO extends BaseImportDTO
{
    protected String typeName;
    protected String valueOld;
    protected String valueNew;
    
    
    public PropertyImportDTO(String action, String typeName, String valueOld, String valueNew, boolean active)
    {
        super(action, active);
        this.typeName = typeName;
        this.valueOld = valueOld;
        this.valueNew = valueNew;
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
}
