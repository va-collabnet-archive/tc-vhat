package gov.va.med.term.vhat.mojo;

import gov.va.med.term.vhat.data.TerminologyDataReader;
import gov.va.med.term.vhat.data.dto.ConceptImportDTO;
import gov.va.med.term.vhat.data.dto.DesignationImportDTO;
import gov.va.med.term.vhat.data.dto.MapEntryImportDTO;
import gov.va.med.term.vhat.data.dto.MapSetImportDTO;
import gov.va.med.term.vhat.data.dto.PropertyImportDTO;
import gov.va.med.term.vhat.data.dto.RelationshipImportDTO;
import gov.va.med.term.vhat.data.dto.SubsetImportDTO;
import gov.va.med.term.vhat.data.dto.SubsetMembershipImportDTO;
import gov.va.med.term.vhat.data.dto.TypeImportDTO;
import gov.va.med.term.vhat.data.dto.Version;
import gov.va.med.term.vhat.propertyTypes.PT_Attributes;
import gov.va.med.term.vhat.propertyTypes.PT_Attributes.Attribute;
import gov.va.med.term.vhat.propertyTypes.PT_ContentVersion;
import gov.va.med.term.vhat.propertyTypes.PT_ContentVersion.ContentVersion;
import gov.va.med.term.vhat.propertyTypes.PT_IDs;
import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility.DescriptionType;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Descriptions;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Refsets;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Relations;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.ValuePropertyPair;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;

/**
 * Goal which converts VHAT data into the workbench jbin format
 * 
 * @goal convert-vhat-data
 * 
 * @phase process-sources
 */
public class VHATImportMojo extends AbstractMojo
{
	private final String vhatNamespaceSeed_ = "gov.va.med.term.vhat";
	private Map<String, List<RelationshipImportDTO>> relationshipMap = null;
	private TerminologyDataReader importer_;
	private EConceptUtility eConceptUtil_;

	private HashMap<String, String> referencedConcepts = new HashMap<String, String>();
	private HashMap<String, String> loadedConcepts = new HashMap<String, String>();
	private UUID rootConceptUUID;

	private PropertyType attributes_, descriptions_, relationships_, ids_;
	private PT_ContentVersion contentVersion_;
	private BPT_Refsets refsets_;

	private EConcept allVhatConceptsRefset;

	/**
	 * Where to put the output file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */

	private File outputDirectory;
	/**
	 * Location of source data file. May be a file or a directory.
	 * 
	 * @parameter
	 * @required
	 */
	private File inputFile;

	/**
	 * Loader version number
	 * Use parent because project.version pulls in the version of the data file, which I don't want.
	 * 
	 * @parameter expression="${project.parent.version}"
	 * @required
	 */
	private String loaderVersion;

	/**
	 * Content version number
	 * 
	 * @parameter expression="${project.version}"
	 * @required
	 */
	private String releaseVersion;

	private HashSet<Long> conceptsWithNoDesignations = new HashSet<Long>();
	private int mapEntryCount = 0;
	private int mapSetCount = 0;

	public void execute() throws MojoExecutionException
	{
		File f = outputDirectory;

		try
		{
			if (!f.exists())
			{
				f.mkdirs();
			}

			File touch = new File(f, "VHATEConcepts.jbin");
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(touch)));

			eConceptUtil_ = new EConceptUtility(vhatNamespaceSeed_, "VHAT Path", dos);
			
			ids_ = new PT_IDs();
			attributes_ = new PT_Attributes();
			descriptions_ = new BPT_Descriptions("VHAT");
			relationships_ = new BPT_Relations("VHAT");
			contentVersion_ = new PT_ContentVersion();
			refsets_ = new BPT_Refsets("VHAT");
			refsets_.addProperty("All VHAT Concepts");

			importer_ = new TerminologyDataReader(inputFile);
			List<ConceptImportDTO> items = importer_.process();

			relationshipMap = importer_.getRelationshipsMap();
			List<TypeImportDTO> dto = importer_.getTypes();

			EConcept vhatMetadata = createType(dos, ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid(), "VHAT Metadata");

