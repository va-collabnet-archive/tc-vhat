package gov.va.med.term.vhat.data.dto;

public class BaseImportDTO
{

    protected String action;
    protected boolean active;

    public BaseImportDTO()
    {
        super();
    }

    public BaseImportDTO(String action, boolean active)
    {
        super();
        this.action = action;
        this.active = active;
    }


    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

}