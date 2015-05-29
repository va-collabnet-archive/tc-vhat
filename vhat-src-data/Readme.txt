Steps to deploy new source content:

	1) Place the source files into the native-source folder
	2) Update the version number as appropriate in pom.xml
	3) Run a command like this to deploy - (VA is a server name that must be defined with credentials in your maven configuration):
		mvn deploy -DaltDeploymentRepository=maestro::default::https://va.maestrodev.com/archiva/repository/data-files/
		
Note - new source content should not be checked into SVN.  When finished, simply empty the native-source folder.

For VHAT - the loader currently expects a single XML file.
	1) - VHAT 20130211.xml
	
The file name is ignored - it just needs to be an xml file.
	
