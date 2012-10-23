package gov.va.med.term.vhat.data.dto;


public class MapEntryImportDTO extends ConceptImportDTO
{
    private String sourceConceptCode;
    private String targetConceptCode;
    private int sequence;
    private Long grouping;
    private Long muid;
	
	public MapEntryImportDTO(String action, String name, String code, Long vuid,
			boolean active, String sourceConceptCode, String targetConceptCode, 
			int sequence, Long grouping, Long muid)
	{
		super(action, name, code, vuid, active);
		this.sourceConceptCode = sourceConceptCode;
		this.targetConceptCode = targetConceptCode;
		this.sequence = sequence;
		this.grouping = grouping;
		this.muid = muid;
	}

	public String getSourceConceptCode()
	{
		return sourceConceptCode;
	}

	public void setSourceConceptCode(String sourceConceptCode)
	{
		this.sourceConceptCode = sourceConceptCode;
	}

	public String getTargetConceptCode()
	{
		return targetConceptCode;
	}

	public void setTargetConceptCode(String targetConceptCode)
	{
		this.targetConceptCode = targetConceptCode;
	}

	public int getSequence()
	{
		return sequence;
	}

	public void setSequence(int sequence)
	{
		this.sequence = sequence;
	}

	public Long getMuid()
	{
		return muid;
	}

	public void setMuid(Long muid)
	{
		this.muid = muid;
	}

	public Long getGrouping()
	{
		return grouping;
	}

	public void setGrouping(Long grouping)
	{
		this.grouping = grouping;
	}
}
