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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.oxford.comlab.Configuration;
import org.oxford.comlab.Configuration.AssertionDuplicationStrategy;
import org.oxford.comlab.Configuration.StoreRewritingsToFile;
import org.oxford.comlab.Configuration.TestSuiteType;
import org.oxford.comlab.aBoxGeneration.algorithmclasses.AlgorithmClass;
import org.oxford.comlab.aBoxGeneration.algorithmclasses.StronglyFaithful;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Term;
import org.oxford.comlab.requiem.rewriter.Variable;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.RDFXMLOntologyFormat;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.model.UnknownOWLOntologyException;

/**
 * This class can be used for the generation of large ABoxes for given ontology schemas (TBoxes) and conjunctive queries. 
 * The ABoxes are stored on disk and are split into several files if they get big.  
 * @author gios
 *
 */
public class LargeABoxGenerator {
	
	/** Total number of ABox assertions allowed per .owl file */
	private static long maxElementsPerFile = 5000;
	
	private String pathToStore;
	private OWLOntology newOntology;
	private OWLOntologyManager manager;
	private ArrayList<String> individuals;
	private OWLDataFactory factory;
	
	private Configuration config;
	private OWLOntology inputOntology;
	
	public LargeABoxGenerator( Configuration configuration ){
		config = configuration;
		if( config == null ){
			config = new Configuration();
			config.testSuiteType = TestSuiteType.STRICT;
			config.assertionDuplicationStrategy = AssertionDuplicationStrategy.TOTAL_NUMBER_OF_ASSERTIONS;
			config.storeRewritingsToFile = StoreRewritingsToFile.FALSE;
			config.totalNumberOfIndividuals = 1000;
		}
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		
		individuals = new ArrayList<String>();
		for( long i=0 ; i<configuration.totalNumberOfIndividuals ; i++ ){
			individuals.add( "a_" + i );
		}
	}
	
	/**
	 * This is the main method of the class that starts the generation of a large ABox for a given ontology (TBox) and conjunctive query. The method uses query rewriting
	 * algorithms to identify "relevant" data patterns, i.e., ABoxes that will provide answers to the query and TBox and then duplicates them in order to generate one large
	 * ABox. These ABoxes are intended for performance analysis. If the ABox becomes large the method will split the ABox into separate files. For technical details about 
	 * the data pattern extraction process the user is referred to the paper:
	 *  
	 * 		G. Stoilos, B Cuenca Grau, and I. Horrocks. How Incomplete is Your Semantic Web Reasoner. In Proc. of AAAI 2010, Atlanta, 2010 
	 *  
	 * @param ontologyFile 			This is the URI of the file that contains the ontology schema (TBox)
	 * @param queryFile 			This is the local path of the file that contains the conjunctive query. Query needs to be in REQUIEM syntax, no SPARQL support
	 * @param terminationThreshold	Is used to give to the generation method a termination criteria. It can either be the maximum number of assertions 
	 * 								in the generated ABox or the maximum number of certain answers that we want the ABox to give for the input ontology
	 * 								and query. Note that the final ABox might diverge a bit from the specified numbers.
	 * @throws Exception
	 */
	public void createSyntheticABox(String inputOntologyFile, String queryFile, long terminationThresholds ) throws Exception {

//		if( terminationThreshold <1 )
//			throw new Exception( "termination criteria should at-least be larger than 1" );
			
		URI physicalURI = URI.create(inputOntologyFile);
        inputOntology = manager.loadOntologyFromPhysicalURI( physicalURI );
		
		/**	Get patterns using REQUIEM query rewriter */
		AlgorithmClass algorithmClass = new StronglyFaithful();
		ArrayList<Clause> ucqRewriting = algorithmClass.buildUCQRewriting( queryFile, inputOntologyFile, config, inputOntology );

		if( ucqRewriting != null ){
			String queryFileName = new File( queryFile ).getName();

			createLargeABox(terminationThresholds, ucqRewriting, queryFileName, inputOntologyFile, algorithmClass);
		}
		else
			throw new Exception( "invalid query" );
	}

