package gov.va.med.term.vhat.mojo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.etypes.EIdentifierString;
import org.ihtsdo.tk.binding.snomed.SnomedMetadataRf2;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.TkRevision;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.refex.TkRefexAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refex.type_member.TkRefexMember;
import org.ihtsdo.tk.dto.concept.component.refex.type_string.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.refex.type_uuid.TkRefexUuidMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

public class EConceptUtility
{
	private final UUID author_ = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
	private final UUID statusCurrentUuid_ = SnomedMetadataRf2.ACTIVE_VALUE_RF2.getUuids()[0];
	private final UUID statusRetiredUuid_ = SnomedMetadataRf2.INACTIVE_VALUE_RF2.getUuids()[0];
	private final UUID path_ = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
	private final UUID synonym_ = SnomedMetadataRf2.SYNONYM_RF2.getUuids()[0];
	private final UUID fullySpecifiedName_ = SnomedMetadataRf2.FULLY_SPECIFIED_NAME_RF2.getUuids()[0];
	private final UUID synonymAcceptable_ = SnomedMetadataRf2.ACCEPTABLE_RF2.getUuids()[0];
	private final UUID synonymPreferred_ = SnomedMetadataRf2.PREFERRED_RF2.getUuids()[0];
	private final UUID usEnRefset_ = SnomedMetadataRf2.US_ENGLISH_REFSET_RF2.getUuids()[0];
	private final UUID characteristic_ = SnomedMetadataRf2.STATED_RELATIONSHIP_RF2.getUuids()[0];
	private final UUID notRefinable = ArchitectonicAuxiliary.Concept.NOT_REFINABLE.getPrimoridalUid();
	private final UUID isARel = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();
	private final UUID module_ = TkRevision.unspecifiedModuleUuid;
	private final String lang_ = "en";
	
	private String uuidRoot_;
	
	private int annotationCounter_ = 0;
	
	public EConceptUtility(String uuidRoot) throws Exception
	{
		this.uuidRoot_ = uuidRoot;
	}
	
	public EConcept createConcept(UUID primordial, long time)
	{
		return createConcept(primordial, time, statusCurrentUuid_);
	}

	public EConcept createConcept(UUID primordial, long time, UUID status)
	{
		EConcept concept = new EConcept();
		concept.setPrimordialUuid(primordial);
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setPrimordialComponentUuid(primordial);
		conceptAttributes.setAuthorUuid(author_);
		conceptAttributes.setDefined(false);
		conceptAttributes.setStatusUuid(status);
		conceptAttributes.setPathUuid(path_);
		conceptAttributes.setModuleUuid(module_);
		conceptAttributes.setTime(time);
		concept.setConceptAttributes(conceptAttributes);
		return concept;
	}

	public TkDescription addSynonym(EConcept concept, UUID descriptionPrimoridal, String synonym, boolean preferred, long time)
	{
		TkDescription d = addDescription(concept, descriptionPrimoridal, synonym, synonym_, false, time);
		addAnnotation(d, UUID.nameUUIDFromBytes((uuidRoot_ + "annotation:" + annotationCounter_++).getBytes()),
				(preferred ? synonymPreferred_ : synonymAcceptable_), usEnRefset_, time);
		return d;
	}
	
	public TkDescription addFullySpecifiedName(EConcept concept, UUID descriptionPrimoridal, String fullySpecifiedName, long time)
	{
		TkDescription d = addDescription(concept, descriptionPrimoridal, fullySpecifiedName, fullySpecifiedName_, false, time);
		addAnnotation(d, UUID.nameUUIDFromBytes((uuidRoot_ + "annotation:" + annotationCounter_++).getBytes()),
				synonymPreferred_, usEnRefset_, time);
		return d;
	}
	
	public TkDescription addDescription(EConcept concept, UUID descriptionPrimordial, String descriptionValue, UUID descriptionType, boolean retired, long time)
	{
		List<TkDescription> descriptions = concept.getDescriptions();
		if (descriptions == null)
		{
			descriptions = new ArrayList<TkDescription>();
			concept.setDescriptions(descriptions);
		}
		TkDescription description = new TkDescription();
		description.setConceptUuid(concept.getPrimordialUuid());
		description.setLang(lang_);
		description.setPrimordialComponentUuid(descriptionPrimordial);
		description.setTypeUuid(descriptionType);
		description.setText(descriptionValue);
		description.setStatusUuid(retired ? statusRetiredUuid_ : statusCurrentUuid_);
		description.setAuthorUuid(author_);
		description.setPathUuid(path_);
		description.setModuleUuid(module_);
		description.setTime(time);

		descriptions.add(description);
		return description;
	}
	
	public EIdentifierString addAdditionalIds(EConcept concept, Object denotation, UUID authorityUUID, boolean retired, long time)
	{
		if (denotation != null)
		{
			List<TkIdentifier> additionalIds = concept.getConceptAttributes().getAdditionalIdComponents();
			if (additionalIds == null)
			{
				additionalIds = new ArrayList<TkIdentifier>();
				concept.getConceptAttributes().setAdditionalIdComponents(additionalIds);
			}

			// create the identifier and add it to the additional ids list
			EIdentifierString cid = new EIdentifierString();
			additionalIds.add(cid);

			// populate the identifier with the usual suspects
			cid.setAuthorityUuid(authorityUUID);
			cid.setAuthorUuid(author_);
			cid.setPathUuid(path_);
			cid.setModuleUuid(module_);
			cid.setStatusUuid(retired ? statusRetiredUuid_ : statusCurrentUuid_);
			cid.setTime(time);
			// populate the actual value of the identifier
			cid.setDenotation(denotation);
			return cid;
		}
		return null;
	}
	