			// read in the dynamic types
			for (TypeImportDTO typeImportDTO : dto)
			{
				if (typeImportDTO.getKind().equals("DesignationType"))
				{
					Property p = descriptions_.addProperty(typeImportDTO.getName());
					//Add some rankings for FSN / synonym handling
					if (p.getSourcePropertyNameFSN().equals("Fully Specified Name"))
					{
						p.setPropertySubType(BPT_Descriptions.FSN);
					}
					else if (p.getSourcePropertyNameFSN().equals("Preferred Name"))
					{
						p.setPropertySubType(BPT_Descriptions.SYNONYM);
					}
					else if (p.getSourcePropertyNameFSN().equals("Synonym"))
					{
						p.setPropertySubType(BPT_Descriptions.SYNONYM + 1);
					}
				}
				else if (typeImportDTO.getKind().equals("RelationshipType"))
				{
					Property p = relationships_.addProperty(typeImportDTO.getName());
					if (p.getSourcePropertyNameFSN().equals("has_parent"))
					{
						p.setWBPropertyType(EConceptUtility.isARelUuid_);
					}
				}
				else if (typeImportDTO.getKind().equals("PropertyType"))
				{
					// Move Search_Term up to the concept, add it as a description
					if (typeImportDTO.getName().equals("Search_Term"))
					{
						descriptions_.addProperty(typeImportDTO.getName());
					}
					else
					{
						attributes_.addProperty(typeImportDTO.getName());
					}
				}
			}
			
			//get the refset names
			for (SubsetImportDTO subset : importer_.getSubsets())
			{
				refsets_.addProperty(subset.getSubsetName());
			}

			eConceptUtil_.loadMetaDataItems(Arrays.asList(ids_, contentVersion_, descriptions_, attributes_, relationships_, refsets_), vhatMetadata.getPrimordialUuid(), dos);
			loadedConcepts.put(refsets_.getRefsetIdentityParent().getPrimordialUuid().toString(), eConceptUtil_.PROJECT_REFSETS_NAME);

			ConsoleUtil.println("Metadata load stats");
			for (String line : eConceptUtil_.getLoadStats().getSummary())
			{
				ConsoleUtil.println(line);
			}
			
			eConceptUtil_.clearLoadStats();

			allVhatConceptsRefset = refsets_.getConcept("All VHAT Concepts");
			loadedConcepts.put(allVhatConceptsRefset.getPrimordialUuid().toString(), "All VHAT Concepts");

			Map<Long, Set<Long>> subsetMembershipMap = new HashMap<Long, Set<Long>>();
			// get the subset memberships to build the refset for each subset
			for (ConceptImportDTO item : items)
			{
				List<DesignationImportDTO> designations = item.getDesignations();
				for (DesignationImportDTO designation : designations)
				{
					List<SubsetMembershipImportDTO> subsets = designation.getSubsets();
					for (SubsetMembershipImportDTO subsetMembership : subsets)
					{
						Set<Long> vuids = subsetMembershipMap.get(subsetMembership.getVuid());
						if (vuids == null)
						{
							vuids = new HashSet<Long>();
							subsetMembershipMap.put(subsetMembership.getVuid(), vuids);
						}
						vuids.add(designation.getVuid());
					}
				}
			}
			
			// create all the subsets - option 1 - this could be swapped out with option 2, below (which is currently commented out)
			List<SubsetImportDTO> subsets = importer_.getSubsets();
			for (SubsetImportDTO subset : subsets)
			{
				loadRefset(subset.getSubsetName(), subsetMembershipMap.get(subset.getVuid()));
			}

			for (ConceptImportDTO item : items)
			{
				writeEConcept(dos, item);
			}
			
			eConceptUtil_.storeRefsetConcepts(refsets_, dos);

			ArrayList<String> missingConcepts = new ArrayList<String>();

			for (String refUUID : referencedConcepts.keySet())
			{
				if (loadedConcepts.get(refUUID) == null)
				{
					missingConcepts.add(refUUID);
					ConsoleUtil.printErrorln("Data error - The concept " + refUUID + " - " + referencedConcepts.get(refUUID)
							+ " was referenced, but not loaded - will be created as '-MISSING-'");
				}
			}

			if (missingConcepts.size() > 0)
			{
				EConcept missingParent = eConceptUtil_.createConcept("Missing Concepts");
				eConceptUtil_.addRelationship(missingParent, rootConceptUUID);
				missingParent.writeExternal(dos);
				for (String refUUID : missingConcepts)
				{
					EConcept c = eConceptUtil_.createConcept(UUID.fromString(refUUID), "-MISSING-");
					eConceptUtil_.addRelationship(c, missingParent.getPrimordialUuid());
					c.writeExternal(dos);
				}
			}

			if (mapEntryCount > 0)
			{
				ConsoleUtil.println("Skipped " + mapEntryCount + " MapEntry objects");
			}
			if (mapSetCount > 0)
			{
				ConsoleUtil.println("Skipped " + mapSetCount + " MapSet objects");
			}

			dos.flush();
			dos.close();

			// Put in names instead of IDs so the load stats print nicer:
			Hashtable<String, String> stringsToSwap = new Hashtable<String, String>();
			for (SubsetImportDTO subset : subsets)
			{
				stringsToSwap.put(subset.getVuid() + "", subset.getSubsetName());
			}
			

