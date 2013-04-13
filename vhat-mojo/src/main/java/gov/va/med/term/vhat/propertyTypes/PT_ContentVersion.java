package gov.va.med.term.vhat.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

public class PT_ContentVersion extends BPT_ContentVersion
{
	public enum ContentVersion
	{
		RELEASE_DATE("Release Date");

		private Property property;

		private ContentVersion(String niceName)
		{
			// Don't know the owner yet - will be autofilled when we add this to the parent, below.
			property = new Property(null, niceName);
		}

		public Property getProperty()
		{
			return property;
		}
	}

	public PT_ContentVersion()
	{
		super();
		for (ContentVersion cv : ContentVersion.values())
		{
			addProperty(cv.getProperty());
		}
	}
}
