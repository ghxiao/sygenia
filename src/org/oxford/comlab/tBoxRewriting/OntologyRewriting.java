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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.oxford.comlab.datastructures.LabeledGraph;
import org.oxford.comlab.requiem.parser.ELHIOParser;
import org.oxford.comlab.requiem.resolutiontracking.VoidTracker;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Rewriter;
import org.oxford.comlab.requiem.rewriter.Term;
import org.oxford.comlab.requiem.rewriter.TermFactory;
import org.oxford.comlab.requiem.rewriter.Variable;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.RDFXMLOntologyFormat;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;

/**
 * 
 * @author gios
 *
 * This class was used for the experimental evaluation in the paper: 
 * 
 *  Giorgos Stoilos, Bernardo Cuenca Grau, Boris Motis, and Ian Horrocks. Repairing Ontologies for Incomplete Reasoners. Submitted to ISWC 2011. 
 */
public class OntologyRewriting {
	
	public static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	public static OWLDataFactory factory = manager.getOWLDataFactory();
	
	
	//REQUIEM specific
	protected TermFactory m_termFactory = new TermFactory(); 
	protected ELHIOParser m_parser = new ELHIOParser(m_termFactory);
	protected Rewriter m_rewriter = new Rewriter();
	
	public Set<Clause> rewriteOntology( String ontologyFile ) throws Exception {
		
		long timeStart = System.currentTimeMillis();
		
		System.out.println( "Computing the RL-rewriting of " + ontologyFile );
		
		URI physicalURIOfBaseOntology = URI.create( ontologyFile );
		OWLOntology onto = manager.loadOntologyFromPhysicalURI( physicalURIOfBaseOntology );
		
		ArrayList<Clause> clausifiedOntology = m_parser.getClauses(ontologyFile);

		Set<Clause> ontologyRewritingInClauses = null;
		Set<OWLAxiom> rlRewritingInAxioms = new HashSet<OWLAxiom>();
		Set<Clause> atomicQueries = new HashSet<Clause>();
		for( OWLClass owlClasses : onto.getReferencedClasses() ){
			Term queryHead = m_termFactory.getFunctionalTerm("Q", m_termFactory.getVariable(0));
			Term[] queryBody = new Term[1];
			queryBody[0] = m_termFactory.getFunctionalTerm(owlClasses.getURI().getFragment(), m_termFactory.getVariable(0));
			atomicQueries.add( new Clause( queryBody, queryHead) );
		}
		for( OWLObjectProperty owlObjProperty : onto.getReferencedObjectProperties() ){
			Term queryHead = m_termFactory.getFunctionalTerm("Q", m_termFactory.getVariable(0),m_termFactory.getVariable(1));
			Term[] queryBody = new Term[1];
			queryBody[0] = m_termFactory.getFunctionalTerm(owlObjProperty.getURI().getFragment(), m_termFactory.getVariable(0),m_termFactory.getVariable(1));
			atomicQueries.add( new Clause( queryBody, queryHead) );
		}

		for( Clause currentAtomicQuery : atomicQueries ) {
			System.out.println( "Rewriting atomic query: " + currentAtomicQuery );
			ArrayList<Clause> clauseSetToBeSaturated = new ArrayList<Clause>( clausifiedOntology );
			clauseSetToBeSaturated.add( currentAtomicQuery );
			
			ArrayList<Clause> rewriting = m_rewriter.rewrite(clauseSetToBeSaturated, "G", new VoidTracker());
			if( currentAtomicQuery.getDistinguishedVariables().size() == 1 ) {				
				for( Clause clauseInRewriting : rewriting ) {
//					if( clauseInRewriting.toString().contains("Tr_") || clauseInRewriting.toString().contains("AUX$"))
//						continue;
//					System.out.println( clauseInRewriting );
					if( clauseInRewriting.getBody().length==0 || clauseInRewriting.getBody()[0].toString().equals(currentAtomicQuery.getBody()[0].toString()) ) //avoiding tautologies
						continue;

					Map<Variable,OWLDescription> rolledUpConcepts = rollUpQueryInUCQRewriting( onto, clauseInRewriting, currentAtomicQuery );
					for( Variable var : rolledUpConcepts.keySet() )
						rlRewritingInAxioms.add( factory.getOWLSubClassAxiom(rolledUpConcepts.get(var), getOWLDescriptionForQueryAtom(onto, currentAtomicQuery.getBody()[0].getFunctionalPrefix())) );
				}
			}
			else
				for( Clause clauseInRewriting : rewriting ) {
					if( clauseInRewriting.getBody()[0].toString().equals(currentAtomicQuery.getBody()[0].toString()) ) //avoiding tautologies
						continue;
					OWLObjectPropertyExpression subObjectProperty=null;
					if( currentAtomicQuery.getBody()[0].getArgument(0).equals(clauseInRewriting.getBody()[0].getArgument(0)) && currentAtomicQuery.getBody()[0].getArgument(1).equals(clauseInRewriting.getBody()[0].getArgument(1)))
						subObjectProperty=getOWLObjectPropertyForQueryAtom(onto, clauseInRewriting.getBody()[0].getFunctionalPrefix());
					else if (currentAtomicQuery.getBody()[0].getArgument(1).equals(clauseInRewriting.getBody()[0].getArgument(0)) && currentAtomicQuery.getBody()[0].getArgument(0).equals(clauseInRewriting.getBody()[0].getArgument(1)))
						subObjectProperty=getOWLObjectPropertyForQueryAtom(onto, clauseInRewriting.getBody()[0].getFunctionalPrefix()).getInverseProperty();
					rlRewritingInAxioms.add( factory.getOWLSubObjectPropertyAxiom( subObjectProperty, getOWLObjectPropertyForQueryAtom(onto, currentAtomicQuery.getBody()[0].getFunctionalPrefix() )));
				}
		}
		String pathToStoreRLRewriting = physicalURIOfBaseOntology.toString().replace(".owl", "") + "_RL-rewriting" + ".owl";
		
		URI physicalURIOfRLRewriting = URI.create( pathToStoreRLRewriting );
		OWLOntology rlRewriting = manager.createOntology( URI.create( onto.getURI() + "_RL-rewriting" ) );

//		manager.addAxiom( extendedOntology, factory.getOWLImportsDeclarationAxiom(extendedOntology, physicalURIOfBaseOntology));
//		manager.addAxioms( extendedOntology, tBox.getAxioms() );
		manager.addAxioms( rlRewriting, rlRewritingInAxioms );
		
		manager.saveOntology( rlRewriting, new RDFXMLOntologyFormat(), physicalURIOfRLRewriting );
		
		System.out.println( "RL-rewriting computed by rewriting " + (onto.getReferencedClasses().size()+onto.getReferencedObjectProperties().size()) + " atomic symbols");
		System.out.println( "RL-rewriting contains: " + rlRewriting.getAxiomCount() + " axioms and computed in " + (System.currentTimeMillis()-timeStart) + " ms");
		
		return ontologyRewritingInClauses;		
	}
	
