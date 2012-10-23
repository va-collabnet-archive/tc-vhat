package gov.va.med.term.vhat.data;

import gov.va.med.term.vhat.data.dto.CodeSystem;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


public class TerminologyDataReader extends DefaultHandler
{

    protected static final int MAX_BATCH_SIZE = 1000;
    protected static final int CONCEPTS_PROCESSED_TO_LOG = 5000;

    protected static final String ROOT_ELEMENT = "Terminology";
    protected static final String CODESYSTEM_ELEMENT = "CodeSystem";
    protected static final String VERSION_ELEMENT = "Version";
    protected static final String CODEDCONCEPT_ELEMENT = "CodedConcept";
    protected static final String CODEDCONCEPTS_ELEMENT = "CodedConcepts";
    protected static final String MAPSET_ELEMENT = "MapSet";
    protected static final String MAPSETS_ELEMENT = "MapSets";
    protected static final String MAPENTRY_ELEMENT = "MapEntry";
    protected static final String MAPENTRIES_ELEMENT = "MapEntries";
    
    protected static final String DESIGNATION_ELEMENT = "Designation";
    protected static final String PROPERTY_ELEMENT = "Property";
    protected static final String RELATIONSHIP_ELEMENT = "Relationship";
    protected static final String TYPE_ELEMENT = "Type";
    protected static final String TYPES_ELEMENT = "Types";
    protected static final String SUBSET_MEMBERSHIP_ELEMENT = "SubsetMembership";
    protected static final String SUBSET_MEMBERSHIPS_ELEMENT = "SubsetMemberships";
    protected static final String MOVE_FROM_CONCEPT_CODE_ELEMENT = "MoveFromConceptCode";

    protected static final String DESIGNATIONS_ELEMENT = "Designations";
    protected static final String PROPERTIES_ELEMENT = "Properties";
    protected static final String RELATIONSHIPS_ELEMENT = "Relationships";
    
    protected static final String SUBSET_ELEMENT = "Subset";
    protected static final String SUBSETS_ELEMENT = "Subsets";

    protected static final String CODE_ELEMENT = "Code";
    protected static final String NAME_ELEMENT = "Name";
    protected static final String VUID_ELEMENT = "VUID";
    
    protected static final String DESCRIPTION_ELEMENT = "Description";
    protected static final String COPYRIGHT_ELEMENT = "Copyright";
    protected static final String COPYRIGHT_URL_ELEMENT = "CopyrightURL";
    
    protected static final String EFFECTIVE_DATE_ELEMENT = "EffectiveDate";
    protected static final String RELEASE_DATE_ELEMENT = "ReleaseDate";
    protected static final String SOURCE_ELEMENT = "Source";
    protected static final String APPEND_ELEMENT = "Append";    
    protected static final String ACTIVE_ELEMENT = "Active";

    protected static final String TYPE_NAME_ELEMENT = "TypeName";
    protected static final String VALUE_NEW_ELEMENT = "ValueNew";
    protected static final String VALUE_OLD_ELEMENT = "ValueOld";

    protected static final String SOURCE_CODE_ELEMENT = "SourceCode";
    protected static final String TARGET_CODE_ELEMENT = "TargetCode";
    protected static final String SEQUENCE_ELEMENT = "Sequence";
    protected static final String GROUPING_ELEMENT = "Grouping";
    protected static final String MUID_ELEMENT = "MUID";

    protected static final String NEW_TARGETCODE_ELEMENT = "NewTargetCode";
    protected static final String OLD_TARGETCODE_ELEMENT = "OldTargetCode";

    protected static final String SOURCE_CODE_SYSTEM = "SourceCodeSystem";
    protected static final String SOURCE_VERSION_NAME = "SourceVersionName";
    protected static final String TARGET_CODE_SYSTEM = "TargetCodeSystem";
    protected static final String TARGET_VERSION_NAME = "TargetVersionName";
    
    protected static final String SUBSET_NAME_ELEMENT = "SubsetName";
    
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    
    protected static final String ACTION_ELEMENT = "Action";
    protected static final String ACTION_ADD = "add";
    protected static final String ACTION_UPDATE = "update";
    protected static final String ACTION_NONE = "none";
    protected static final String ACTION_REMOVE = "remove";

    protected static final String PREFERRED_DESIGNATION_TYPE_ELEMENT = "PreferredDesignationType";

