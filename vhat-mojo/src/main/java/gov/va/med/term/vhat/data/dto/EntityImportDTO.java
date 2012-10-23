package gov.va.med.term.vhat.data.dto;

public class EntityImportDTO extends BaseImportDTO
{
    protected Long vuid;
    protected String code;
    
    public EntityImportDTO()
    {
        super();
    }

    public EntityImportDTO(String action, String code, Long vuid, boolean active)
    {
        super(action, active);
        this.vuid = vuid;
        this.code = code;
    }
    
    public Long getVuid()
    {
        return vuid;
    }

    public void setVuid(Long vuid)
    {
        this.vuid = vuid;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

}