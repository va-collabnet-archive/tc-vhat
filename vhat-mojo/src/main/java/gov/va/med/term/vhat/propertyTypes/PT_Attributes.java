package gov.va.med.term.vhat.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Attributes;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

public class PT_Attributes extends BPT_Attributes
{
	public enum Attribute
	{
		SOURCE_DESCRIPTION_VUID("Source Description VUID");

		private Property property;

		private Attribute(String niceName)
		{
			// Don't know the owner yet - will be autofilled when we add this to the parent, below.
			property = new Property(null, niceName);
		}

		public Property getProperty()
		{
			return property;
		}
	}

	public PT_Attributes(String uuidRoot)
	{
		super(uuidRoot);
		for (Attribute attr : Attribute.values())
		{
			addProperty(attr.getProperty());
		}
	}
}