	private Map<Variable,OWLDescription> rollUpQueryInUCQRewriting( OWLOntology tBox, Clause currentClause, Clause currentAtomicQuery ){
		
		Term[] bodyTerms = currentClause.getBody();
		LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression> queryGraph = new LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression>();
		for( int i=0 ; i < bodyTerms.length ; i++ ){
			Term[] variablesOfTerm = bodyTerms[i].getArguments();

			if( variablesOfTerm.length == 2 ) {
				if( variablesOfTerm[0] instanceof Variable && variablesOfTerm[1] instanceof Variable )
					queryGraph.addEdge( (Variable)variablesOfTerm[0], (Variable)variablesOfTerm[1], getOWLObjectPropertyForQueryAtom(tBox, bodyTerms[i].getName() ) );
			}
			else {
				Variable queryVar = (Variable)variablesOfTerm[0];
				queryGraph.addLabel(queryVar, getOWLDescriptionForQueryAtom(tBox, bodyTerms[i].getName()));
			}
		}
		return buildRolledUpConceptsFromQueryGraph( queryGraph, currentClause );
	}
	
	private Map<Variable, OWLDescription> buildRolledUpConceptsFromQueryGraph(LabeledGraph<Variable,OWLDescription,OWLObjectPropertyExpression> queryGraph, Clause currentClause) {
		
//		if( queryGraph.hasCycles( ) )
//			return null;
		
		Map<Variable,OWLDescription> varsToRolledUpConcepts = new HashMap<Variable,OWLDescription>();
		Queue<Variable> toVisit = new LinkedList<Variable>();
        toVisit.addAll( currentClause.getDistinguishedVariables() );
        Set<Variable> visitedNodes = new HashSet<Variable>();
        while( !toVisit.isEmpty() ){
        	Variable currentVar = toVisit.poll();
        	visitedNodes.add( currentVar );
        	OWLDescription complexConcept = buildDescriptionForNode( currentVar, queryGraph, currentClause, visitedNodes );
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
