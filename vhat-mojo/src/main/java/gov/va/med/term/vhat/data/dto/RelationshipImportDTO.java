package gov.va.med.term.vhat.data.dto;

public class RelationshipImportDTO extends BaseImportDTO
{
    protected String typeName;
    protected String sourceCode;
    protected String newTargetCode;
    protected String oldTargetCode;
    
    
    public RelationshipImportDTO(String action, String typeName, String sourceCode, String oldTargetCode, String newTargetCode, boolean active)
    {
        super(action, active);
        this.typeName = typeName;
        this.newTargetCode = newTargetCode;
        this.oldTargetCode = oldTargetCode;
        this.sourceCode = sourceCode;
    }
    
    public String getTypeName()
    {
        return typeName;
    }
    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    public String getSourceCode()
    {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode)
    {
        this.sourceCode = sourceCode;
    }

    public String getNewTargetCode()
    {
        return newTargetCode;
    }

    public void setNewTargetCode(String newTargetCode)
    {
        this.newTargetCode = newTargetCode;
    }

    public String getOldTargetCode()
    {
        return oldTargetCode;
    }

    public void setOldTargetCode(String oldTargetCode)
    {
        this.oldTargetCode = oldTargetCode;
    }
}