			ConsoleUtil.println("Load Statistics");
			// swap out vuids with names to make it more readable...
			for (String line : eConceptUtil_.getLoadStats().getSummary())
			{
				Enumeration<String> e = stringsToSwap.keys();
				while (e.hasMoreElements())
				{
					String current = e.nextElement();
					line = line.replaceAll(current, stringsToSwap.get(current));
				}
				ConsoleUtil.println(line);
			}

			// this could be removed from final release. Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(new File(outputDirectory, "vhatUuidDebugMap.txt"));

			if (conceptsWithNoDesignations.size() > 0)
			{
				ConsoleUtil.printErrorln(conceptsWithNoDesignations.size() + " concepts were found with no descriptions at all.  These were assigned '-MISSING-'");
				FileWriter fw = new FileWriter(new File(outputDirectory, "NoDesignations.txt"));
				for (Long l : conceptsWithNoDesignations)
				{
					fw.write(l.toString());
					fw.write(System.getProperty("line.separator"));
				}
				fw.close();
			}
			ConsoleUtil.writeOutputToFile(new File(outputDirectory, "ConsoleOutput.txt").toPath());
		}
		catch (Exception ex)
		{
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}

	}

	private void writeEConcept(DataOutputStream dos, ConceptImportDTO conceptDto) throws Exception
	{
		if (conceptDto instanceof MapEntryImportDTO)
		{
			mapEntryCount++;
		}
		else if (conceptDto instanceof MapSetImportDTO)
		{
			mapSetCount++;
		}
		else
		{
			long time = eConceptUtil_.defaultTime_;

			EConcept concept = eConceptUtil_.createConcept(getConceptUuid(conceptDto.getVuid().toString()), time, eConceptUtil_.statusCurrentUuid_);
			loadedConcepts.put(concept.getPrimordialUuid().toString(), conceptDto.getVuid().toString());
			eConceptUtil_.addAdditionalIds(concept, conceptDto.getVuid().toString(), ids_.getProperty("VUID").getUUID(), false);

			for (PropertyImportDTO property : conceptDto.getProperties())
			{
				eConceptUtil_.addStringAnnotation(concept, property.getValueNew(), attributes_.getProperty(property.getTypeName()).getUUID(), false);
			}

			List<DesignationImportDTO> designationDto = conceptDto.getDesignations();
			ArrayList<ValuePropertyPairExtended> descriptionHolder = new ArrayList<>(designationDto.size());
			for (DesignationImportDTO didto : designationDto)
			{
				descriptionHolder.add(new ValuePropertyPairExtended(didto.getValueNew(), getDescriptionUuid(didto.getVuid().toString()),
						descriptions_.getProperty(didto.getTypeName()), didto, !didto.isActive()));
			}
			
			List<TkDescription> wbDescriptions = eConceptUtil_.addDescriptions(concept, descriptionHolder);
			
			//Descriptions have now all been added to the concepts - now we need to process the rest of the ugly bits of vhat
			//and place them on the descriptions.
			for (int i = 0; i < descriptionHolder.size(); i++)
			{
				ValuePropertyPairExtended vpp = descriptionHolder.get(i);
				TkDescription desc = wbDescriptions.get(i);
				
				if (vpp.getValue().equals("VHAT"))
				{
					// On the root node, we need to add some extra attributes
					eConceptUtil_.addDescription(concept, "VHAT", DescriptionType.SYNONYM, true, null, null, false);
					eConceptUtil_.addDescription(concept, "VHA Terminology", DescriptionType.SYNONYM, false, null, null, false);
					ConsoleUtil.println("Root concept FSN is 'VHAT' and the UUID is " + concept.getPrimordialUuid());
					Version version = importer_.getVersion();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					eConceptUtil_.addStringAnnotation(concept, sdf.format(version.getReleaseDate()), ContentVersion.RELEASE_DATE.getProperty().getUUID(), false);
					eConceptUtil_.addStringAnnotation(concept, releaseVersion, contentVersion_.RELEASE.getUUID(), false);
					eConceptUtil_.addStringAnnotation(concept, loaderVersion, contentVersion_.LOADER_VERSION.getUUID(), false);
					rootConceptUUID = concept.getPrimordialUuid();
				}
				eConceptUtil_.addAdditionalIds(desc, vpp.getDesignationImportDTO().getVuid(), ids_.getProperty("VUID").getUUID());

				// VHAT is kind of odd, in that the attributes are attached to the description, rather than the concept.
				for (PropertyImportDTO property : vpp.getDesignationImportDTO().getProperties())
				{
					// Move these up, retype as a description
					if (property.getTypeName().equals("Search_Term"))
					{
						//Don't need to worry about running these through the auto-sorter description add method - these will never need to be promoted to FSN
						//since they only exist under other descriptions.
						TkDescription searchTermDesc = eConceptUtil_.addDescription(concept, property.getValueNew(), DescriptionType.SYNONYM, false,
								descriptions_.getProperty(property.getTypeName()).getUUID(), 
								descriptions_.getProperty(property.getTypeName()).getPropertyType().getPropertyTypeReferenceSetUUID(), !property.isActive());
						// Annotate which description it came from
						eConceptUtil_.addStringAnnotation(searchTermDesc, vpp.getDesignationImportDTO().getVuid() + "", 
								Attribute.SOURCE_DESCRIPTION_VUID.getProperty().getUUID(), false);
					}
					else
					{
						eConceptUtil_.addStringAnnotation(desc, property.getValueNew(), attributes_.getProperty(property.getTypeName()).getUUID(), false);
					}
				}

				//This is an alternate way to add the subsets, (option 2) but it doesn't seem to lead to subsets as nice in the WB.
				//So, we are using method 1, up above, instead.
				//// Same here, with the refset membership being attached to the description, rather than the concept.
				//if (vpp.getDesignationImportDTO().getSubsets() != null)
				//{
				//	for (SubsetMembershipImportDTO subset : vpp.getDesignationImportDTO().getSubsets())
				//	{
				//		eConceptUtil_.addUuidAnnotation(desc, null, getSubsetUuid(subset.getVuid() + ""));
				//	}
				//}
			}
			
			if (descriptionHolder.size() == 0)
			{
				// Seems like a data error - but it is happening... no descriptions at all.....
				conceptsWithNoDesignations.add(conceptDto.getVuid());
				// The workbench implodes if you don't have a fully specified name....
				eConceptUtil_.addDescription(concept, "-MISSING-", DescriptionType.FSN, true, null, null, false);
			}

			List<RelationshipImportDTO> relationshipImports = relationshipMap.get(conceptDto.getCode());
			if (relationshipImports != null)
			{
				for (RelationshipImportDTO relationshipImportDTO : relationshipImports)
				{
					UUID sourceUuid = getConceptUuid(relationshipImportDTO.getSourceCode());
					UUID targetUuid = getConceptUuid(relationshipImportDTO.getNewTargetCode());

					referencedConcepts.put(targetUuid.toString(), relationshipImportDTO.getNewTargetCode());

					if (!sourceUuid.equals(concept.getPrimordialUuid()))
					{
						throw new MojoExecutionException("Design failure!");
					}

					eConceptUtil_.addRelationship(concept, targetUuid, relationships_.getProperty(relationshipImportDTO.getTypeName()), time);
				}
			}

			eConceptUtil_.addRefsetMember(allVhatConceptsRefset, concept.getPrimordialUuid(), null, true, time);
			concept.writeExternal(dos);
		}
	}

	private EConcept createType(DataOutputStream dos, UUID parentUuid, String typeName) throws Exception
	{
		EConcept concept = eConceptUtil_.createConcept(typeName);
		loadedConcepts.put(concept.getPrimordialUuid().toString(), typeName);
		eConceptUtil_.addRelationship(concept, parentUuid);
		concept.writeExternal(dos);
		return concept;
	}

	private void loadRefset(String typeName, Set<Long> refsetMembership) throws Exception
	{
		EConcept concept = refsets_.getConcept(typeName);
		loadedConcepts.put(concept.getPrimordialUuid().toString(), typeName);

		if (refsetMembership != null)
		{
			for (Long memberVuid : refsetMembership)
			{
				eConceptUtil_.addRefsetMember(concept, getDescriptionUuid(memberVuid.toString()), null, true, null);
			}
		}
	}

	private UUID getConceptUuid(String codeId)
	{
		return ConverterUUID.createNamespaceUUIDFromString("code:" + codeId, true);
	}

	private UUID getDescriptionUuid(String descriptionId)
	{
		return ConverterUUID.createNamespaceUUIDFromString("description:" + descriptionId, true);
	}

	public static void main(String[] args) throws MojoExecutionException
	{
		VHATImportMojo i = new VHATImportMojo();
		i.outputDirectory = new File("../vhat-econcept/target");
		i.inputFile = new File("../vhat-econcept/target/generated-resources/xml/");
		i.execute();
	}
	
	private class ValuePropertyPairExtended extends ValuePropertyPair
	{
		private DesignationImportDTO didto_;
		public ValuePropertyPairExtended(String value, UUID descriptionUUID, Property property, DesignationImportDTO didto, boolean disabled)
		{
			super(value, descriptionUUID, property);
			didto_ = didto;
			setDisabled(disabled);
		}
		
		public DesignationImportDTO getDesignationImportDTO()
		{
			return didto_;
		}
	}
}