package org.oxford.comlab.SyGENiA.examples;
import java.io.IOException;

import org.oxford.comlab.querygenerator.QTBGenerator;
import org.semanticweb.owl.model.OWLOntologyCreationException;


public class ExampleQueryGeneration {

	public static String personalisedPath = "/media/SHARE/JustificationTests/";
	
	public static void main( String[] args ){
		
		String workingDir = System.getProperty( "user.dir" );
		
		try {
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/univ-bench.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/univ-bench_DLLite.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/V.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/AX-acyclic.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/A-acyclic1.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/S-noaddress.owl");			
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/U.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/UX.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/P5X.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/P5.owl");
			new QTBGenerator().generateQTB("file:" + workingDir + "/examples/ontologies/QueryGeneration/P1.owl");
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
