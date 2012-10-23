/*
 * Created on Oct 18, 2004
 */

package gov.va.med.term.vhat.data.dto;

public class CodeSystem
{
    private String name;
    private Long vuid;
    private String description;
    private String copyright;
    private String copyrightURL;
    private String preferredDesignationType;

    public CodeSystem()
    {

    }

    public CodeSystem(String name, Long vuid, String description, String copyright, String copyrightURL, String preferredDesignationType)
    {
        this.name = name;
        this.vuid = vuid;
        this.description = description;
        this.copyright = copyright;
        this.copyrightURL = copyrightURL;
        this.preferredDesignationType = preferredDesignationType;
    }


    /**
     * @return Returns the copyright.
     */
    public String getCopyright()
    {
        return copyright;
    }

    /**
     * @param copyright
     *            The copyright to set.
     */
    public void setCopyright(String copyright)
    {
        this.copyright = copyright;
    }

    /**
	 * @return the copyrightURL
	 */
	public String getCopyrightURL()
	{
		return copyrightURL;
	}

	/**
	 * @param copyrightURL the copyrightURL to set
	 */
	public void setCopyrightURL(String copyrightURL)
	{
		this.copyrightURL = copyrightURL;
	}

	/**
     * @return Returns the description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    public String toString()
    {
    	return this.getName();
    }

    public Long getVuid()
    {
        return vuid;
    }
    
    public void setVuid(Long vuid)
    {
        this.vuid = vuid;
    }
    
    /**
     * @return Returns the designationType
     */
    public String getPreferredDesignationType()
    {
        return preferredDesignationType;
    }

    public void setPreferredDesignationType(String preferredDesignationType)
    {
        this.preferredDesignationType = preferredDesignationType;
    }
}
