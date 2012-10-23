package gov.va.med.term.vhat.data.dto;

public class TypeImportDTO
{
    protected String kind;
    protected String name;

    
    public TypeImportDTO(String kind, String name)
    {
        super();
        this.kind = kind;
        this.name = name;
    }
    public String getKind()
    {
        return kind;
    }
    public void setKind(String kind)
    {
        this.kind = kind;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
}
