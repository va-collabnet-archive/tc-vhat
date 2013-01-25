package gov.va.med.term.vhat.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import java.util.UUID;

public class PT_ContentVersion extends BPT_ContentVersion
{
	public enum ContentVersion
	{
		RELEASE("release"),
		LOADER_VERSION("loaderVersion");

		private String niceName;
		private PropertyType pt;
		private ContentVersion(String niceName)
		{
			this.niceName = niceName;
		}
		
		public String getNiceName()
		{
			return this.niceName;
		}
		
		protected void setPropertyOwner(PropertyType pt)
		{
			this.pt = pt;
		}
		
		public UUID getUUID()
		{
			return pt.getPropertyUUID(this.niceName);
		}
	}

	public PT_ContentVersion(String uuidRoot)
	{
		super(uuidRoot);
		for (ContentVersion cv : ContentVersion.values())
		{
			addPropertyName(cv.getNiceName());
			cv.setPropertyOwner(this);
		}
	}
}
