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

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.oxford.comlab.datastructures.LabeledGraph;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
import org.semanticweb.owl.model.OWLObjectSubPropertyAxiom;
import org.semanticweb.owl.model.OWLSubClassAxiom;

public class ChaseGenerator {
	
	private OWLDataFactory factory;
	private long sequenceGenerator = 0;
	private Set<OWLIndividual> rootIndividuals;
	private LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression> chase; 

	public ChaseGenerator(OWLDataFactory factory) {
		this.factory=factory;
	}
	
	public LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression> constructChase( Set<OWLSubClassAxiom> inclusionAxioms, Set<OWLObjectSubPropertyAxiom> roleInclusionAxioms ) {
		
		chase = new LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression>();
		initialiseChase( inclusionAxioms, roleInclusionAxioms, chase );
		expandChase( inclusionAxioms, roleInclusionAxioms, chase );
		return chase;
	}
	
	private void expandChase(Set<OWLSubClassAxiom> tBox, Set<OWLObjectSubPropertyAxiom> rBox, LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression> chase) {
		Set<OWLIndividual> inds = new HashSet<OWLIndividual>();
		inds.addAll(chase.getElements());
			boolean chaseExpanded = true;
			Set<String> newAxioms = new HashSet<String>();
			while (chaseExpanded) {
				chaseExpanded = false;
				inds.addAll(chase.getElements());
				Set<OWLIndividual> individuals = new HashSet<OWLIndividual>(inds);
				for (OWLIndividual currentIndividual:individuals ) {
					//System.out.println(currentIndividual);
					for ( OWLSubClassAxiom inclusionAxiom : tBox ) {
						OWLDescription subClassDescription = inclusionAxiom.getSubClass();
						if (!newAxioms.contains(currentIndividual+":"+inclusionAxiom)) {
							//If the LHS of the inclusion axioms is an atomic concept A
							if ( subClassDescription instanceof OWLClass && chase.getLabelsOfNode( currentIndividual ) != null && chase.getLabelsOfNode( currentIndividual ).contains(inclusionAxiom.getSubClass()) ) {
								chaseExpanded = applyTBoxAxiom(chase, inclusionAxiom, inclusionAxiom.getSuperClass(), currentIndividual, newAxioms, inds);
								newAxioms.add( currentIndividual+":"+inclusionAxiom );
							}
							//if the LHS of the inclusion axioms is an intersection of atomic concepts A \sqcap B
							else if (subClassDescription instanceof OWLObjectIntersectionOf ) {
								OWLObjectIntersectionOf objIntersection = (OWLObjectIntersectionOf)subClassDescription;
								if (chase.getLabelsOfNode( currentIndividual ) != null && chase.getLabelsOfNode( currentIndividual ).containsAll(objIntersection.getOperands())) {
									chaseExpanded = applyTBoxAxiom(chase, inclusionAxiom, inclusionAxiom.getSuperClass(), currentIndividual, newAxioms, inds);
									newAxioms.add( currentIndividual+":"+inclusionAxiom );
								}
							}
							//If the LHS of the inclusion axioms is an existential restriction \exists R.\top
							else if ( subClassDescription instanceof OWLObjectSomeRestriction ) {
								OWLObjectSomeRestriction objSomeRestriction = (OWLObjectSomeRestriction)subClassDescription;
								if (objSomeRestriction.getFiller().isOWLThing()) {
									//If there is an outgoing edge from the current individual that matches exactly R in \exists R.\top
									if ( chase.getAllLabelsOfOutgoingEdges(currentIndividual) != null && chase.getAllLabelsOfOutgoingEdges(currentIndividual).contains( objSomeRestriction.getProperty() ) ) {
										chaseExpanded = applyTBoxAxiom(chase, inclusionAxiom, inclusionAxiom.getSuperClass(), currentIndividual, newAxioms, inds);
										newAxioms.add( currentIndividual+":"+inclusionAxiom );
									}
									//If there is an outgoing edge from the current individual that is the inverse of the one that is in \exists R.\top
									else if ( chase.getAllLabelsOfOutgoingEdges(currentIndividual) != null && chase.getAllLabelsOfOutgoingEdges(currentIndividual).contains( objSomeRestriction.getProperty().getInverseProperty().getSimplified() ) ) {
										for ( LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression>.Edge outgoingEdges : chase.getSuccessors( currentIndividual ))
											if ( outgoingEdges.getEdgeLabel().equals( objSomeRestriction.getProperty().getInverseProperty().getSimplified() ) ) {
												chaseExpanded = applyTBoxAxiom(chase, inclusionAxiom, inclusionAxiom.getSuperClass(), outgoingEdges.getToElement(), newAxioms, inds);
												newAxioms.add( currentIndividual+":"+inclusionAxiom );
											}
									}
								}
								//If the LHS of the inclusion axiom is a qualified existential restriction \exists R.A
								else {
									if ( chase.getAllLabelsOfOutgoingEdges(currentIndividual)!=null && chase.getAllLabelsOfOutgoingEdges(currentIndividual).contains( objSomeRestriction.getProperty() ) ) {
										for ( LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression>.Edge outgoingEdges : chase.getSuccessors( currentIndividual ))
											if (outgoingEdges.getEdgeLabel().equals(objSomeRestriction.getProperty()) && chase.getLabelsOfNode(outgoingEdges.getToElement()) != null && chase.getLabelsOfNode(outgoingEdges.getToElement()).contains(objSomeRestriction.getFiller())) {
												chaseExpanded = applyTBoxAxiom(chase, inclusionAxiom, inclusionAxiom.getSuperClass(), currentIndividual, newAxioms, inds);
												newAxioms.add( currentIndividual+":"+inclusionAxiom );
											}
									}		
									else if ( chase.getAllLabelsOfOutgoingEdges(currentIndividual)!=null && chase.getAllLabelsOfOutgoingEdges(currentIndividual).contains( objSomeRestriction.getProperty().getInverseProperty().getSimplified() ) ) {
										for ( LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression>.Edge outgoingEdges : chase.getSuccessors( currentIndividual ))
											if ( outgoingEdges.getEdgeLabel().equals( objSomeRestriction.getProperty().getInverseProperty().getSimplified() )&& chase.getLabelsOfNode(outgoingEdges.getToElement()) != null && chase.getLabelsOfNode(outgoingEdges.getToElement()).contains(objSomeRestriction.getFiller())) {
												chaseExpanded = applyTBoxAxiom(chase, inclusionAxiom, inclusionAxiom.getSuperClass(), outgoingEdges.getToElement(), newAxioms, inds);
												newAxioms.add( currentIndividual+":"+inclusionAxiom );
											}
									}
								}
							}
						}
					}
					//Role Inclusion Axioms R \sqsubseteq S
					for (OWLObjectSubPropertyAxiom roleInclusionAxiom : rBox) {
						OWLObjectPropertyExpression subProperty = roleInclusionAxiom.getSubProperty();
						if (!newAxioms.contains(currentIndividual+":"+roleInclusionAxiom)) {
							Set<LabeledGraph<OWLIndividual, OWLClass, OWLObjectPropertyExpression>.Edge> successors = new HashSet<LabeledGraph<OWLIndividual, OWLClass, OWLObjectPropertyExpression>.Edge>(chase.getSuccessors(currentIndividual)); 
							for ( LabeledGraph<OWLIndividual, OWLClass, OWLObjectPropertyExpression>.Edge currentEdge : successors ) {
								OWLIndividual toElem = currentEdge.getToElement();
								if (currentEdge.getEdgeLabel().equals(subProperty)) {
									chase.addEdge(currentIndividual, toElem, roleInclusionAxiom.getSuperProperty());
									newAxioms.add( currentIndividual+":"+roleInclusionAxiom );
								}
							}
						}
					}
				}
			}
		}
	
