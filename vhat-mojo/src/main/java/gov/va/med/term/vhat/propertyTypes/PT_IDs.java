package gov.va.med.term.vhat.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_IDs;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

public class PT_IDs extends BPT_IDs
{
	public enum ID
	{
		VUID("VUID");

		private Property property;

		private ID(String niceName)
		{
			// Don't know the owner yet - will be autofilled when we add this to the parent, below.
			property = new Property(null, niceName);
		}

		public Property getProperty()
		{
			return property;
		}
	}

	public PT_IDs()
	{
		super();
		for (ID id : ID.values())
		{
			addProperty(id.getProperty());
		}
	}
}