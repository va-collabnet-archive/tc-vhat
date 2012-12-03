package gov.va.med.term.vhat.mojo;

import gov.va.med.term.vhat.data.TerminologyDataReader;
import gov.va.med.term.vhat.data.dto.ConceptImportDTO;
import gov.va.med.term.vhat.data.dto.DesignationImportDTO;
import gov.va.med.term.vhat.data.dto.PropertyImportDTO;
import gov.va.med.term.vhat.data.dto.RelationshipImportDTO;
import gov.va.med.term.vhat.data.dto.SubsetImportDTO;
import gov.va.med.term.vhat.data.dto.SubsetMembershipImportDTO;
import gov.va.med.term.vhat.data.dto.TypeImportDTO;
import gov.va.med.term.vhat.data.dto.Version;
import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
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
import org.dwfa.ace.refset.ConceptConstants;
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
public class VHATImportMojo extends AbstractMojo {

    private Map<String, EConcept> typeMap = new HashMap<String, EConcept>();
    private Map<String, List<RelationshipImportDTO>> relationshipMap = null;
    private TerminologyDataReader importer_;
    private EConceptUtility eConceptUtil_;
    
    /**
     * Where to put the output file.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    
    private File outputDirectory;
    /**
     * Location of source data file.  May be a file or a directory.
     * 
     * @parameter
     * @required
     */
    private File inputFile;
    
    private int conceptsWithNoDesignations = 0;
    
    private String uuidRoot_ = "gov.va.med.term.vhat:";

    public void execute() throws MojoExecutionException {
        File f = outputDirectory;

        try {
            if (!f.exists()) {
                f.mkdirs();
            }

            File touch = new File(f, "VHATEConcepts.jbin");
            DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(touch)));

            
            eConceptUtil_ = new EConceptUtility(uuidRoot_);
            
            importer_ = new TerminologyDataReader(inputFile);
            List<ConceptImportDTO> items = importer_.process();

            relationshipMap = importer_.getRelationshipsMap();
            List<TypeImportDTO> dto = importer_.getTypes();
            
            EConcept vhatMetadata = createType(dos, ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid(), "VHAT Metadata");
            //This is chosen to line up with other va refsets
            EConcept vaRefsets = createType(dos, ConceptConstants.REFSET.getUuids()[0], "VA Refsets", 
            		ConverterUUID.nameUUIDFromBytes(("gov.va.refset.VA Refsets").getBytes()), null);
            
            
            EConcept properties = createType(dos, vhatMetadata.primordialUuid, "Attribute Types");
            EConcept descriptions = createType(dos, vhatMetadata.primordialUuid, "Description Types");
            EConcept relationships = createType(dos, vhatMetadata.primordialUuid, "Relationship Types");
            EConcept contentVersion = createType(dos, vhatMetadata.primordialUuid, "Content Version");
            EConcept idTypes = createType(dos, vhatMetadata.primordialUuid, "ID Types");
            
            EConcept subsetRefset = createType(dos, vaRefsets.primordialUuid, "VHAT Subsets");