    private static final Object KIND_ELEMENT = "Kind";

//    private static final String KIND_PROPERTY_TYPE = "PropertyType";
//    private static final String KIND_DESIGNATION_TYPE = "DesignationType";
//    private static final String KIND_RELATIONSHIP_TYPE = "RelationshipType";


    protected Stack<String> elementStack = new Stack<String>();
    
    protected String currentElement;
    protected String parentElement;
    protected String elementData;
    protected StringBuffer elementDataBuffer;
    
    protected CodeSystem currentCodeSystem;
    protected Version currentVersion;
    protected String currentConceptCode;
    protected List<ConceptImportDTO> importConcepts = new ArrayList<ConceptImportDTO>();
    protected List<SubsetImportDTO> importSubsets = new ArrayList<SubsetImportDTO>();
    protected List<TypeImportDTO> importTypes = new ArrayList<TypeImportDTO>();
    protected List<RelationshipImportDTO> importRelationships = new ArrayList<RelationshipImportDTO>();
    protected List<SubsetMembershipImportDTO> importSubsetMemberships = null;
    protected List<DesignationImportDTO> importDesignations = null;
    protected List<PropertyImportDTO> importProperties = null;
    protected List<String> importDesignationNames = null;
    protected boolean codeSystemProcessed = false;
    protected boolean versionProcessed = false;
    protected boolean codedConceptProcessed = false;
    protected boolean conceptDesignationProcessed = true;
    protected boolean designationNameProcessed = false;
    protected boolean subsetMembershipProcessed = false;
    protected boolean conceptPropertyProcessed = false;
    protected boolean conceptRelationshipProcessed = false;
    protected ConceptImportDTO importConcept = null;
    protected DesignationImportDTO importDesignation = null;
//    protected MapSet mapSet = null;
    protected Set<Long> mapSetEntityIds = new HashSet<Long>();
    protected Map<RelationshipImportDTO, ConceptImportDTO> conceptToRelationshipMap = new HashMap<RelationshipImportDTO, ConceptImportDTO>();
    
    
    protected String conceptType = null; 
    
    protected File inputFile;
    protected String schemaName;
    protected int numberOfConceptsProcessed = 0;
    protected HashMap<String, String> elementMap = new HashMap<String, String>();

    public TerminologyDataReader(File inputFile)
    {
        this.inputFile = inputFile;
        this.schemaName = "TerminologyData.xsd";
    }

    public List<ConceptImportDTO> process() throws Exception 
    {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
//        XSDLocator builder = new XSDLocator();

        
        try
        {
            if (schemaName != null)
        	{
//                URL url = builder.getClass().getResource(schemaName);
//    			if (url == null)
//    			{
//    				throw new FileNotFoundException("Unable to locate file: " + schemaName);
//    			}

/*    			
    			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);      
    			Schema schema = factory.newSchema(url);      
    			Validator validator = schema.newValidator();      
    			validator.validate(source);
*/
            	
            	SAXParser parser = parserFactory.newSAXParser();
            	
            	if (inputFile.isDirectory())
            	{
            		ArrayList<File> files = new ArrayList<File>();
            		for (File f : inputFile.listFiles())
            		{
            			if (f.isFile() && f.getName().toLowerCase().endsWith(".xml"))
            			{
            				files.add(f);
            			}
            		}
            		
            		if (files.size() != 1)
            		{
            			throw new Exception(files.size() + " xml files were found inside of " + inputFile.getAbsolutePath() 
            					+ " but this implementation requires 1 and only 1 xml files to be present.");
            		}
            		
            		System.out.println("Processing: " + files.get(0).getAbsolutePath());
            		parser.parse(files.get(0), this);
            		
            	}
            	else
            	{
            		System.out.println("Processing: " + inputFile.getAbsolutePath());
            		parser.parse(inputFile, this);
            	}
        	}
        }
		catch (SAXParseException e)
		{
			throw new Exception("The import file did not validate against the Schema file: "
					+ schemaName + " at line " +  e.getLineNumber() + ", column " + e.getColumnNumber() + ". The error is: " + e.getMessage(), e);
		}
		catch (NullPointerException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new Exception(e.getMessage(), e);
		}
		return importConcepts;

    }

