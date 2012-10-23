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
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
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
    private Map<Long, EConcept> subsetMap = new HashMap<Long, EConcept>();
    private Map<String, List<RelationshipImportDTO>> relationshipMap = null;
    private LoadStats ls_ = new LoadStats();
    
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

    public void execute() throws MojoExecutionException {
        File f = outputDirectory;

        try {
            if (!f.exists()) {
                f.mkdirs();
            }

            File touch = new File(f, "VHATEConcepts.jbin");
            DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(touch)));

            eConceptUtil_ = new EConceptUtility("gov.va.med.term.vhat:");
            
            importer_ = new TerminologyDataReader(inputFile);
            List<ConceptImportDTO> items = importer_.process();

            relationshipMap = importer_.getRelationshipsMap();
            List<TypeImportDTO> dto = importer_.getTypes();
            
            EConcept vhatMetadata = createType(dos, ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid(), "VHAT Metadata");
            //This is chosen to line up with other va refsets
            EConcept vaRefsets = createType(dos, ConceptConstants.REFSET.getUuids()[0], "VA Refsets", 
            		UUID.nameUUIDFromBytes(("gov.va.refset.VA Refsets").getBytes()), null);
            
            
            EConcept properties = createType(dos, vhatMetadata.primordialUuid, "Properties");
            EConcept descriptions = createType(dos, vhatMetadata.primordialUuid, "Descriptions");
            EConcept relationships = createType(dos, vhatMetadata.primordialUuid, "Relationships");
            
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
            for (SubsetImportDTO subset : subsets) {
                subsetMap.put(subset.getVuid(), 
                		createType(dos, 
                				subsetRefset.getPrimordialUuid(), 
                				subset.getSubsetName(),
                				eConceptUtil_.getSubsetUuid(subset.getVuid() + ""), 
                				subsetMembershipMap.get(subset.getVuid())));
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
            
            typeMap.put("ReleaseDate", createType(dos, properties.getPrimordialUuid(), "ReleaseDate"));
            typeMap.put("VUID", createType(dos, ArchitectonicAuxiliary.Concept.ID_SOURCE.getPrimoridalUid(), "VUID"));
            
            for (ConceptImportDTO item : items) {
                writeEConcept(dos, item);
            }
            dos.flush();
            dos.close();
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
        }
        
        System.out.println("Load Statistics");
        for (String s : ls_.getSummary())
        {
        	System.out.println(s);
        }
        
        if (conceptsWithNoDesignations > 0)
        {
        	System.err.println(conceptsWithNoDesignations + " concepts were found with no descriptions at all.  These were assigned '-MISSING-'");
        }

    }

    public void writeEConcept(DataOutputStream dos,ConceptImportDTO conceptDto) throws Exception 
    {
        long time = System.currentTimeMillis();
        
        EConcept concept = eConceptUtil_.createConcept(eConceptUtil_.getConceptUuid(conceptDto.getVuid().toString()), time);
        ls_.addConcept();
 
        eConceptUtil_.addAdditionalIds(concept, conceptDto.getVuid().toString(), 
    		   typeMap.get("VUID").getPrimordialUuid(),
    		   false, time);
		ls_.addId("VUID");
        
		for (PropertyImportDTO property : conceptDto.getProperties())
		{
			eConceptUtil_.addAnnotation(concept.getConceptAttributes(), 
					eConceptUtil_.getPropertyUuid(concept.getPrimordialUuid().toString() + property.getValueNew()),
					property.getValueNew(), typeMap.get(property.getTypeName()).getPrimordialUuid(), false, time);
			ls_.addAnnotation("Concept", property.getTypeName());
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
            
            TkDescription addedDescription = null;

            if (designationImportDTO.getTypeName().equals("Fully Specified Name"))
            {
            	fullySpecifiedNameAdded = true;
            	addedDescription =  eConceptUtil_.addFullySpecifiedName(concept,
            			eConceptUtil_.getDescriptionUuid(designationImportDTO.getVuid().toString()),
            			designationImportDTO.getValueNew(), time);
            	ls_.addDescription(designationImportDTO.getTypeName());
            	ls_.addAnnotation("Description", "US English Refset");
            }
            else if (designationImportDTO.getTypeName().equals("Preferred Name")) 
            {
            	addedDescription = eConceptUtil_.addSynonym(concept, 
            			eConceptUtil_.getDescriptionUuid(designationImportDTO.getVuid().toString()), 
            			designationImportDTO.getValueNew(), true, time);
            	ls_.addAnnotation("Synonym", "US English Refset");
            	ls_.addDescription("Preferred Name -> Synonym");
            	
            	//This one is better
            	bestDescription = designationImportDTO;
            	
            	
                if (designationImportDTO.getValueNew().equals("VHAT"))
                {
                	//On the root node, we need to add some extra attributes
                	Version version = importer_.getVersion();
                	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                	eConceptUtil_.addAnnotation(concept.getConceptAttributes(),
                			eConceptUtil_.getPropertyUuid("ReleaseDate:" + sdf.format(version.getReleaseDate())),
                			sdf.format(version.getReleaseDate()), typeMap
                			.get("ReleaseDate").getPrimordialUuid(), false, time);
        			ls_.addAnnotation("Concept", "ReleaseDate");
                }
            } 
            else 
            {
            	addedDescription = eConceptUtil_.addDescription(concept,
            			eConceptUtil_.getDescriptionUuid(designationImportDTO.getVuid().toString()),
            			designationImportDTO.getValueNew(), designationEConcept.getPrimordialUuid(), false, time);
            	ls_.addDescription(designationImportDTO.getTypeName());
            }
 
            for (PropertyImportDTO property : designationImportDTO.getProperties())
            {
    			eConceptUtil_.addAnnotation(addedDescription, 
    					eConceptUtil_.getPropertyUuid(addedDescription.getPrimordialComponentUuid().toString() + property.getValueNew()),
    					property.getValueNew(), 
    					typeMap.get(property.getTypeName()).getPrimordialUuid(), false, time);
    			ls_.addAnnotation("Description", property.getTypeName());
            }
            
            if (designationImportDTO.getSubsets() != null)
            {
            	for (SubsetMembershipImportDTO subset : designationImportDTO.getSubsets()) 
            	{
            		eConceptUtil_.addAnnotation(addedDescription, 
            				eConceptUtil_.getSubsetUuid(addedDescription.getPrimordialComponentUuid().toString() + subset.getVuid().toString()),
            				eConceptUtil_.getSubsetUuid(subset.getVuid() + ""), 
            				subsetMap.get(subset.getVuid()).getPrimordialUuid(), time);
            		ls_.addAnnotation("Description", subsetMap.get(subset.getVuid()).getDescriptions().get(0).getText());
            	}
            }
        }
        
        if (!fullySpecifiedNameAdded)
        {
        	if (bestDescription != null)
        	{
	        	//The workbench implodes if you don't have a fully specified name....
	        	eConceptUtil_.addFullySpecifiedName(concept,
	        			eConceptUtil_.getDescriptionUuid("GeneratedFSN" + bestDescription.getVuid().toString()),
	        			bestDescription.getValueNew(), time);
	        	ls_.addDescription("Fully Specified Name");
	        	ls_.addAnnotation("Description", "US English Refset");
        	}
        	else
        	{
        		//Seems like a data error - but it is happening... no descriptions at all.....
        		conceptsWithNoDesignations++;
	        	//The workbench implodes if you don't have a fully specified name....
	        	eConceptUtil_.addFullySpecifiedName(concept,
	        			eConceptUtil_.getDescriptionUuid("Generated Name" + conceptsWithNoDesignations),
	        			"-MISSING-", time);
	        	ls_.addDescription("Fully Specified Name");
	        	ls_.addAnnotation("Description", "US English Refset");
        	}
        }

        List<RelationshipImportDTO> relationshipImports = relationshipMap.get(conceptDto.getCode());
        if (relationshipImports != null) 
        {
            for (RelationshipImportDTO relationshipImportDTO : relationshipImports) 
            {
                UUID sourceUuid = eConceptUtil_.getConceptUuid(relationshipImportDTO.getSourceCode());
                UUID targetUuid = eConceptUtil_.getConceptUuid(relationshipImportDTO.getNewTargetCode());
                UUID typeUuid = typeMap.get(relationshipImportDTO.getTypeName()).getPrimordialUuid();

                if (!sourceUuid.equals(concept.getPrimordialUuid()))
                {
                	throw new MojoExecutionException("Design failure!");
                }
                
                eConceptUtil_.addRelationship(concept, 
                		targetUuid, 
                		typeUuid, 
                		eConceptUtil_.getConceptUuid(relationshipImportDTO.getSourceCode() + ":" + relationshipImportDTO.getNewTargetCode()), time);

                 ls_.addRelationship(relationshipImportDTO.getTypeName());
            }
        }
        
        concept.writeExternal(dos);
    }

    public EConcept createType(DataOutputStream dos, UUID parentUuid, String typeName) throws Exception {
        return createType(dos, parentUuid, typeName, eConceptUtil_.getTypeUuid(typeName), null);
    }

    public EConcept createType(DataOutputStream dos, UUID parentUuid, String typeName, UUID typeUuid, 
    		Set<Long> refsetMembership) throws Exception 
    {
        long time = System.currentTimeMillis();

        EConcept concept = eConceptUtil_.createConcept(typeUuid, time);
        ls_.addConcept();
        
        eConceptUtil_.addFullySpecifiedName(concept, eConceptUtil_.getDescriptionUuid(typeName), typeName, time);
        ls_.addDescription("Fully Specified Name");
        ls_.addAnnotation("Description", "US English Refset");
        eConceptUtil_.addSynonym(concept, eConceptUtil_.getDescriptionUuid("Generated Synonym" + typeName), typeName, true, time);
        ls_.addAnnotation("Synonym", "US English Refset");
        ls_.addDescription("Synonym");

        eConceptUtil_.addRelationship(concept, parentUuid, null, eConceptUtil_.getRelUuid(typeName), time);
        ls_.addRelationship("is a");

        if (refsetMembership != null) 
        {
             for (Long memberVuid : refsetMembership) 
             {
            	 eConceptUtil_.addRefsetMember(concept,
            			 eConceptUtil_.getDescriptionUuid(memberVuid.toString()),
            			 typeUuid, 
            			 eConceptUtil_.getSubsetUuid(concept.getConceptAttributes().getPrimordialComponentUuid().toString() + 
            					 memberVuid.toString()), time);
                 ls_.addSubsetMember(typeName);
             }
        }

        concept.writeExternal(dos);
        

        return concept;
    }
    
    public static void main(String[] args) throws MojoExecutionException
    {
    	VHATImportMojo i = new VHATImportMojo();
    	i.outputDirectory = new File("../vhatConvert-data/target");
    	i.inputFile = new File("../vhatConvert-data/target/generated-resources/xml/VHAT 20111121.xml");
    	i.execute();
    }
}