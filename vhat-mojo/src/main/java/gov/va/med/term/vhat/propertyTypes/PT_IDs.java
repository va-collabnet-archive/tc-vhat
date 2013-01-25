package gov.va.med.term.vhat.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_IDs;
import java.util.UUID;

public class PT_IDs extends BPT_IDs
{
	public enum IDs
	{
		VUID("VUID"); 

		private String niceName;
		private PT_IDs pt_ids;
		private IDs(String niceName)
		{
			this.niceName = niceName;
		}
		
		public String getNiceName()
		{
			return this.niceName;
		}
		
		protected void setPropertyOwner(PT_IDs pt_ids)
		{
			this.pt_ids = pt_ids;
		}
		
		public UUID getUUID()
		{
			return pt_ids.getPropertyUUID(this.niceName);
		}
	}

	public PT_IDs(String uuidRoot)
	{
		super(uuidRoot);
		for (IDs ids : IDs.values())
		{
			addPropertyName(ids.getNiceName());
			ids.setPropertyOwner(this);
		}
	}
}
