/* Copyright 2010 by the Oxford University Computing Laboratory

   This file is part of SyGENiA.

   SyGENiA is free software: you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   SyGENiA is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License
   along with SyGENiA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.oxford.comlab.aBoxGeneration;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Set;

import org.oxford.comlab.Configuration;
import org.oxford.comlab.Configuration.QAAlgorithmClass;
import org.oxford.comlab.Configuration.StoreRewritingsToFile;
import org.oxford.comlab.Configuration.TestSuiteType;
import org.oxford.comlab.aBoxGeneration.algorithmclasses.AlgorithmClass;
import org.oxford.comlab.aBoxGeneration.algorithmclasses.FOReproducible;
import org.oxford.comlab.aBoxGeneration.algorithmclasses.StronglyFaithful;
import org.oxford.comlab.aBoxGeneration.algorithmclasses.WeaklyFaithful;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.RDFXMLOntologyFormat;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;

/**
 * This class can be used for the generation of testing bases for given ontology schemas (TBoxes) and conjunctive queries. 
 * The testing bases are stored on disk in a specific folder structure.
 * @author gios
 *
 */
public class TestSuiteGenerator {

	private Configuration config;
	private OWLOntology inputOntology;
//	private UCQGenerationManager ucqGenerator;
	
	private long largestTUAssertions;
	private long smallestTUAssertions;
	private long sizeOfTB;
	
	public TestSuiteGenerator( Configuration configuration ){
		config = configuration;
		if( config == null ){
			config = new Configuration();
			config.testSuiteType = TestSuiteType.STRICT;
			config.storeRewritingsToFile = StoreRewritingsToFile.FALSE;
		}
//		ucqGenerator = new UCQGenerationManager();
	}

	public void createTestSuite(String ontologyFile, String queryFile) throws Exception {

		long time = System.currentTimeMillis();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		URI physicalURIOfBaseOntology = URI.create( ontologyFile );
        inputOntology = manager.loadOntologyFromPhysicalURI( physicalURIOfBaseOntology );
        sizeOfTB = 0;
        largestTUAssertions = 0;
        smallestTUAssertions = 100;
        
        //this is for unfolding the Datalog program up to some depth. This is still on-going and should be not changed from 2
//		int terminationCondition = 2;

//		ArrayList<Clause> ucqRewriting = ucqGenerator.buildUCQRewriting( queryFile, ontologyFile, terminationCondition, config, inputOntology );
		AlgorithmClass algorithmClass;
		System.out.println( "Starting query rewriting algorithm" );
		if( config.qaAlgorithmClass == QAAlgorithmClass.weakly_faithful )
			algorithmClass = new WeaklyFaithful();
		else if ( config.qaAlgorithmClass == QAAlgorithmClass.strongly_faithful )
			algorithmClass = new StronglyFaithful();
		else if ( config.qaAlgorithmClass == QAAlgorithmClass.firstOrder_reproducible )
			algorithmClass = new FOReproducible();
		else 
			throw new Exception("Algorithm class could not be recognised. Use one of WELL_BEHAVED, MuINVARIANT or ABoxSATURATION");

		ArrayList<Clause> ucqRewriting = algorithmClass.buildUCQRewriting( queryFile, ontologyFile, config, inputOntology );
		if( ucqRewriting != null ){
			String queryFileName = new File( queryFile ).getName();
			System.out.println( "\nStarting ABox Generation" );
			for( Clause query : ucqRewriting ){
				Set<ABoxInstantiation> testingUnitAssertions = algorithmClass.instantiateQuery(query, inputOntology);

				String folderStoringPattern = "_TB-"+ algorithmClass.getName() + "/" + queryFileName.replace(".txt", "") + "/";

				storeTestingUnitsToDisk( query, folderStoringPattern, physicalURIOfBaseOntology, testingUnitAssertions );
			}
		}
		else
			throw new Exception( "invalid query" );
		System.out.println( "\nTest Suite generated in:\t" + (System.currentTimeMillis()-time) + " ms");
		System.out.println( "TB for " + config.qaAlgorithmClass + " algorithms contains:\t" + sizeOfTB + " TUs" );
		System.out.println( "min-TU:  " + smallestTUAssertions + " assertions, min-TU: " + largestTUAssertions + " assertions\n\n");
	}
	
	private void storeTestingUnitsToDisk(Clause query, String folderStoringPattern, URI physicalURIOfBaseOntology, Set<ABoxInstantiation> createdTestingUnits) throws Exception {
		String cannonicalPatternName = createCannonicalPatternName( query.toString() );
		for( ABoxInstantiation testingUnit :  createdTestingUnits ){
			if( testingUnit.getABoxAssertions().size() > largestTUAssertions )
				largestTUAssertions = testingUnit.getABoxAssertions().size();
			if( testingUnit.getABoxAssertions().size() < smallestTUAssertions )
				smallestTUAssertions = testingUnit.getABoxAssertions().size();

			String patternWithVarMapping = testingUnit.getDistinguishedVariableMapping() + "_D_" + testingUnit.getNonDistinguishedVariableMapping();
			String pattern = cannonicalPatternName + "/Pattern_" + patternWithVarMapping;
			String strippedName = folderStoringPattern + pattern;
			String pathToStoreTestingUnit = physicalURIOfBaseOntology.toString().replace(".owl", "") + strippedName + ".owl";
			storeTestingUnit(pathToStoreTestingUnit, physicalURIOfBaseOntology, testingUnit.getABoxAssertions(), strippedName);
		}
	}
	
	private void storeTestingUnit(String pathToStoreTestingUnit, URI physicalURIOfBaseOntology, Set<OWLAxiom> testingUnitAssertions, String strippedName) throws Exception {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		
		URI physicalURIOfTestingUnit = URI.create( pathToStoreTestingUnit );
		OWLOntology testingUnitOntology = manager.createOntology( URI.create( inputOntology.getURI() + strippedName ) );

		manager.addAxiom( testingUnitOntology, factory.getOWLImportsDeclarationAxiom(testingUnitOntology, physicalURIOfBaseOntology));
		manager.addAxioms( testingUnitOntology, testingUnitAssertions );
		try{
			manager.saveOntology( testingUnitOntology, new RDFXMLOntologyFormat(), physicalURIOfTestingUnit );
			sizeOfTB++;
		}catch( OWLOntologyStorageException exc ){
			if ( exc.getMessage().contains( "File name too long)" ) )
				System.out.println( "could not create testing unit for " + strippedName + ". File name too long...Continuing with rest of testing units");
			else
				throw exc;
		}
	}

	public static String createCannonicalPatternName(String querystr) {
		String patternName = querystr.trim();
		patternName = patternName.replaceAll( " ", "");
		patternName = patternName.replaceAll( ",", "");
		patternName = patternName.replaceAll( "<-", "-");
		patternName = patternName.replaceAll( "$", "");
		char[] charStr = patternName.toCharArray();
		for( int i=0 ; i<charStr.length ; i++ ){
			if( charStr[i] == '^' )
				charStr[i] = ',';
			else if( charStr[i] == '?' )
				charStr[i] = 'X';
		}
		return new String( charStr ); 
	}
}