    public void startElement(String namespaceUri, String localName,
            String qualifiedName, Attributes attributes) throws SAXException
    {
        parentElement = currentElement;
        currentElement = qualifiedName;
        elementStack.push(qualifiedName);
        elementDataBuffer = new StringBuffer();

        
        try
        {
            // we are starting CodedConcepts so let's finish up with codeSystem
            if (!processElements(qualifiedName, true))
            {
                if (currentElement.equals(VERSION_ELEMENT))
                {
                	processCodeSystem();
                }
                else if (currentElement.equals(CODEDCONCEPTS_ELEMENT))
                {
                	conceptType = CODEDCONCEPT_ELEMENT;
                    processVersion();
                }
                else if (currentElement.equals(DESIGNATIONS_ELEMENT) || 
                        currentElement.equals(RELATIONSHIPS_ELEMENT) )
                {
                    // we need to process the concept information
                    checkConceptProccessed();
                }
                else if (currentElement.equalsIgnoreCase(PROPERTIES_ELEMENT))
                {
                    checkConceptProccessed();
                    checkDesignationProcessed();
                }
                else if (currentElement.equals(MAPENTRIES_ELEMENT))
                {
                    conceptElement(currentElement, false);
                    // we are at the end of map sets
                	conceptType = MAPENTRY_ELEMENT;
                }
                else if (currentElement.equals(SUBSET_MEMBERSHIPS_ELEMENT))
                {
                    checkDesignationProcessed();
                }
                else if (currentElement.equals(MAPSETS_ELEMENT))
                {
                	conceptType = MAPSET_ELEMENT;
                	if (currentVersion == null)
                	{
                	    processVersion();
                	}
                	// we only use the stat for MAP Sets so get it only under that condition
                }
            }
        }
        catch(NullPointerException e)
        {
        	throw e;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new SAXException(e);
        }
    }

    public void endElement(String namespaceUri, String localName,
            String qualifiedName) throws SAXException
    {
        elementData = elementDataBuffer.toString();
        
        currentElement = (String) elementStack.pop();
        if (elementStack.size() > 1)
        {
            parentElement = (String) elementStack.peek();
        }

        try
        {
            if (!processElements(qualifiedName, false))
            {
                if (currentElement.equals(CODESYSTEM_ELEMENT))
                {
                }
                if (currentElement.equals(VERSION_ELEMENT))
                {
                    // we need to load the version because the current version object is not associated with the session
                }
                if (qualifiedName.equals(TYPE_ELEMENT))
                {
                    // the the type and add it to the list
                    TypeImportDTO typeDTO = getTypeData();
                    importTypes.add(typeDTO);
                }
                else if (qualifiedName.equals(RELATIONSHIP_ELEMENT))
                {
                    // add the relationship
                    importRelationships.add(getRelationshipData());
                }
                else if (qualifiedName.equals(PROPERTY_ELEMENT))
                {
                    // add the property
                    importProperties.add(getPropertyData());
                }
                else if (qualifiedName.equals(PROPERTIES_ELEMENT) && parentElement.equals(DESIGNATION_ELEMENT))
                {
                    importDesignation.addProperties(importProperties);
                    importProperties.clear();
                }
                else if (qualifiedName.equals(SUBSET_ELEMENT))
                {
                    SubsetImportDTO subsetDTO = getSubsetData();
                    importSubsets.add(subsetDTO);
                }
                else if (qualifiedName.equals(CODEDCONCEPTS_ELEMENT) || qualifiedName.equals(MAPSETS_ELEMENT) ||
                		qualifiedName.equals(MAPENTRIES_ELEMENT))
                {
                }
                else if (qualifiedName.equals(SUBSET_MEMBERSHIP_ELEMENT))
                {
                    importSubsetMemberships.add(getSubsetMembershipData());
                }
                else
                {
                    elementMap.put(currentElement, elementData);
                }
            }
        }
        catch (NullPointerException ex)
        {
        	throw ex;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new SAXException(e);
        }
    }

	public void characters(char[] chars, int startIndex, int endIndex)
    {
        String s = new String(chars, startIndex, endIndex);
        
        if (elementDataBuffer != null)
        {
            elementDataBuffer.append(s);
        }            
    }

    protected boolean processElements(String elementName, boolean isStartElement) throws Exception
    {
        boolean processed = true;
        if (elementName.equals(DESIGNATION_ELEMENT))
        {
            conceptDesignationElement(isStartElement);
        }
        else if (elementName.equals(CODEDCONCEPT_ELEMENT) || elementName.equals(MAPSET_ELEMENT) || elementName.equals(MAPENTRY_ELEMENT))
        {
            conceptElement(elementName, isStartElement);
        }
        else
        {
            processed = false;
        }
        return processed;
    }

    
    protected void conceptElement(String elementName, boolean isStartElement) throws Exception
    {
        if (isStartElement)
        {
            importDesignations = new ArrayList<DesignationImportDTO>();
            importProperties = new ArrayList<PropertyImportDTO>();
            codedConceptProcessed = false;
        }
        else
        {
            checkConceptProccessed();
            importConcept.setDesignations(importDesignations);
            importConcept.setProperties(importProperties);
        }
    }
    