            Map<Long, Set<Long>> subsetMembershipMap = new HashMap<Long, Set<Long>>();
            // get the subset memberships to build the refset for each subset
            for (ConceptImportDTO item : items) {
                List<DesignationImportDTO> designations = item.getDesignations();
                for (DesignationImportDTO designation : designations) {
                    List<SubsetMembershipImportDTO> subsets = designation.getSubsets();
                    for (SubsetMembershipImportDTO subsetMembership : subsets) {
                        Set<Long> vuids = subsetMembershipMap.get(subsetMembership.getVuid());
                        if (vuids == null) {
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
	    		createType(dos, 
	    				subsetRefset.getPrimordialUuid(), 
	    				subset.getSubsetName(),
	    				getSubsetUuid(subset.getVuid() + ""), 
	    				subsetMembershipMap.get(subset.getVuid()));  //TODO this duplicates the subset info - it is also added to the concepts.  Do we want both?  Are they exact duplicates?
            }

            // create all the types for properties, descriptions and relationships
            for (TypeImportDTO typeImportDTO : dto) {
                if (typeImportDTO.getKind().equals("DesignationType")) {
                    typeMap.put(typeImportDTO.getName(), createType(dos, descriptions.getPrimordialUuid(), typeImportDTO.getName()));
                } else if (typeImportDTO.getKind().equals("RelationshipType")) {
                    typeMap.put(typeImportDTO.getName(), createType(dos, relationships.getPrimordialUuid(), typeImportDTO.getName()));
                } else if (typeImportDTO.getKind().equals("PropertyType")) {
                    typeMap.put(typeImportDTO.getName(), createType(dos, properties.getPrimordialUuid(), typeImportDTO.getName()));
                }
            }
            
            typeMap.put("ReleaseDate", createType(dos, contentVersion.getPrimordialUuid(), "ReleaseDate"));
            typeMap.put("VUID", createType(dos, idTypes.getPrimordialUuid(), "VUID"));
            
            for (ConceptImportDTO item : items) {
                writeEConcept(dos, item);
            }
            dos.flush();
            dos.close();
            
            //Put in names instead of IDs so the load stats print nicer:
            Hashtable<String, String> stringsToSwap = new Hashtable<String, String>();
            for (SubsetImportDTO subset : subsets) 
            {
                stringsToSwap.put(subset.getVuid() + "", subset.getSubsetName());
            }
            
            System.out.println("Load Statistics");
            //swap out vuids with names to make it more readable...
            for (String line : eConceptUtil_.getLoadStats().getSummary())
            {
            	Enumeration<String> e = stringsToSwap.keys();
            	while (e.hasMoreElements())
            	{
            		String current = e.nextElement();
            		line = line.replaceAll(current, stringsToSwap.get(current));
            	}
            	System.out.println(line);
            }
            
    		//this could be removed from final release.  Just added to help debug editor problems.
    		ConsoleUtil.println("Dumping UUID Debug File");
    		ConverterUUID.dump(new File(outputDirectory, "vhatUuidDebugMap.txt"));
    		
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
        }

        if (conceptsWithNoDesignations > 0)
        {
        	System.err.println(conceptsWithNoDesignations + " concepts were found with no descriptions at all.  These were assigned '-MISSING-'");
        }

    }

    public void writeEConcept(DataOutputStream dos,ConceptImportDTO conceptDto) throws Exception 
    {
        long time = System.currentTimeMillis();
        
        EConcept concept = eConceptUtil_.createConcept(getConceptUuid(conceptDto.getVuid().toString()), time, eConceptUtil_.statusCurrentUuid_);
        eConceptUtil_.addAdditionalIds(concept, conceptDto.getVuid().toString(), typeMap.get("VUID").getPrimordialUuid(), false);
        
		for (PropertyImportDTO property : conceptDto.getProperties())
		{
			eConceptUtil_.addStringAnnotation(concept, property.getValueNew(),typeMap.get(property.getTypeName()).getPrimordialUuid(), false);
		}

        List<DesignationImportDTO> designationDto = conceptDto.getDesignations();
        boolean fullySpecifiedNameAdded = false;
        DesignationImportDTO bestDescription = null;
        for (DesignationImportDTO designationImportDTO : designationDto) 
        {
        	EConcept designationEConcept = typeMap.get(designationImportDTO.getTypeName());
            if (designationEConcept == null) 
            {
                throw new MojoExecutionException("Type Name: " + designationImportDTO.getTypeName());
            }
            if (designationImportDTO.getValueNew() == null) 
            {
                throw new MojoExecutionException("Description is null for concept: " + conceptDto.getName());
            }
        	
            if (bestDescription == null)
            {
            	//Make sure we have something....
            	bestDescription = designationImportDTO;
            }
            
            if (designationImportDTO.getValueNew().equals("VHAT"))
            {
            	//On the root node, we need to add some extra attributes
            	Version version = importer_.getVersion();
            	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            	eConceptUtil_.addStringAnnotation(concept, sdf.format(version.getReleaseDate()), 
            			typeMap.get("ReleaseDate").getPrimordialUuid(), false);
            }

            if (designationImportDTO.getTypeName().equals("Fully Specified Name"))
            {
            	fullySpecifiedNameAdded = true;
            	eConceptUtil_.addFullySpecifiedName(concept,
            			getDescriptionUuid(designationImportDTO.getVuid().toString()),
            			designationImportDTO.getValueNew(), null);
            }
            else if (designationImportDTO.getTypeName().equals("Preferred Name")) 
            {
            	//This one is better
            	bestDescription = designationImportDTO;
            } 
            
            //I like to maintain the terminologies actual type naming for the descriptions as well
            //This will duplicate the FSN and added above in those cases, but thats ok.
            TkDescription addedDescription = eConceptUtil_.addDescription(concept,
        			getDescriptionUuid(designationImportDTO.getVuid().toString()),
        			designationImportDTO.getValueNew(), designationEConcept.getPrimordialUuid(), false);
 
            //VHAT is kind of odd, in that the attributes are attached to the description, rather than the concept.
            for (PropertyImportDTO property : designationImportDTO.getProperties())
            {
    			eConceptUtil_.addStringAnnotation(addedDescription, property.getValueNew(), 
    					typeMap.get(property.getTypeName()).getPrimordialUuid(), false);
            }
            
            //Same here, with the refset membership being attached to the description, rather than the concept.
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
	        	//The workbench implodes if you don't have a fully specified name....
	        	eConceptUtil_.addFullySpecifiedName(concept,bestDescription.getValueNew(), null);
        	}
        	else
        	{
        		//Seems like a data error - but it is happening... no descriptions at all.....
        		conceptsWithNoDesignations++;
	        	//The workbench implodes if you don't have a fully specified name....
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
                UUID typeUuid = typeMap.get(relationshipImportDTO.getTypeName()).getPrimordialUuid();

                if (!sourceUuid.equals(concept.getPrimordialUuid()))
                {
                	throw new MojoExecutionException("Design failure!");
                }
                
                eConceptUtil_.addRelationship(concept, targetUuid, typeUuid, time);
            }
        }
        
        concept.writeExternal(dos);
    }

    public EConcept createType(DataOutputStream dos, UUID parentUuid, String typeName) throws Exception {
    	 EConcept concept = eConceptUtil_.createConcept(typeName, typeName);
         eConceptUtil_.addRelationship(concept, parentUuid, null, null);
         concept.writeExternal(dos);
         return concept;
    }

    public EConcept createType(DataOutputStream dos, UUID parentUuid, String typeName, UUID typeUuid, 
    		Set<Long> refsetMembership) throws Exception 
    {
        EConcept concept = eConceptUtil_.createConcept(typeUuid, typeName, null, eConceptUtil_.statusCurrentUuid_);
        
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
    	i.outputDirectory = new File("../vhat-data/target");
    	i.inputFile = new File("../vhat-data/target/generated-resources/xml/VHAT 20111121.xml");
    	i.execute();
    }
}