	private boolean applyTBoxAxiom(LabeledGraph<OWLIndividual, OWLClass, OWLObjectPropertyExpression> chase, OWLSubClassAxiom inclusionAxiom, OWLDescription superClass, OWLIndividual currentIndividual, Set<String> newAxioms, Set<OWLIndividual> individuals ) {
		if (superClass instanceof OWLClass) {
			OWLClass sc = (OWLClass)superClass;
			chase.addLabel(currentIndividual, sc);
			return true;
		}
		else if (superClass instanceof OWLObjectIntersectionOf ) {
			Set<OWLDescription> intersectionOperands = ((OWLObjectIntersectionOf)superClass).getOperands();
			boolean someExpansion = false;
			for ( OWLDescription interOperand : intersectionOperands )
				someExpansion = applyTBoxAxiom(chase, inclusionAxiom, interOperand, currentIndividual, newAxioms, individuals);
			return someExpansion;
		}
		else if ( superClass instanceof OWLObjectSomeRestriction ) {
			OWLObjectSomeRestriction objExistsRestrictions = (OWLObjectSomeRestriction)superClass; 
			OWLObjectPropertyExpression objProperty = objExistsRestrictions.getProperty();

			OWLIndividual freshIndividual = factory.getOWLIndividual(URI.create("http://a_"+(sequenceGenerator++)));
			chase.addEdge(currentIndividual, freshIndividual, objProperty);
			individuals.add( freshIndividual );
			applyTBoxAxiom(chase, inclusionAxiom, objExistsRestrictions.getFiller(), freshIndividual, newAxioms, individuals);
			return true;
		}
		return false;
	}
	