    protected void conceptDesignationElement(boolean isStartElement) throws Exception
    {
        if (isStartElement)
        {
            importSubsetMemberships = new ArrayList<SubsetMembershipImportDTO>();
            conceptDesignationProcessed = false;
        }
        else
        {
            checkDesignationProcessed();
            importDesignation.setSubsets(importSubsetMemberships);
        }
    }

    private void checkDesignationProcessed() throws Exception
    {
        if (conceptDesignationProcessed == false)
        {
            conceptDesignationProcessed = true;
            importDesignation = getDesignationData();
            importDesignations.add(importDesignation);
        }
    }
    /**
     * @throws Exception 
     */
    private void checkConceptProccessed() throws Exception
    {
        // process codedConcept (Designation start element)
        if (codedConceptProcessed == false)
        {
            codedConceptProcessed = true;
            importConcept = getConceptData();
            importConcepts.add(importConcept);
            numberOfConceptsProcessed++;
        }
    }

    
    protected void processCodeSystem() throws Exception
    {
    	String codeSystemName = elementMap.get(NAME_ELEMENT);
        String vuidString = elementMap.get(VUID_ELEMENT);
//    	String action = elementMap.get(ACTION_ELEMENT);
    	String description = elementMap.get(DESCRIPTION_ELEMENT);
    	String copyright = elementMap.get(COPYRIGHT_ELEMENT);
    	String copyrightURL = elementMap.get(COPYRIGHT_URL_ELEMENT);
    	String preferredDesignationType = elementMap.get(PREFERRED_DESIGNATION_TYPE_ELEMENT);
    	elementMap.clear();
    	
        Long vuid = null;
        if (vuidString != null)
        {
            vuid = Long.valueOf(vuidString);
        }

        CodeSystem codeSystem = new CodeSystem(codeSystemName, vuid, description, copyright, copyrightURL, preferredDesignationType);

        currentCodeSystem = codeSystem;
    }
    
    protected void processVersion() throws Exception
    {
        currentVersion = new Version();
        currentVersion.setCodeSystem(currentCodeSystem);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        currentVersion.setReleaseDate(sdf.parse(elementMap.get(RELEASE_DATE_ELEMENT)));
        currentVersion.setEffectiveDate(sdf.parse(elementMap.get(EFFECTIVE_DATE_ELEMENT)));
        currentVersion.setDescription(elementMap.get(DESCRIPTION_ELEMENT));
        currentVersion.setName(elementMap.get(NAME_ELEMENT));
        currentVersion.setSource(elementMap.get(SOURCE_ELEMENT));
        currentVersion.setAppend(Boolean.parseBoolean(elementMap.get(APPEND_ELEMENT)));
        
        elementMap.clear();

    }
    
    /**
     * @param versionElement
     * @param codeSystem
     * @param version
     * @throws Exception 
     */
    protected ConceptImportDTO getConceptData() throws Exception
    {
    	String action = elementMap.get(ACTION_ELEMENT);
    	String name = elementMap.get(NAME_ELEMENT);
        String vuidString = elementMap.get(VUID_ELEMENT);
    	String code = elementMap.get(CODE_ELEMENT);
    	String active = elementMap.get(ACTIVE_ELEMENT);
        Long vuid = null;
        
    	if (active == null && (ACTION_ADD.equals(action) || ACTION_UPDATE.equals(action)) )
    	{
    	    throw new Exception("Active element must be specified for concept code: "+code);
    	}
    	else if (active == null)
    	{
            active = Boolean.TRUE.toString();
    	}
        if (vuidString != null)
        {
            vuid = Long.valueOf(vuidString);
        }
    	ConceptImportDTO conceptDTO = null;
    	
    	if (conceptType.equals(MAPENTRY_ELEMENT))
    	{
    		String sourceCode = elementMap.get(SOURCE_CODE_ELEMENT);
    		String targetCode = elementMap.get(TARGET_CODE_ELEMENT);
    		String sequenceString = elementMap.get(SEQUENCE_ELEMENT);
    		String groupingString = elementMap.get(GROUPING_ELEMENT);
    		String muidString = elementMap.get(MUID_ELEMENT);
    		Long muid = null;
    		int sequence = 0;
    		Long grouping = null;
            if (muidString != null)
            {
                muid = Long.valueOf(muidString);
            }
            if (sequenceString != null)
            {
            	sequence = Integer.valueOf(sequenceString);
            }
            if (groupingString != null)
            {
            	grouping = Long.valueOf(groupingString);
            }
            conceptDTO = new MapEntryImportDTO(action, name, code, vuid, parseBoolean(active),sourceCode, targetCode, sequence, grouping, muid);
    	}
    	else if (conceptType.equals(MAPSET_ELEMENT))
    	{
    		String sourceCodeSystem = elementMap.get(SOURCE_CODE_SYSTEM);
    		String sourceVersionName = elementMap.get(SOURCE_VERSION_NAME);
    		String targetCodeSystem = elementMap.get(TARGET_CODE_SYSTEM);
    		String targetVersionName = elementMap.get(TARGET_VERSION_NAME);
    		conceptDTO = new MapSetImportDTO(action, name, code, vuid, parseBoolean(active), sourceCodeSystem, sourceVersionName, targetCodeSystem, targetVersionName);
    	}
    	else if (conceptType.equals(CODEDCONCEPT_ELEMENT))
    	{
        	conceptDTO = new ConceptImportDTO(action, name, code, vuid, parseBoolean(active));        
    	}
        currentConceptCode = code;
        elementMap.clear();

        return conceptDTO;
    }
    
