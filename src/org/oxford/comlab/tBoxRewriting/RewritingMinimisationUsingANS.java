/* Copyright 2011 by the Oxford University Computing Laboratory

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

package org.oxford.comlab.tBoxRewriting;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.oxford.comlab.compass.SystemInterface;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.RDFXMLOntologyFormat;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectPropertyInverse;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
import org.semanticweb.owl.model.OWLObjectSubPropertyAxiom;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.model.OWLSubClassAxiom;

/**
 * 
 * @author gios
 *
 * This class was used for the experimental evaluation in the paper: 
 * 
 *  Giorgos Stoilos, Bernardo Cuenca Grau, Boris Motis, and Ian Horrocks. Repairing Ontologies for Incomplete Reasoners. Submitted to ISWC 2011. 
 */
public class RewritingMinimisationUsingANS {

	public static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	public static OWLDataFactory factory = manager.getOWLDataFactory();
	
	private long sequenceGenerator;
	
	public void minimiseRLRewriting(String originalOntologyFile, String rlRewritingFile, SystemInterface cqSystem) throws Exception {
		
		long timeStart = System.currentTimeMillis();
		
		cqSystem.initializeSystem();
		
		URI physicalURIOfOriginalOnto = URI.create( "file:" + originalOntologyFile );
		OWLOntology originalOnto = manager.loadOntologyFromPhysicalURI( physicalURIOfOriginalOnto );
		
		URI physicalURIOfRLRewriting = URI.create( rlRewritingFile );
		OWLOntology rlRewritingOntology = manager.loadOntologyFromPhysicalURI( physicalURIOfRLRewriting ); 
		Set<OWLAxiom> minimisedAxioms = new HashSet<OWLAxiom>();
		System.out.println("Reducing "+rlRewritingOntology.getAxiomCount()+" axioms by using ans... ");
		int processedAxioms = 0;
		for( OWLAxiom owlAx : rlRewritingOntology.getAxioms() ) {
			processedAxioms++;
			printProgressOfWork( processedAxioms, rlRewritingOntology.getAxioms().size() );
			if( owlAx.toString().contains( "Tr_" ) )
				continue;
			Set<OWLAxiom> tempABox = new HashSet<OWLAxiom>( );
			sequenceGenerator = 0;
			if( owlAx instanceof OWLSubClassAxiom ) {
				tempABox.addAll( unfoldPatternIntoAssertions( ((OWLSubClassAxiom)owlAx).getSubClass(), factory.getOWLIndividual( URI.create( "http://dymmy_" + ++sequenceGenerator ) ) ) );
				cqSystem.loadQuery( ((OWLSubClassAxiom)owlAx).getSuperClass().asOWLClass().getURI().toString() , 1 );
			}
			else if ( owlAx instanceof OWLObjectSubPropertyAxiom ) {
				OWLObjectSubPropertyAxiom ria = (OWLObjectSubPropertyAxiom)owlAx;
				OWLObjectPropertyExpression subProperty = ria.getSubProperty();
				OWLObjectPropertyExpression superProperty = ria.getSuperProperty();
				tempABox.add( factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLIndividual( URI.create( "http://dymmy_" + ++sequenceGenerator ) ), subProperty, 
																					factory.getOWLIndividual( URI.create( "http://dymmy_" + ++sequenceGenerator ) )));
				cqSystem.loadQuery( superProperty.asOWLObjectProperty().getURI().toString() , 2 );
			}
			else if ( owlAx instanceof OWLDeclarationAxiom )
				continue;
			String tempOntologyFileForABox = saveOntology( originalOnto, physicalURIOfRLRewriting, tempABox, "_temp-ABox" );
			tempOntologyFileForABox = tempOntologyFileForABox.replace( "file:/", "/" );
			File tempOntologyFileForABoxFile = new File( tempOntologyFileForABox );
			
			cqSystem.loadTestToSystem( originalOntologyFile, tempOntologyFileForABoxFile );
			
			long certainAnswers = cqSystem.runLoadedQuery( );
        	if( certainAnswers < 1 )
        		minimisedAxioms.add(owlAx);
        	else 
        		System.out.println( "Axiom " + owlAx + " is redundant" );
        	cqSystem.clearRepository();
		}
		cqSystem.clearRepository();
		System.out.println( "Starting intra-axiom redundancy elimination");
		Set<OWLAxiom> moreMinimised = new HashSet<OWLAxiom>(minimisedAxioms);
		Set<OWLAxiom> redundantAxioms = new HashSet<OWLAxiom>();
		processedAxioms = 0;
		int allAxioms = minimisedAxioms.size();
		int totalUnitsOfWork = allAxioms*allAxioms;
		for( OWLAxiom axiom1 : minimisedAxioms ) {
//			System.out.println( "axiom1 :" + axiom1 );
			if( redundantAxioms.contains(axiom1) ) {
				processedAxioms+=allAxioms;
                printProgressOfWork(processedAxioms, totalUnitsOfWork);
				continue;
			}
			printProgressOfWork(processedAxioms, totalUnitsOfWork);
			Set<OWLAxiom> extendedTBox = new HashSet<OWLAxiom>( originalOnto.getAxioms() );
			extendedTBox.add( axiom1 );
			
			String tempOntologyFileForExtendedTBox = saveOntology( originalOnto, physicalURIOfOriginalOnto, extendedTBox, "_temp-TBox"  );
			tempOntologyFileForExtendedTBox = tempOntologyFileForExtendedTBox.replace( "file:/", "/" );
			for( OWLAxiom axiom2 : minimisedAxioms ) {
//				System.out.println( "axiom2 :" + axiom2 );
				processedAxioms++;
				if( axiom1.equals(axiom2) || redundantAxioms.contains(axiom2) )
					continue;
				if( axiom1 instanceof OWLSubClassAxiom && axiom2 instanceof OWLSubClassAxiom ) {
					
					Set<OWLAxiom> tempABox = new HashSet<OWLAxiom>( );
					sequenceGenerator = 0;
					
					tempABox.addAll( unfoldPatternIntoAssertions( ((OWLSubClassAxiom)axiom2).getSubClass(), factory.getOWLIndividual( URI.create( "http://dymmy_" + ++sequenceGenerator ) ) ) );
					String tempOntologyFileForABox = saveOntology( originalOnto, physicalURIOfRLRewriting, tempABox, "_temp-ABox" );
					tempOntologyFileForABox = tempOntologyFileForABox.replace( "file:/", "/" );
//					System.out.println(tempABox);
					File tempOntologyFileForABoxFile = new File( tempOntologyFileForABox );

					cqSystem.loadQuery( ((OWLSubClassAxiom)axiom2).getSuperClass().asOWLClass().getURI().toString(), 1 );
					cqSystem.loadTestToSystem( tempOntologyFileForExtendedTBox, tempOntologyFileForABoxFile );

					long certainAnswers = cqSystem.runLoadedQuery( );
		        	if( certainAnswers >= 1 ) {
		        		System.out.println( "Axiom " + axiom2 + " is redundant due to " + axiom1 );
		        		moreMinimised.remove(axiom2);
		        		redundantAxioms.add(axiom2);
		        	}
		        	cqSystem.clearRepository();
				}
			}
		}
		
