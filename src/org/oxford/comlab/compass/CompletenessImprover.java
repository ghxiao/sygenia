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

package org.oxford.comlab.compass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;

import org.oxford.comlab.datastructures.LabeledGraph;
import org.oxford.comlab.requiem.parser.ELHIOParser;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Term;
import org.oxford.comlab.requiem.rewriter.Variable;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.Reasoner.Configuration;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.RDFXMLOntologyFormat;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectPropertyInverse;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
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
 * This class was used in the experimental evaluation of the following paper:
 * 
 * Giorgos Stoilos and Bernardo Cuenca Grau. Repairing Incomplete Reasoners. In Poc. of the 24th International Workshop on Description Logics (DL 2011).
 * 
 * This method would probably soon be deprecated by new Repairing code!!
 * 
 */
public class CompletenessImprover {

	public static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	public static OWLDataFactory factory = manager.getOWLDataFactory();

	private long sequenceGenerator;

//	public static void main(String[] args){
//		CompletenessImprover complImprover = new CompletenessImprover();
//		
//		String ontologyFile = "file:/media/SHARE/Galen/moduleU_JIA_procedures.owl";
//		String failedPatternsFolder = "/media/SHARE/JustificationTests/RealOntologies/EvaluationResults/CompletenessResults-Per_System/OWLim/Injective";
//		String queriesFolder = "/media/SHARE/Galen/Queries";
//		complImprover.improveSystemCompleteness( null, ontologyFile, queriesFolder, failedPatternsFolder);
//	}