    protected TypeImportDTO getTypeData() throws Exception
    {
        String name = elementMap.get(NAME_ELEMENT);
        String kind = elementMap.get(KIND_ELEMENT);
        elementMap.clear();
        
        TypeImportDTO typeDTO = new TypeImportDTO(kind, name);
        
        return typeDTO;
    }
    
    private SubsetImportDTO getSubsetData() throws Exception
    {
        String action = elementMap.get(ACTION_ELEMENT);
        String subsetName = elementMap.get(NAME_ELEMENT);
        String vuidString = elementMap.get(VUID_ELEMENT);
        String active = elementMap.get(ACTIVE_ELEMENT);
        Long vuid = null;
        elementMap.clear();
        
        if (active == null)
        {
            // default of 'Active' element is true
            active = "true";
        }
        if (vuidString != null)
        {
            vuid = Long.valueOf(vuidString);
        }
        SubsetImportDTO subsetDTO = new SubsetImportDTO(action, subsetName, vuid, parseBoolean(active));
        
        return subsetDTO;
    }

    private DesignationImportDTO getDesignationData() throws Exception
    {
    	String action = elementMap.get(ACTION_ELEMENT);
    	String typeName = elementMap.get(TYPE_NAME_ELEMENT);
    	String code = elementMap.get(CODE_ELEMENT);
        String vuidString = elementMap.get(VUID_ELEMENT);
    	String valueOld = elementMap.get(VALUE_OLD_ELEMENT);
    	String valueNew = elementMap.get(VALUE_NEW_ELEMENT);
    	String active = elementMap.get(ACTIVE_ELEMENT);
    	String moveFromConceptCode = elementMap.get(MOVE_FROM_CONCEPT_CODE_ELEMENT);
        elementMap.clear();

        Long vuid = null;
    	if (active == null)
    	{
    		// default of 'Active' element is true
    		active = "true";
    	}
    	
        if (vuidString != null)
        {
            vuid = Long.valueOf(vuidString);
        }
        
    	DesignationImportDTO designationDTO = new DesignationImportDTO(action, typeName, code, valueOld, valueNew, vuid, parseBoolean(active));
    	designationDTO.setMoveFromConceptCode(moveFromConceptCode);
        return designationDTO;
    }
    
    protected PropertyImportDTO getPropertyData() throws Exception
    {
        String action = elementMap.get(ACTION_ELEMENT);
        String typeName = elementMap.get(TYPE_NAME_ELEMENT);
        String valueOld = elementMap.get(VALUE_OLD_ELEMENT);
        String valueNew = elementMap.get(VALUE_NEW_ELEMENT);
        String active = elementMap.get(ACTIVE_ELEMENT);
        elementMap.clear();

        if (active == null)
        {
            // default of 'Active' element is true
            active = "true";
        }
        if (ACTION_UPDATE.equals(action))
        {
        	if (valueOld == null)
        	{
                throw new Exception("Property old value cannot be null on an action 'update' for Concept code: "+currentConceptCode+" and CodeSystem: "+currentCodeSystem.getName());
        	}
        }
        if (valueOld != null && valueOld.equals(valueNew) == true)
        {
            throw new Exception("Property old and new values ("+valueOld+") cannot be the same for Property type: "+typeName+", Concept code: "+currentConceptCode+" and CodeSystem: "+currentCodeSystem.getName());
        }

        PropertyImportDTO propertyDTO = new PropertyImportDTO(action, typeName, valueOld, valueNew, parseBoolean(active));

        return propertyDTO;
    }

