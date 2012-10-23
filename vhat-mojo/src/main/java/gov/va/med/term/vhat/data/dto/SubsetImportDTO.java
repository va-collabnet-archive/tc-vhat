package gov.va.med.term.vhat.data.dto;

public class SubsetImportDTO extends EntityImportDTO
{
    protected String subsetName;
    
    public SubsetImportDTO(String action, String subsetName, Long vuid, boolean active)
    {
        super(action, null, vuid, active);
        this.subsetName = subsetName;
    }
    
    public SubsetImportDTO(String action, String subsetName, boolean active)
    {
        super(action, null, null, active);
        this.subsetName = subsetName;
    }
    public String getSubsetName()
    {
        return subsetName;
    }
    public void setSubsetName(String subsetName)
    {
        this.subsetName = subsetName;
    }
}
