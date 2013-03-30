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
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion.BaseContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Descriptions;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Relations;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
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
	private Map<String, List<RelationshipImportDTO>> relationshipMap = null;
	private TerminologyDataReader importer_;
	private EConceptUtility eConceptUtil_;

	private HashMap<String, String> referencedConcepts = new HashMap<String, String>();
	private HashMap<String, String> loadedConcepts = new HashMap<String, String>();
	private UUID rootConceptUUID;

	private PropertyType attributes_, descriptions_, relationships_, ids_, contentVersion_;

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
	private String uuidRoot_ = "gov.va.med.term.vhat:";

	public void execute() throws MojoExecutionException
	{
		//The way this converter is structured, can't leave this feature enabled
		ConverterUUID.enableDupeUUIDException = false;
		ids_ = new PT_IDs(uuidRoot_);
		attributes_ = new PT_Attributes(uuidRoot_);
		descriptions_ = new BPT_Descriptions(uuidRoot_);
		relationships_ = new BPT_Relations(uuidRoot_);
		contentVersion_ = new PT_ContentVersion(uuidRoot_);

		File f = outputDirectory;

		try
		{
			if (!f.exists())
			{
				f.mkdirs();
			}

			File touch = new File(f, "VHATEConcepts.jbin");
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(touch)));

			eConceptUtil_ = new EConceptUtility(uuidRoot_, "VHAT Path", dos);

			importer_ = new TerminologyDataReader(inputFile);
			List<ConceptImportDTO> items = importer_.process();

			relationshipMap = importer_.getRelationshipsMap();
			List<TypeImportDTO> dto = importer_.getTypes();

			EConcept vhatMetadata = createType(dos, ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid(), "VHAT Metadata");

			EConcept vaRefsets = eConceptUtil_.createVARefsetRootConcept();
			loadedConcepts.put(vaRefsets.getPrimordialUuid().toString(), eConceptUtil_.VA_REFSET_NAME);
			vaRefsets.writeExternal(dos);

			// read in the dynamic types
			for (TypeImportDTO typeImportDTO : dto)
			{
				if (typeImportDTO.getKind().equals("DesignationType"))
				{
					descriptions_.addProperty(typeImportDTO.getName());
				}
				else if (typeImportDTO.getKind().equals("RelationshipType"))
				{
					relationships_.addProperty(typeImportDTO.getName());
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

			eConceptUtil_.loadMetaDataItems(Arrays.asList(ids_, contentVersion_, descriptions_, attributes_, relationships_), vhatMetadata.getPrimordialUuid(), dos);

			EConcept subsetRefset = createType(dos, vaRefsets.primordialUuid, "VHAT Refsets");

			// store this one later
			allVhatConceptsRefset = eConceptUtil_.createConcept("All VHAT Concepts", subsetRefset.primordialUuid);
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

			// create all the subsets
			List<SubsetImportDTO> subsets = importer_.getSubsets();
			for (SubsetImportDTO subset : subsets)
			{
				// TODO this duplicates the subset info - it is also added to the concepts. Do we want both? Are they exact duplicates?
				createType(dos, 
						subsetRefset.getPrimordialUuid(), 
						subset.getSubsetName(), 
						getSubsetUuid(subset.getVuid() + ""), 
						subsetMembershipMap.get(subset.getVuid()));
			}

			for (ConceptImportDTO item : items)
			{
				writeEConcept(dos, item);
			}

			allVhatConceptsRefset.writeExternal(dos);

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
				EConcept missingParent = eConceptUtil_.createConcept("Missing Concepts", "Missing Concepts");
				eConceptUtil_.addRelationship(missingParent, rootConceptUUID, null, null);
				missingParent.writeExternal(dos);
				for (String refUUID : missingConcepts)
				{
					EConcept c = eConceptUtil_.createConcept(UUID.fromString(refUUID), "-MISSING-");
					eConceptUtil_.addRelationship(c, missingParent.getPrimordialUuid(), null, null);
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

	public void writeEConcept(DataOutputStream dos, ConceptImportDTO conceptDto) throws Exception
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
			long time = System.currentTimeMillis();

			EConcept concept = eConceptUtil_.createConcept(getConceptUuid(conceptDto.getVuid().toString()), time, eConceptUtil_.statusCurrentUuid_);
			loadedConcepts.put(concept.getPrimordialUuid().toString(), conceptDto.getVuid().toString());
			eConceptUtil_.addAdditionalIds(concept, conceptDto.getVuid().toString(), ids_.getProperty("VUID").getUUID(), false);

			for (PropertyImportDTO property : conceptDto.getProperties())
			{
				eConceptUtil_.addStringAnnotation(concept, property.getValueNew(), attributes_.getProperty(property.getTypeName()).getUUID(), false);
			}

			List<DesignationImportDTO> designationDto = conceptDto.getDesignations();
			boolean fullySpecifiedNameAdded = false;
			DesignationImportDTO bestDescription = null;
			for (DesignationImportDTO designationImportDTO : designationDto)
			{
				if (designationImportDTO.getValueNew() == null)
				{
					throw new MojoExecutionException("Description is null for concept: " + conceptDto.getName());
				}

				if (bestDescription == null)
				{
					// Make sure we have something....
					bestDescription = designationImportDTO;
				}

				if (designationImportDTO.getValueNew().equals("VHAT"))
				{
					// On the root node, we need to add some extra attributes
					Version version = importer_.getVersion();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					eConceptUtil_.addStringAnnotation(concept, sdf.format(version.getReleaseDate()), ContentVersion.RELEASE_DATE.getProperty().getUUID(), false);
					eConceptUtil_.addStringAnnotation(concept, releaseVersion, BaseContentVersion.RELEASE.getProperty().getUUID(), false);
					eConceptUtil_.addStringAnnotation(concept, loaderVersion, BaseContentVersion.LOADER_VERSION.getProperty().getUUID(), false);
					rootConceptUUID = concept.getPrimordialUuid();
				}

				if (designationImportDTO.getTypeName().equals("Fully Specified Name"))
				{
					fullySpecifiedNameAdded = true;
					// Let it generate a UUID for this one - don't want to collide with the second copy below
					TkDescription fsn = eConceptUtil_.addFullySpecifiedName(concept, designationImportDTO.getValueNew(), null, !designationImportDTO.isActive());
					eConceptUtil_.addAdditionalIds(fsn, designationImportDTO.getVuid(), ids_.getProperty("VUID").getUUID());
				}
				else if (designationImportDTO.getTypeName().equals("Preferred Name"))
				{
					// This one is better
					bestDescription = designationImportDTO;
				}

				// I like to maintain the terminologies actual type naming for the descriptions as well
				// This will duplicate the FSN added above in those cases, but thats ok.
				TkDescription addedDescription = eConceptUtil_.addDescription(concept, getDescriptionUuid(designationImportDTO.getVuid().toString()),
						designationImportDTO.getValueNew(), descriptions_.getProperty(designationImportDTO.getTypeName()).getUUID(), !designationImportDTO.isActive());
				eConceptUtil_.addAdditionalIds(addedDescription, designationImportDTO.getVuid(), ids_.getProperty("VUID").getUUID());

				// VHAT is kind of odd, in that the attributes are attached to the description, rather than the concept.
				for (PropertyImportDTO property : designationImportDTO.getProperties())
				{
					// Move these up, retype as a description
					if (property.getTypeName().equals("Search_Term"))
					{
						TkDescription searchTermDesc = eConceptUtil_.addDescription(concept, property.getValueNew(), descriptions_.getProperty(property.getTypeName())
								.getUUID(), !property.isActive());
						// Annotate which description it came from
						eConceptUtil_.addStringAnnotation(searchTermDesc, designationImportDTO.getVuid() + "", Attribute.SOURCE_DESCRIPTION_VUID.getProperty().getUUID(),
								false);
					}
					else
					{
						eConceptUtil_.addStringAnnotation(addedDescription, property.getValueNew(), attributes_.getProperty(property.getTypeName()).getUUID(), false);
					}
				}

				// Same here, with the refset membership being attached to the description, rather than the concept.
				if (designationImportDTO.getSubsets() != null)
				{
					for (SubsetMembershipImportDTO subset : designationImportDTO.getSubsets())
					{
						eConceptUtil_.addUuidAnnotation(addedDescription, null, getSubsetUuid(subset.getVuid() + ""));
					}
				}
			}

			if (!fullySpecifiedNameAdded)
			{
				if (bestDescription != null)
				{
					// The workbench implodes if you don't have a fully specified name....
					eConceptUtil_.addFullySpecifiedName(concept, bestDescription.getValueNew(), null, !bestDescription.isActive());
				}
				else
				{
					// Seems like a data error - but it is happening... no descriptions at all.....
					conceptsWithNoDesignations.add(conceptDto.getVuid());
					// The workbench implodes if you don't have a fully specified name....
					eConceptUtil_.addFullySpecifiedName(concept, "-MISSING-", null);
				}
			}

			List<RelationshipImportDTO> relationshipImports = relationshipMap.get(conceptDto.getCode());
			if (relationshipImports != null)
			{
				for (RelationshipImportDTO relationshipImportDTO : relationshipImports)
				{
					UUID sourceUuid = getConceptUuid(relationshipImportDTO.getSourceCode());
					UUID targetUuid = getConceptUuid(relationshipImportDTO.getNewTargetCode());
					UUID typeUuid = relationships_.getProperty(relationshipImportDTO.getTypeName()).getUUID();

					referencedConcepts.put(targetUuid.toString(), relationshipImportDTO.getNewTargetCode());

					if (!sourceUuid.equals(concept.getPrimordialUuid()))
					{
						throw new MojoExecutionException("Design failure!");
					}

					eConceptUtil_.addRelationship(concept, targetUuid, typeUuid, time);
				}
			}

			eConceptUtil_.addRefsetMember(allVhatConceptsRefset, concept.getPrimordialUuid(), true, time);
			concept.writeExternal(dos);
		}
	}

	public EConcept createType(DataOutputStream dos, UUID parentUuid, String typeName) throws Exception
	{
		EConcept concept = eConceptUtil_.createConcept(typeName, typeName);
		loadedConcepts.put(concept.getPrimordialUuid().toString(), typeName);
		eConceptUtil_.addRelationship(concept, parentUuid, null, null);
		concept.writeExternal(dos);
		return concept;
	}

	public EConcept createType(DataOutputStream dos, UUID parentUuid, String typeName, UUID typeUuid, Set<Long> refsetMembership) throws Exception
	{
		EConcept concept = eConceptUtil_.createConcept(typeUuid, typeName, null, eConceptUtil_.statusCurrentUuid_);
		loadedConcepts.put(concept.getPrimordialUuid().toString(), typeName);
		eConceptUtil_.addRelationship(concept, parentUuid, null, null);

		if (refsetMembership != null)
		{
			for (Long memberVuid : refsetMembership)
			{
				eConceptUtil_.addRefsetMember(concept, getDescriptionUuid(memberVuid.toString()), true, null);
			}
		}

		concept.writeExternal(dos);
		return concept;
	}

	public UUID getSubsetUuid(String vuid)
	{
		return ConverterUUID.nameUUIDFromBytes((uuidRoot_ + "subset:" + vuid).getBytes());
	}

	public UUID getConceptUuid(String codeId)
	{
		return ConverterUUID.nameUUIDFromBytes((uuidRoot_ + "code:" + codeId).getBytes());
	}

	public UUID getDescriptionUuid(String descriptionId)
	{
		return ConverterUUID.nameUUIDFromBytes((uuidRoot_ + "description:" + descriptionId).getBytes());
	}

	public static void main(String[] args) throws MojoExecutionException
	{
		VHATImportMojo i = new VHATImportMojo();
		i.outputDirectory = new File("../vhat-econcept/target");
		i.inputFile = new File("../vhat-econcept/target/generated-resources/xml/");
		i.execute();
	}
}