		OWLOntologyManager targetOntologyManager=OWLManager.createOWLOntologyManager();
        OWLOntology targetOntology=targetOntologyManager.createOntology(originalOnto.getURI());
        targetOntologyManager.addAxioms(targetOntology, originalOnto.getAxioms());
        targetOntologyManager.addAxioms(targetOntology,moreMinimised);
        
		String pathToStoreOriginalPlusMinimised = physicalURIOfOriginalOnto.toString().replace(".owl", "") + "_repaired" + ".owl";
        
        targetOntologyManager.saveOntology(targetOntology,URI.create(pathToStoreOriginalPlusMinimised));
		
        System.out.println( "Minimised rewriting contains " + moreMinimised.size() + " axioms");        
        System.out.println("Final minimisation completed in " + (System.currentTimeMillis()-timeStart) + " ms");
        
        for( OWLAxiom ax : moreMinimised )
        	System.out.println( ax );
	}
	
	private Set<OWLAxiom> unfoldPatternIntoAssertions( OWLDescription subClass, OWLIndividual owlNamedIndividual  ) {
		Set<OWLAxiom> classesAxioms = new HashSet<OWLAxiom>();
		if ( subClass instanceof OWLClass )
			classesAxioms.add( factory.getOWLClassAssertionAxiom( owlNamedIndividual, (OWLClass)subClass ));
		else if ( subClass instanceof OWLObjectSomeRestriction ){
			OWLObjectSomeRestriction someValuesFrom = (OWLObjectSomeRestriction) subClass;
			OWLObjectPropertyExpression objectProperty = someValuesFrom.getProperty();
			OWLDescription conceptInRestriction = someValuesFrom.getFiller();
			OWLIndividual successorIndv = factory.getOWLIndividual( URI.create( "http://dymmy_" + ++sequenceGenerator ) );
			if( objectProperty instanceof OWLObjectProperty )
				classesAxioms.add( factory.getOWLObjectPropertyAssertionAxiom( owlNamedIndividual, objectProperty, successorIndv) );
			else if( objectProperty instanceof OWLObjectPropertyInverse )
				classesAxioms.add( factory.getOWLObjectPropertyAssertionAxiom( successorIndv, objectProperty, owlNamedIndividual ) );
			else{
				System.out.println( "A strange case happened. It was " + subClass );
				System.exit( 0 );
			}
			classesAxioms.addAll( unfoldPatternIntoAssertions( conceptInRestriction, successorIndv ) );
		}
		else if ( subClass instanceof OWLObjectIntersectionOf ){
			OWLObjectIntersectionOf subClassList = (OWLObjectIntersectionOf) subClass;
			for( OWLDescription conjOperand : subClassList.getOperands() )
				classesAxioms.addAll( unfoldPatternIntoAssertions( conjOperand, owlNamedIndividual ) );
		}
		else{
			System.out.println( "A strange case happened. It was " + subClass );
			System.exit( 0 );
		}
		return classesAxioms;
	}
	
	private String saveOntology(OWLOntology tBox, URI physicalURIOfBaseOntology, Set<OWLAxiom> newOntologyAxioms, String nameQuantificationForNewOntology) throws OWLOntologyCreationException, OWLOntologyChangeException, OWLOntologyStorageException {
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();

		String pathToStoreNewOntology = physicalURIOfBaseOntology.toString().replace(".owl", "") + nameQuantificationForNewOntology + ".owl";
		
		URI physicalURIOfExtendedOntology = URI.create( pathToStoreNewOntology );
		OWLOntology extendedOntology = manager.createOntology( URI.create( tBox.getURI() + nameQuantificationForNewOntology ) );

//		manager.addAxiom( extendedOntology, factory.getOWLImportsDeclarationAxiom(extendedOntology, physicalURIOfBaseOntology));
//		manager.addAxioms( extendedOntology, tBox.getAxioms() );
		manager.addAxioms( extendedOntology, newOntologyAxioms );
		
		manager.saveOntology( extendedOntology, new RDFXMLOntologyFormat(), physicalURIOfExtendedOntology );
		
		return pathToStoreNewOntology;
	}
	private static void printProgressOfWork(int processedAxioms, int totalUnitsOfWork) {
        float percentace = processedAxioms; 
        percentace /= totalUnitsOfWork;
        System.out.println( Math.ceil( percentace*100 ) + " %,  ");
	}
}