    protected RelationshipImportDTO getRelationshipData() throws Exception
    {
        String action = elementMap.get(ACTION_ELEMENT);
        String typeName = elementMap.get(TYPE_NAME_ELEMENT);
        String newTargetCode = elementMap.get(NEW_TARGETCODE_ELEMENT);
        String oldTargetCode = elementMap.get(OLD_TARGETCODE_ELEMENT);
        String active = elementMap.get(ACTIVE_ELEMENT);
        elementMap.clear();

        if (active == null)
        {
            // default of 'Active' element is true
            active = "true";
        }
        if (oldTargetCode != null && oldTargetCode.equals(newTargetCode) == true)
        {
            throw new Exception("Old target code and new target code ("+oldTargetCode+") cannot be the same for Relationship type: "+typeName+", Concept code: "+currentConceptCode+" and CodeSystem: "+currentCodeSystem.getName());
        }

        
        RelationshipImportDTO relationshipDTO = new RelationshipImportDTO(action, typeName, importConcept.getCode(), oldTargetCode, newTargetCode, parseBoolean(active));

        if (importConcept.getCode() == null)
        {
            conceptToRelationshipMap.put(relationshipDTO, importConcept);
        }
        
        return relationshipDTO;
    }

    private SubsetMembershipImportDTO getSubsetMembershipData()
    {
        String action = elementMap.get(ACTION_ELEMENT);
        String vuidString = elementMap.get(VUID_ELEMENT);
        String active = elementMap.get(ACTIVE_ELEMENT);
        elementMap.clear();

        SubsetMembershipImportDTO subsetDTO = new SubsetMembershipImportDTO(action, Long.valueOf(vuidString), parseBoolean(active));
        return subsetDTO;
    }
    
    private boolean parseBoolean(String value)
    {
        boolean result = false;
        if (value == null || value.equalsIgnoreCase("true") || value.equals("1"))
        {
            result = true;
        }
        
        return result;
    }
    
    public List<TypeImportDTO> getTypes()
    {
    	return this.importTypes;
    }
    public List<RelationshipImportDTO> getRelationships()
    {
    	return importRelationships;
    }
    
    public Version getVersion()
    {
    	return currentVersion;
    }
    
    public Map<String, List<RelationshipImportDTO>> getRelationshipsMap()
    {
    	Map<String, List<RelationshipImportDTO>> results = new HashMap<String, List<RelationshipImportDTO>>();
    	for (RelationshipImportDTO relationship : importRelationships)
		{
    		String code = relationship.getSourceCode();
    		List<RelationshipImportDTO> list = results.get(code);
    		if (list == null)
    		{
    			list = new ArrayList<RelationshipImportDTO>();
    			results.put(code, list);
    		}
    		list.add(relationship);
		}
    	return results;
    }
    
    public List<SubsetImportDTO> getSubsets()
    {
    	return importSubsets;
    }

//    public static void main(String[] args) throws Exception
//    {
//        TerminologyDataReader importer = new TerminologyDataReader("C:/TerminologyData.xml");
//        List<ConceptImportDTO> items = importer.process();
//        List<TypeImportDTO> dto = importer.getTypes();
//        for (TypeImportDTO typeImportDTO : dto)
//		{
//        	if (typeImportDTO.getKind().equals("DesignationType"))
//        	{
////        		System.out.println(typeImportDTO.getName());
//        	}
//		}
//        List<SubsetImportDTO> importSubsets = importer.getSubsets();
//        for (SubsetImportDTO subsetImportDTO : importSubsets)
//		{
//			System.out.println(subsetImportDTO.getSubsetName());
//		}
///*        
//        List<Relationsh/ipImportDTO> rels = importer.getRelationships();
//        for (RelationshipImportDTO relationshipImportDTO : rels)
//		{
//			System.out.println(relationshipImportDTO.getSourceCode()+ " -> "+ relationshipImportDTO.getNewTargetCode()+ " type:"+ relationshipImportDTO.getTypeName());
//		}
//*/		
//    }
    
}
