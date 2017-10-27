/* Copyright 2010-2011 by the Oxford University Computing Laboratory

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

package org.oxford.comlab.querygenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

import org.oxford.comlab.datastructures.LabeledGraph;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLLogicalAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;

public class QTBGenerator {
	
	private static OWLDataFactory factory;
	
	public void generateQTB( String ontologyFile ) throws OWLOntologyCreationException, IOException{
		long currentTime = System.currentTimeMillis();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		
		URI physicalURIOfBaseOntology = URI.create( ontologyFile );
        OWLOntology inputOntology = manager.loadOntologyFromPhysicalURI( physicalURIOfBaseOntology );
        String qtbFolder = ontologyFile.replaceAll(".owl", "") + "_QTB/";
        
        //Normalise the axioms a bit
        SubClassNormaliser subClassNormaliser = new SubClassNormaliser(factory);
		for ( OWLLogicalAxiom ontAxiom : inputOntology.getLogicalAxioms() )
			ontAxiom.accept(subClassNormaliser);
		        
        ChaseGenerator chaseGenerator = new ChaseGenerator(factory);
        LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression> chase = chaseGenerator.constructChase( subClassNormaliser.getSubClassAxioms(), subClassNormaliser.getSubPropertyAxioms() );
        Set<OWLIndividual> rootIndividuals = chaseGenerator.getRootIndividuals();
//        chaseGenerator.printGraph(chase);
        long qtbSize = extractAndStoreAllChaseQueries( chase, rootIndividuals, qtbFolder );
        
        System.out.println( "QTB generated in " + (System.currentTimeMillis()-currentTime) + "ms and contains " + qtbSize + " queries\n" );
	}

	private long extractAndStoreAllChaseQueries(LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression> chase, Set<OWLIndividual> rootIndividuals, String qtbFolder) throws IOException {
		Set<String> conjunctiveQueries = new TreeSet<String>();
		for ( OWLIndividual rootIndv : rootIndividuals ) {
			long variableCounter = 0;
			String currentCQString = "Q(?0)<-";
			generateQueriesForCurrentIndividual(currentCQString, rootIndv, chase, variableCounter, conjunctiveQueries, rootIndividuals);
		}
		File outputFile = new File( qtbFolder.replace("file:", "") );
		System.out.println("Saving QTB at: " + qtbFolder );
		outputFile.mkdir();
		long sequenceGenerator = 0;
		for ( String cq : conjunctiveQueries ) {
			String cqFilePath;
			if ( sequenceGenerator<10 )
				cqFilePath = "0" + sequenceGenerator++ + "_" + createCannonicalPatternName( cq );
			else 
				cqFilePath = sequenceGenerator++ + "_" + createCannonicalPatternName( cq );
			outputFile = new File(qtbFolder.replace("file:", "") + cqFilePath + ".txt");
			FileWriter out = new FileWriter(outputFile);
			out.write( cq );
			out.close();
		}
		return sequenceGenerator;
	}

	private void generateQueriesForCurrentIndividual(String currentCQString, OWLIndividual currentIndv, LabeledGraph<OWLIndividual, OWLClass, OWLObjectPropertyExpression> chase, long variableCounter, Set<String> conjunctiveQueries, Set<OWLIndividual> rootIndividuals) {
		Set<OWLClass> nodeAtomicConcepts = chase.getLabelsOfNode( currentIndv );
		if ( nodeAtomicConcepts != null )
			for ( OWLClass nodeLabel : nodeAtomicConcepts )
				if ( nodeLabel!=null && !nodeLabel.isOWLThing() && !currentIndv.equals( factory.getOWLIndividual(URI.create("http://a_"+nodeLabel.getURI().getFragment())) ) )
					conjunctiveQueries.add( currentCQString + nodeLabel.getURI().getFragment() + "(?" + variableCounter + ")" );
	
		for ( LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression>.Edge roleEdge : chase.getSuccessors( currentIndv ) ) {
			OWLObjectPropertyExpression owlObjProperty = roleEdge.getEdgeLabel();
			if ( !roleEdge.getToElement().equals( factory.getOWLIndividual(URI.create("http://d_"+owlObjProperty))) ) {
				if ( !owlObjProperty.isAnonymous() ) {
					String newCQString = currentCQString + roleEdge.getEdgeLabel() + "(?" + variableCounter + ",?" + (variableCounter+1) + ")";
					conjunctiveQueries.add( newCQString );
					generateQueriesForCurrentIndividual( newCQString + ", ", roleEdge.getToElement(), chase, variableCounter+1, conjunctiveQueries, rootIndividuals);
				}
				else {
					String newCQString = currentCQString + roleEdge.getEdgeLabel().getInverseProperty().getSimplified() + "(?" + (variableCounter+1) + ",?" + variableCounter + ")";
					conjunctiveQueries.add( newCQString );
					generateQueriesForCurrentIndividual( newCQString + ", ", roleEdge.getToElement(), chase, variableCounter+1, conjunctiveQueries, rootIndividuals);
				}
			}
			if (rootIndividuals.contains(roleEdge.getToElement())&&(!currentIndv.equals(factory.getOWLIndividual(URI.create("http://e_"+owlObjProperty))))&&!currentIndv.equals(factory.getOWLIndividual(URI.create("http://c_"+owlObjProperty)))) {
				String dCQstring = "Q(?0,?1)<-";
				if (!owlObjProperty.isAnonymous())
					conjunctiveQueries.add(dCQstring + roleEdge.getEdgeLabel() + "(?0,?1)" );
				else
					conjunctiveQueries.add(dCQstring + roleEdge.getEdgeLabel().getInverseProperty().getSimplified() + "(?1,?0)" );
			}
		}
	}
	public static String createCannonicalPatternName(String querystr) {
		String patternName = querystr.trim();
		patternName = patternName.replaceAll( " ", "");
		patternName = patternName.replaceAll( ",", "");
		patternName = patternName.replaceAll( "<-", "-");
		patternName = patternName.replaceAll( "$", "");
		char[] charStr = patternName.toCharArray();
		for ( int i=0 ; i<charStr.length ; i++ ) {
			if ( charStr[i] == '^' )
				charStr[i] = ',';
			else if ( charStr[i] == '?' )
				charStr[i] = 'X';
		}
		return new String( charStr ); 
	}
}