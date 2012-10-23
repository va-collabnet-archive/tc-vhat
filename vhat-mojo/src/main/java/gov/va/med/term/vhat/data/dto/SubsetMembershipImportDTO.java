package gov.va.med.term.vhat.data.dto;

public class SubsetMembershipImportDTO extends EntityImportDTO
{
    protected long entityId;
    
    
    public SubsetMembershipImportDTO(String action, long vuid, boolean active)
    {
        super(action, null, vuid, active);
    }
    
    public long getEntityId()
    {
        return entityId;
    }

    public void setEntityId(long entityId)
    {
        this.entityId = entityId;
    }

    
}