	private void createLargeABox(long terminationThreshold, ArrayList<Clause> ucqRewriting, String queryFileName, String inputOntoFileName, AlgorithmClass algorithmClass ) throws OWLOntologyCreationException, OWLOntologyChangeException, UnknownOWLOntologyException, OWLOntologyStorageException {

		URI inputOntoPhysicalURI = URI.create(inputOntoFileName);

		long terminationCounter = 0;
		long fileSplitter = 1;
		Set<OWLAxiom> unsavedAssertions = new HashSet<OWLAxiom>(); 

		boolean constructionFinished = false;
		while( !constructionFinished ){
			for( Clause query : ucqRewriting ){
				if( terminationCounter >= terminationThreshold ){
					constructionFinished = true;
					break;
				}
				Map<Term,String> varMapper = instantiateVariables( query );
				unsavedAssertions.addAll( algorithmClass.instantiateQueryUsingVariableMapping(inputOntology, query, varMapper ) );

				if( config.assertionDuplicationStrategy == AssertionDuplicationStrategy.TOTAL_NUMBER_OF_ASSERTIONS )
					terminationCounter = terminationCounter + (unsavedAssertions.size()-terminationCounter);
				else if( config.assertionDuplicationStrategy == AssertionDuplicationStrategy.TOTAL_NUMBER_OF_ANSWERS )
					terminationCounter++;

				//comment out fileSplitting if you want to build only a signle .owl file containing the ABox
				if( (terminationCounter/fileSplitter) >= maxElementsPerFile ){
					System.out.println( "spliting ABox" );
					saveDataset( inputOntoFileName, inputOntology.getURI(), terminationThreshold, fileSplitter, queryFileName, inputOntoPhysicalURI, unsavedAssertions );
					manager = OWLManager.createOWLOntologyManager();		
					unsavedAssertions.clear();
					fileSplitter++;
				}
			}
		}
		saveDataset( inputOntoFileName, inputOntology.getURI(), terminationThreshold, fileSplitter, queryFileName, inputOntoPhysicalURI, unsavedAssertions );
	}
	private Map<Term, String> instantiateVariables(Clause query) {
		ArrayList<Variable> queryVars = query.getVariables();
		Map<Term,String> varMapper = new HashMap<Term,String>();
		
		Random ran = new java.util.Random();
		for( int j=0 ; j<queryVars.size() ; j++ ){
			varMapper.put( queryVars.get( j ), individuals.get( ran.nextInt(individuals.size()) ) );
		}
		return varMapper;
	}
	public void saveDataset(String ontologyFile, URI inputOntoURI, long threshold, long fileSplitter, String queryFileName, URI physicalURI, Set<OWLAxiom> unsavedAssertions) throws UnknownOWLOntologyException, OWLOntologyStorageException, OWLOntologyCreationException, OWLOntologyChangeException{
		pathToStore = ontologyFile.replace(".owl", "") + "_ABOXES-Performance/" + threshold + "/ABox_" + fileSplitter + "_For_" + queryFileName.replace(".txt", "") + ".owl";
		newOntology = manager.createOntology( URI.create( inputOntoURI + "_ABox_" + fileSplitter + "_" + queryFileName.replace(".txt", "") ) );
		manager.addAxiom( newOntology, factory.getOWLImportsDeclarationAxiom(newOntology, physicalURI));
		manager.addAxioms( newOntology, unsavedAssertions );
		URI physicalURIOfOutputOntology = URI.create( pathToStore );

		try{
			manager.saveOntology( newOntology, new RDFXMLOntologyFormat(), physicalURIOfOutputOntology );
		}catch( OWLOntologyStorageException exc ){
			if ( exc.getMessage().contains( "File name too long)" ) )
				System.out.println( "could not create ABox " + physicalURIOfOutputOntology + ". File name too long...But continuing");
			else
				throw exc;
		}
	}
}