	public TkRefsetStrMember addAnnotation(TkComponent<?> component, UUID refsetStrMemberPriomoridalUUID, String value, UUID refsetUUID, boolean retired, long time)
	{
		List<TkRefexAbstractMember<?>> annotations = component.getAnnotations();

		if (annotations == null)
		{
			annotations = new ArrayList<TkRefexAbstractMember<?>>();
			component.setAnnotations(annotations);
		}

		if (value != null)
		{
			TkRefsetStrMember strRefexMember = new TkRefsetStrMember();

			strRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
			strRefexMember.setString1(value);
			strRefexMember.setPrimordialComponentUuid(refsetStrMemberPriomoridalUUID);
			strRefexMember.setRefsetUuid(refsetUUID);
			strRefexMember.setStatusUuid(retired ? statusRetiredUuid_ : statusCurrentUuid_);
			strRefexMember.setAuthorUuid(author_);
			strRefexMember.setPathUuid(path_);
			strRefexMember.setModuleUuid(module_);
			strRefexMember.setTime(time);
			annotations.add(strRefexMember);
			return strRefexMember;
		}
		return null;
	}
	
	public TkRefexUuidMember addAnnotation(TkComponent<?> component, UUID refexUUIDMemberPrimordialUUID, UUID value, UUID refsetUUID, long time)
	{
		List<TkRefexAbstractMember<?>> annotations = component.getAnnotations();

		if (annotations == null)
		{
			annotations = new ArrayList<TkRefexAbstractMember<?>>();
			component.setAnnotations(annotations);
		}

		if (value != null)
		{
			TkRefexUuidMember conceptRefexMember = new TkRefexUuidMember();

			conceptRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
			conceptRefexMember.setUuid1(value);
			conceptRefexMember.setPrimordialComponentUuid(refexUUIDMemberPrimordialUUID);
			conceptRefexMember.setRefsetUuid(refsetUUID);
			conceptRefexMember.setStatusUuid(statusCurrentUuid_);
			conceptRefexMember.setAuthorUuid(author_);
			conceptRefexMember.setPathUuid(path_);
			conceptRefexMember.setModuleUuid(module_);
			conceptRefexMember.setTime(time);
			annotations.add(conceptRefexMember);
			return conceptRefexMember;
		}
		return null;
	}
	
	public TkRefexMember addRefsetMember(EConcept concept, UUID componentUUID, UUID refsetUUID, UUID refsetMemberPrimordialUUID, long time)
	{
		List<TkRefexAbstractMember<?>> refsetMembers = concept.getRefsetMembers();
		if (refsetMembers == null)
		{
			refsetMembers = new ArrayList<TkRefexAbstractMember<?>>();
			concept.setRefsetMembers(refsetMembers);
		}
		
		TkRefexMember conceptRefexMember = new TkRefexMember();
		conceptRefexMember.setComponentUuid(componentUUID);
		conceptRefexMember.setPrimordialComponentUuid(refsetMemberPrimordialUUID);
		conceptRefexMember.setRefsetUuid(refsetUUID);
		conceptRefexMember.setStatusUuid(statusCurrentUuid_);
		conceptRefexMember.setAuthorUuid(author_);
		conceptRefexMember.setPathUuid(path_);
		conceptRefexMember.setModuleUuid(module_);
		conceptRefexMember.setTime(time);
		refsetMembers.add(conceptRefexMember);
		
		return conceptRefexMember;
	}
		
	/**
	 * relationshipPrimoridal is optional - if not provided, the default value of IS_A_REL is used.
	 */
	public TkRelationship addRelationship(EConcept concept, UUID targetPrimordial, UUID relationshipPrimoridalType, UUID relPrimoridialComponentUuid, long time) 
	{
		List<TkRelationship> relationships = concept.getRelationships();
		if (relationships == null)
		{
			relationships = new ArrayList<TkRelationship>();
			concept.setRelationships(relationships);
		}
		 
		TkRelationship rel = new TkRelationship();
		rel.setPrimordialComponentUuid(relPrimoridialComponentUuid);
		rel.setC1Uuid(concept.getPrimordialUuid());
		rel.setTypeUuid(relationshipPrimoridalType == null ? isARel : relationshipPrimoridalType);
		rel.setC2Uuid(targetPrimordial);
		rel.setCharacteristicUuid(characteristic_);
		rel.setRefinabilityUuid(notRefinable);
		rel.setStatusUuid(statusCurrentUuid_);
		rel.setAuthorUuid(author_);
		rel.setPathUuid(path_);
		rel.setModuleUuid(module_);
		rel.setTime(time);
		rel.setRelGroup(0);  

		relationships.add(rel);
		return rel;
	}
	
	public UUID getRelUuid(String typeName)
	{
		return UUID.nameUUIDFromBytes((uuidRoot_ + "rel:" + typeName).getBytes());
    }
	
    public UUID getTypeUuid(String typeName) 
    {
        return UUID.nameUUIDFromBytes((uuidRoot_ + "type:" + typeName).getBytes());
    }

    public UUID getSubsetUuid(String vuid) 
    {
        return UUID.nameUUIDFromBytes((uuidRoot_ + "subset:" + vuid).getBytes());
    }

    public UUID getConceptUuid(String codeId) 
    {
        return UUID.nameUUIDFromBytes((uuidRoot_ + "code:" + codeId).getBytes());
    }
    
    public UUID getPropertyUuid(String codeId) 
    {
        return UUID.nameUUIDFromBytes((uuidRoot_ + "property:" + codeId).getBytes());
    }

    public UUID getDescriptionUuid(String descriptionId) 
    {
        return UUID.nameUUIDFromBytes((uuidRoot_ + "description:" + descriptionId).getBytes());
    }
}