	public void improveSystemCompleteness( SystemInterface cqSystem, String ontologyFile, String queriesFolder, String failedPatternsFolder ){
		
		try {
			long start = System.currentTimeMillis();
			URI physicalURIOfBaseOntology = URI.create( ontologyFile );
			OWLOntology tBox = manager.loadOntologyFromPhysicalURI( physicalURIOfBaseOntology );
			
			Set<OWLAxiom> patternAxioms = new HashSet<OWLAxiom>();
	
			for( int queryIndex = 1 ; queryIndex <= 4 ; queryIndex++ ){

				System.out.println( "Query " + queryIndex );
				String queryFile = queriesFolder + "/Q0" + queryIndex + ".txt";
			
				String failedInferencePatternsForQuery = failedPatternsFolder + "/Failed_Query_Q" + queryIndex;
				Clause originalQuery = ELHIOParser.getQueryFromFile( queryFile );
				if( originalQuery == null )
					System.exit( 0 );

				Set<OWLAxiom> completenessRepairAxioms = computeCompletenessRepairs( originalQuery, tBox, failedInferencePatternsForQuery );
				System.out.println( completenessRepairAxioms.size() + " axioms generated for query " + originalQuery );

				completenessRepairAxioms = keepAccurateRepairs( cqSystem, tBox, physicalURIOfBaseOntology, completenessRepairAxioms, queryIndex );
				System.out.println( completenessRepairAxioms.size() + " left after pruning " );
				patternAxioms.addAll( completenessRepairAxioms );
				
			}
			System.out.println( patternAxioms.size() + " new axioms in total");
			patternAxioms.addAll( tBox.getAxioms() );
			saveOntology(tBox, physicalURIOfBaseOntology, patternAxioms, "_extended" );
			
			System.out.println( (System.currentTimeMillis()-start) );

			cqSystem.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Set<OWLAxiom> computeCompletenessRepairs( Clause originalQuery, OWLOntology tBox, String failedInferencePatternFile ) throws IOException, OWLException {

		Set<String> allFailedInferencePatterns = new HashSet<String>();
		FileInputStream fis = new FileInputStream( failedInferencePatternFile );
		BufferedReader br = new BufferedReader(new InputStreamReader( fis ));
		String line = null;
		while( (line = br.readLine()) != null )
			if( line.contains( "Failed at testing unit: Pattern_" ) ){
				StringTokenizer strTok = new StringTokenizer( line , "_" );
				strTok.nextToken();
				allFailedInferencePatterns.add( strTok.nextToken() );
			}
		
		Set<OWLAxiom> completenessRepairAxioms = new HashSet<OWLAxiom>();
		for( String failedInferencePattern : allFailedInferencePatterns ){
//			Build complex concepts from inference pattern by using a form of rolling-up
			Map<Variable,OWLDescription> varsToRolledUpConcepts = rollUpFailedInferencePattern( tBox, failedInferencePattern );
			if( varsToRolledUpConcepts == null )
				System.exit( 0 );
			completenessRepairAxioms.addAll( createTBoxAxiomsForPatterns( originalQuery, tBox, varsToRolledUpConcepts ) );
		}
		return completenessRepairAxioms;
	}
	
	private String saveOntology(OWLOntology tBox, URI physicalURIOfBaseOntology, Set<OWLAxiom> newOntologyAxioms, String nameQuantificationForNewOntology) throws OWLOntologyCreationException, OWLOntologyChangeException, OWLOntologyStorageException {
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();

		String pathToStoreNewOntology = physicalURIOfBaseOntology.toString().replace(".owl", "") + nameQuantificationForNewOntology + ".owl";
		
		URI physicalURIOfExtendedOntology = URI.create( pathToStoreNewOntology );
		OWLOntology extendedOntology = manager.createOntology( URI.create( tBox.getURI() + nameQuantificationForNewOntology ) );

		manager.addAxioms( extendedOntology, newOntologyAxioms );
		manager.saveOntology( extendedOntology, new RDFXMLOntologyFormat(), physicalURIOfExtendedOntology );
		
		return pathToStoreNewOntology;
	}

	private Set<OWLAxiom> createTBoxAxiomsForPatterns(Clause originalQuery, OWLOntology tBox, Map<Variable, OWLDescription> varsToRolledUpConcepts) throws OWLOntologyCreationException, OWLOntologyChangeException {
		Set<OWLAxiom> patternAxioms = new HashSet<OWLAxiom>();
		Term[] bodyTerms = originalQuery.getBody();
		for( int i=0 ; i< bodyTerms.length ; i++ ){
			Term[] variablesOfTerm = bodyTerms[i].getArguments();
			if( variablesOfTerm.length == 1 ) {
				OWLDescription rolledUpConcept = varsToRolledUpConcepts.get( variablesOfTerm[0] );
				OWLSubClassAxiom repair = factory.getOWLSubClassAxiom( rolledUpConcept, getOWLDescriptionForQueryAtom(tBox, bodyTerms[i].getName()) );
				patternAxioms.add( repair );
				if( factory.getOWLThing().equals( rolledUpConcept ) || (!rolledUpConcept.isAnonymous() && rolledUpConcept.asOWLClass().getURI().getFragment().equals( bodyTerms[i].getName())) )
					patternAxioms.remove( repair );
			}
			else
				continue;
		}
		return patternAxioms;
	}

	private Map<Variable,OWLDescription> rollUpFailedInferencePattern( OWLOntology tBox, String failedInferencePattern ){
		
		failedInferencePattern = failedInferencePattern.replaceAll( "X0X", "X0,X" );
		failedInferencePattern = failedInferencePattern.replaceAll( "X1X", "X1,X" );
		failedInferencePattern = failedInferencePattern.replaceAll( "X2X", "X2,X" );
		failedInferencePattern = failedInferencePattern.replaceAll( "X3X", "X3,X" );
		failedInferencePattern = failedInferencePattern.replaceAll( "X4X", "X4,X" );
		failedInferencePattern = failedInferencePattern.replaceAll( "X5X", "X5,X" );
		failedInferencePattern = failedInferencePattern.replaceAll( "X6X", "X6,X" );

		failedInferencePattern = failedInferencePattern.replaceAll( "\\)", "\\)," );
		failedInferencePattern = failedInferencePattern.replaceAll( ",-", "<-" );
		failedInferencePattern = failedInferencePattern.replaceAll( "\\(X", "\\(?" );
		failedInferencePattern = failedInferencePattern.replaceAll( ",X", ",?" );
		failedInferencePattern = failedInferencePattern.replaceAll( "\\),\\?", "\\),X" );
		failedInferencePattern = failedInferencePattern.replaceAll( ".owl", "" );
		failedInferencePattern = failedInferencePattern.substring(0, failedInferencePattern.length()-1);

		Clause failedPatternAsQueryClause = ELHIOParser.parseQuery( failedInferencePattern ); 
		if( failedPatternAsQueryClause == null ){
			System.out.println( "Problem parsing failed inference pattern: " + failedInferencePattern );
			return null;
		}

//		Map<Variable,Set<OWLDescription>> varsToAtomicConcepts = new HashMap<Variable,Set<OWLDescription>>();
		Term[] bodyTerms = failedPatternAsQueryClause.getBody();
		LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression> queryGraph = new LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression>();
		for( int i=0 ; i < bodyTerms.length ; i++ ){

			Term[] variablesOfTerm = bodyTerms[i].getArguments();
			if( variablesOfTerm.length == 2 ){
				queryGraph.addEdge( (Variable)variablesOfTerm[0], (Variable)variablesOfTerm[1], getOWLObjectPropertyForQueryAtom(tBox, bodyTerms[i].getName() ) );
			}
			else{
				Variable queryVar = (Variable)variablesOfTerm[0];
				queryGraph.addLabel(queryVar, getOWLDescriptionForQueryAtom(tBox, bodyTerms[i].getName()));
			}
		}
		return buildRolledUpConceptsFromQueryGraph( queryGraph, failedPatternAsQueryClause );
	}

	private Map<Variable, OWLDescription> buildRolledUpConceptsFromQueryGraph(LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression> queryGraph, Clause failedPatternAsQueryClause) {

//		if( queryGraph.hasCycles( ) )
//			return null;
		Map<Variable,OWLDescription> varsToRolledUpConcepts = new HashMap<Variable,OWLDescription>();
		Queue<Variable> toVisit = new LinkedList<Variable>();
        toVisit.addAll( failedPatternAsQueryClause.getDistinguishedVariables() );
        Set<Variable> visitedNodes = new HashSet<Variable>();
        while( !toVisit.isEmpty() ){
        	Variable currentVar = toVisit.poll();
        	visitedNodes.add( currentVar );
        	OWLDescription complexConcept = buildDescriptionForNode( currentVar, queryGraph, failedPatternAsQueryClause, visitedNodes );
        	varsToRolledUpConcepts.put( currentVar, complexConcept );
        }
        return varsToRolledUpConcepts;
	}

	private OWLDescription buildDescriptionForNode(Variable currentVar, LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression> queryGraph,Clause parsedQuery, Set<Variable> visitedNodes) {
		Set<OWLDescription> allDescriptions = new HashSet<OWLDescription>();
		visitedNodes.add( currentVar );
		for( LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression>.Edge outgoingEdges : queryGraph.getSuccessors( currentVar )){
			if( !parsedQuery.getDistinguishedVariables().contains( outgoingEdges.getToElement() ) && !visitedNodes.contains( outgoingEdges.getToElement() ) && isEdgeUniqueWithSpecificSuccessor( currentVar, outgoingEdges.getEdgeLabel(), outgoingEdges.getToElement(), queryGraph.getSuccessors( currentVar ) ) )
				allDescriptions.add( factory.getOWLObjectSomeRestriction( outgoingEdges.getEdgeLabel(), buildDescriptionForNode( outgoingEdges.getToElement(), queryGraph, parsedQuery, visitedNodes ) ) );
		}
		for( LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression>.Edge incommingEdges : queryGraph.getPredecessors( currentVar )){
			if( !parsedQuery.getDistinguishedVariables().contains( incommingEdges.getToElement() ) && !visitedNodes.contains( incommingEdges.getToElement() ) )
				allDescriptions.add( factory.getOWLObjectSomeRestriction( incommingEdges.getEdgeLabel().getInverseProperty().getSimplified(), buildDescriptionForNode( incommingEdges.getToElement(), queryGraph, parsedQuery, visitedNodes ) ) );
		}
		
		if( queryGraph.getLabelsOfNode( currentVar ) != null )
			allDescriptions.addAll( queryGraph.getLabelsOfNode( currentVar ) );
		if( allDescriptions.size() == 0 )
			return factory.getOWLThing();
		else if ( allDescriptions.size() == 1 )
			return allDescriptions.iterator().next();
		
		return factory.getOWLObjectIntersectionOf( allDescriptions );
	}
	
	private boolean isEdgeUniqueWithSpecificSuccessor(Variable currentVar,OWLObjectPropertyExpression edgeLabel, Variable toElement, Set<LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression>.Edge> edgesOfNode) {
		for( LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression>.Edge outgoingEdges : edgesOfNode )
			if( outgoingEdges.getToElement().equals( toElement ) && !outgoingEdges.getEdgeLabel().equals( edgeLabel ) )
				return false;
		return true;
	}
	
	private Set<OWLAxiom> keepAccurateRepairs(SystemInterface cqSystem, OWLOntology tBox,URI physicalURIOfBaseOntology, Set<OWLAxiom> patternAxioms, int queryIndex) throws Exception {
		cqSystem.initializeSystem();
		Set<OWLAxiom> maximalPatternAxioms = new HashSet<OWLAxiom>( patternAxioms );
		Set<OWLAxiom> prunnedAxioms = new HashSet<OWLAxiom>( );
		Reasoner reasoner = new Reasoner( tBox, new Configuration() );
		for( OWLAxiom ax1 : patternAxioms ){
			for( OWLAxiom ax2 : patternAxioms ){
				if( ax1.equals( ax2 ) || prunnedAxioms.contains( ax1 ) )
					continue;
				
				OWLSubClassAxiom axiom1 = (OWLSubClassAxiom) ax1;
				OWLSubClassAxiom axiom2 = (OWLSubClassAxiom) ax2;
				if( axiom2.getSubClass().isAnonymous() != axiom1.getSubClass().isAnonymous() )
					continue;
				if( reasoner.isSubsumedBy( axiom2.getSubClass(), axiom1.getSubClass() ) ){
					if( isSystemCapableToReasonWithPatternAx2UsingAx1( cqSystem, tBox, physicalURIOfBaseOntology, axiom2.getSubClass(), axiom1, queryIndex ) ){
						maximalPatternAxioms.remove( ax2 );
						prunnedAxioms.add( ax2 );
					}
				}
			}
		}
		return maximalPatternAxioms;
	}

	private boolean isSystemCapableToReasonWithPatternAx2UsingAx1(SystemInterface cqSystem, OWLOntology tBox, URI physicalURIOfBaseOntology, OWLDescription subClassOfAx2, OWLSubClassAxiom axiom1, int queryIndex) throws Exception {
		//Load to the system the TBox extended with axiom ax1
		//Load to the system the original query q(x)
		//Load to the system the ABox A={a:axiom2.getSubClass()}, where a is new
		//See if the system can return q(a) using the extended TBox and A.
		//If yes then ax2 can be safely removed. The system can use pattern ax1 to reason over pattern ax2.

		cqSystem.loadQuery( queryIndex-1 );

		Set<OWLAxiom> extendedTBox = new HashSet<OWLAxiom>( tBox.getAxioms() );
		extendedTBox.add( axiom1 );

		String tempOntologyFileForExtendedTBox = saveOntology( tBox, physicalURIOfBaseOntology, extendedTBox, "_temp-TBox"  );
		tempOntologyFileForExtendedTBox = tempOntologyFileForExtendedTBox.replace( "file:/", "/" );

		Set<OWLAxiom> tempABox = new HashSet<OWLAxiom>( );
		sequenceGenerator = 0;
		tempABox.addAll( unfoldPatternIntoAssertions( subClassOfAx2, factory.getOWLIndividual( URI.create( "http://dymmy_" + ++sequenceGenerator ) ) ) );
		String tempOntologyFileForABox = saveOntology( tBox, physicalURIOfBaseOntology, tempABox, "_temp-ABox" );
		tempOntologyFileForABox = tempOntologyFileForABox.replace( "file:/", "/" );

		File tempOntologyFileForABoxFile = new File( tempOntologyFileForABox );
		cqSystem.loadTestToSystem( tempOntologyFileForExtendedTBox, tempOntologyFileForABoxFile );

		long certainAnswers = cqSystem.runLoadedQuery();
		cqSystem.clearRepository();

		if( certainAnswers < 1 )
			return false;
		return true;
	}

	private Set<OWLAxiom> unfoldPatternIntoAssertions( OWLDescription subClass, OWLIndividual owlNamedIndividual  ) {
		Set<OWLAxiom> classesAxioms = new HashSet<OWLAxiom>();
		if ( subClass instanceof OWLClass ){
			classesAxioms.add( factory.getOWLClassAssertionAxiom( owlNamedIndividual, (OWLClass)subClass ));
		}
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
	
	private OWLDescription getOWLDescriptionForQueryAtom(OWLOntology tBox, String predicate) {
		for( OWLClass ontoClass : tBox.getReferencedClasses() )
			if( ontoClass.getURI().getFragment().toString().equals( predicate ) )
				return ontoClass;
			
		return  factory.getOWLClass( URI.create( tBox.getURI() + "#" + predicate ) );
	}

	private OWLObjectPropertyExpression getOWLObjectPropertyForQueryAtom(OWLOntology tBox, String predicate) {
		for( OWLObjectProperty objPropExpr : tBox.getReferencedObjectProperties() )
			if( objPropExpr.getURI().getFragment().toString().equals( predicate ) )
				return objPropExpr;
			
		return factory.getOWLObjectProperty( URI.create( tBox.getURI() + "#" + predicate ) );
	}
}