	private void initialiseChase( Set<OWLSubClassAxiom> tBox, Set<OWLObjectSubPropertyAxiom> rBox, LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression> chase ) {
		rootIndividuals = new HashSet<OWLIndividual>();
		for ( OWLSubClassAxiom subClass : tBox ) {
			OWLDescription subConceptExpression = subClass.getSubClass();
			if ( subConceptExpression instanceof OWLClass ) {
				OWLClass owlClass = (OWLClass)subConceptExpression;
				OWLIndividual indvA = factory.getOWLIndividual(URI.create("http://a_"+owlClass.getURI().getFragment())); 
				chase.addLabel(indvA, owlClass);
				rootIndividuals.add(indvA);
			}
			else if ( subConceptExpression instanceof OWLObjectSomeRestriction ) {
				OWLObjectSomeRestriction  restr = (OWLObjectSomeRestriction)subConceptExpression;
				OWLObjectPropertyExpression objProp = restr.getProperty();
				OWLIndividual indvC = factory.getOWLIndividual(URI.create("http://c_"+objProp));
				OWLIndividual indvD = factory.getOWLIndividual(URI.create("http://d_"+objProp));
				chase.addEdge(indvC, indvD, objProp);
				if (!restr.getFiller().isOWLThing()&&(restr.getFiller() instanceof OWLClass)) {
					OWLClass fillerClass = (OWLClass)restr.getFiller();
					chase.addLabel(indvD, fillerClass);
				}
				rootIndividuals.add(indvC);
				rootIndividuals.add(indvD);
			}
//			else
				//TODO
		}
		for (OWLObjectSubPropertyAxiom roleInclAxiom : rBox) {
			OWLObjectPropertyExpression subProperty = roleInclAxiom.getSubProperty();
			OWLIndividual indvE = factory.getOWLIndividual(URI.create("http://e_"+subProperty));
			OWLIndividual indvF = factory.getOWLIndividual(URI.create("http://f_"+subProperty));
			chase.addEdge(indvE, indvF, subProperty);
			rootIndividuals.add(indvE);
			rootIndividuals.add(indvF);
		}
	}

	public void printGraph(LabeledGraph<OWLIndividual,OWLClass,OWLObjectPropertyExpression> chase) {
		for ( OWLIndividual indv : chase.getElements() )
			System.out.println( indv + " labels: " + chase.getLabelsOfNode(indv) + " succ: " + chase.getSuccessors(indv) );
	}
	
	public Set<OWLIndividual> getRootIndividuals() {
		return rootIndividuals;
	}
}