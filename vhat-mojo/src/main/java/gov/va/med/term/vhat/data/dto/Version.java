package gov.va.med.term.vhat.data.dto;

import java.util.Date;

public class Version  
{
    protected String name;
    protected CodeSystem codeSystem;
    protected Date effectiveDate;
    protected Date releaseDate;
    protected Date deploymentDate;
    protected Date importDate;
    protected String description;
    protected String source;
    protected Integer conceptCount;
    protected boolean append;

    public Version()
    {
    	
    }
    
    public Version(String name, Date effectiveDate, String description)
    {
        this.name = name;
    	this.effectiveDate = effectiveDate;
    	this.description = description;
    }
    
    
    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the codeSystem
     */
    public CodeSystem getCodeSystem()
    {
        return codeSystem;
    }

    /**
     * @param codeSystem the codeSystem to set
     */
    public void setCodeSystem(CodeSystem codeSystem)
    {
        this.codeSystem = codeSystem;
    }

    /**
     * @return
     */
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return
     */
    public Date getEffectiveDate()
    {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate)
    {
        this.effectiveDate = effectiveDate;
    }
    
    /**
     * @return the deploymentDate
     */
    public Date getDeploymentDate()
    {
        return deploymentDate;
    }

    /**
     * @param deploymentDate the deploymentDate to set
     */
    public void setDeploymentDate(Date deploymentDate)
    {
        this.deploymentDate = deploymentDate;
    }

    /**
     * @return the releaseDate
     */
    public Date getReleaseDate()
    {
        return releaseDate;
    }

    /**
     * @param releaseDate the releaseDate to set
     */
    public void setReleaseDate(Date releaseDate)
    {
        this.releaseDate = releaseDate;
    }
    
	/**
	 * @return the importDate
	 */
    public Date getImportDate()
	{
		return this.importDate;
	}

	/**
	 * @param importDate the importDate to set
	 */
	public void setImportDate(Date importDate)
	{
		this.importDate = importDate;
	}
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    /**
	 * @return the source
	 */
	public String getSource()
    {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source)
	{
		this.source = source;
	}

	/**
	 * @return the conceptCount
	 */
	public Integer getConceptCount()
	{
		return conceptCount;
	}

	/**
	 * @param conceptCount the conceptCount to set
	 */
	public void setConceptCount(Integer conceptCount)
	{
		this.conceptCount = conceptCount;
	}

	public boolean getAppend()
	{
		return append;
	}
	
	public void setAppend(boolean append)
	{
		this.append = append;
	}

}
