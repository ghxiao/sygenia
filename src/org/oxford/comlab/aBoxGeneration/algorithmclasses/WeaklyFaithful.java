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

package org.oxford.comlab.aBoxGeneration.algorithmclasses;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.oxford.comlab.Configuration;
import org.oxford.comlab.Configuration.TestSuiteType;
import org.oxford.comlab.aBoxGeneration.ABoxInstantiation;
import org.oxford.comlab.aBoxGeneration.AllVariableMappings;
import org.oxford.comlab.datastructures.TreeNode;
import org.oxford.comlab.requiem.Requiem;
import org.oxford.comlab.requiem.optimizer.Optimizer;
import org.oxford.comlab.requiem.resolutiontracking.DependencyRestrictedSubsumption;
import org.oxford.comlab.requiem.resolutiontracking.ResolutionTracker;
import org.oxford.comlab.requiem.resolutiontracking.VoidTracker;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Term;
import org.oxford.comlab.requiem.rewriter.TermFactory;
import org.oxford.comlab.requiem.rewriter.Variable;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;

/**
 * 
 * @author gios
 *
 * This class helps generate a (q,\T)-testing suite for the class of weakly faithful algorithms. That is it tries to build a UCQ rewriting by unfolding 
 * the potential Datalog-program up to a point and then generates the TB by fully instantiating the UCQ---that is, using all possible instantiation of the CQs. 
 */
public class WeaklyFaithful implements AlgorithmClass {
	
	public ResolutionTracker tracker;
	public Requiem requiemRewriter;
	
	public String[] reqArgs;
	Set<OWLEntity> ontologyEntities;

	public ArrayList<Clause> buildUCQRewriting(String queryFile, String ontologyFile, Configuration config, OWLOntology ontology) throws Exception {
		
		ontologyEntities = ontology.getReferencedEntities();
		
		ArrayList<Clause> finalClauses = cleanUpRewriting(computeRew(queryFile, ontologyFile, config));
		if(  Boolean.parseBoolean( config.storeRewritingsToFile.toString().toLowerCase() ) )
			requiemRewriter.printRewritingToFile( finalClauses, reqArgs[0], reqArgs[1]);
		
    	return finalClauses;
	}
	
	protected ArrayList<Clause> computeRew(String queryFile, String ontologyFile, Configuration config) throws Exception{
		reqArgs = new String[5];
		reqArgs[0] = queryFile;
		reqArgs[1] = ontologyFile;
		reqArgs[2] = queryFile + "_rewriting.txt";
		reqArgs[3] = "G";
		tracker = new VoidTracker();
		if( config.testSuiteType == TestSuiteType.GENERAL )
			tracker = new DependencyRestrictedSubsumption(); 
		reqArgs[4] = config.storeRewritingsToFile.toString().toLowerCase();

		requiemRewriter = new Requiem();
		
		ArrayList<Clause> rewriting = requiemRewriter.computeRewriting( reqArgs, tracker );

		//Condensation reduction 
		Optimizer optimizer = new Optimizer(  new TermFactory() );
		rewriting = optimizer.condensate( rewriting );
		
		return rewriting; 
	}
	
	private ArrayList<Clause> cleanUpRewriting(ArrayList<Clause> rewriting){
		ArrayList<Clause> finalClauses = new ArrayList<Clause>( rewriting );
		
		//Clean-up clauses that contain atoms which are not in the original ontology or their head is not Q. 
		Term originalQueryHead = requiemRewriter.getQuery().getHead();
		Set<String> originalQuerySignature = new HashSet<String>();
		for( Term bodyTerm : requiemRewriter.getQuery().getBody() )
			originalQuerySignature.add( bodyTerm.getName() );
		
		for( Clause cl : rewriting ){
			Term[] bodyTerms = cl.getBody();
			if( !cl.getHead().getName().toString().equals( originalQueryHead.getName().toString() ) ){
				finalClauses.remove( cl );
				continue;
			}
			for( int i=0 ; i<bodyTerms.length ; i++ )
				if( !isContainedIn( bodyTerms[i].getName() , ontologyEntities ) )
					if( !originalQuerySignature.contains( bodyTerms[i].getName() ) ){
						finalClauses.remove( cl );
						break;
					}
			for (Clause cl2 : finalClauses) {
				if (!cl2.equals(cl) && cl2.toString().equals(cl.toString())) {
					finalClauses.remove( cl );
					break;
				}
			}
		}
		return finalClauses;
	}
	
	@Override
	public Set<ABoxInstantiation> instantiateQuery(Clause query, OWLOntology inputOntology) {
		
		//Set of all ABox instantiations of the form A^q_\pi
		Set<ABoxInstantiation> queryTestingUnits = new HashSet<ABoxInstantiation>();

		//Create every possible variable mapping for the current query
		ArrayList<String> combinationsOfIndividuals = buildAllVariableInstantiations( query );

		for( int j=0 ; j<combinationsOfIndividuals.size() ; j++ ){
			String[] distNonDistVars = { "", "" };
			Map<Term,String> variableMappingInstantiation = parseIndividualCombinations( combinationsOfIndividuals.get( j ), query, distNonDistVars );
			String distinguishedVarString = distNonDistVars[0];
			String nonDistinguishedVarString = distNonDistVars[1];

			Set<OWLAxiom> testingUnitAssertions = instantiateQueryUsingVariableMapping(inputOntology, query, variableMappingInstantiation );

			ABoxInstantiation instantiatedABox = new ABoxInstantiation( distinguishedVarString, nonDistinguishedVarString, testingUnitAssertions); 					
			queryTestingUnits.add( instantiatedABox );

		}//end of combinationsOfIndividuals
		return queryTestingUnits;
	}
	
	private ArrayList<String> buildAllVariableInstantiations( Clause query ) {

		ArrayList<Variable> queryVars = query.getVariables();
		String[] rangeOfInstantiation = new String[queryVars.size()];
		for( int j=0 ; j<queryVars.size() ; j++ )
			rangeOfInstantiation[j] =  "a_" + j;

		TreeNode<String> root = new TreeNode<String>( rangeOfInstantiation[0], 1 );
		AllVariableMappings<String> tree = new AllVariableMappings<String>( root );
		if( queryVars.size() > 1 ){
			int numOfPossibleSuccessorsOfRoot = 2;
			tree.createAllVariableInstantiations( root, rangeOfInstantiation, numOfPossibleSuccessorsOfRoot );
		}

		ArrayList<String> combinationsOfIndividuals = new ArrayList<String>();
		tree.readVariableInstantiations( root, combinationsOfIndividuals );
		
		return combinationsOfIndividuals;
	}
	
	private Map<Term, String> parseIndividualCombinations(String indvCombination,Clause query, String[] distNonDistVars) {
		
		ArrayList<Variable> allQueryVars = query.getVariables();
		ArrayList<Variable> distinguishedVars = query.getDistinguishedVariables();
		
		Map<Term,String> variableMappingInstantiation = new HashMap<Term,String>();
		int k=-1;
		
		StringTokenizer strTok = new StringTokenizer( indvCombination, ";" );
		while( strTok.hasMoreTokens() ){
			String singleVarInstantiation = strTok.nextToken();
			variableMappingInstantiation.put( allQueryVars.get( ++k ), singleVarInstantiation );
			if( distinguishedVars.contains(  allQueryVars.get( k ) ) )
				distNonDistVars[0] = distNonDistVars[0] + singleVarInstantiation + ";";
			else
				distNonDistVars[1] = distNonDistVars[1] + singleVarInstantiation + ";";
		}
		return variableMappingInstantiation;
	}
	
	public Set<OWLAxiom> instantiateQueryUsingVariableMapping(OWLOntology ontology, Clause clause, Map<Term, String> variableMappingToIndividuals) {
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		
		URI ontoURI = ontology.getURI();
		Set<OWLDataProperty> dataProperties = ontology.getReferencedDataProperties();
		Set<OWLObjectProperty> objectProperties = ontology.getReferencedObjectProperties();
		Set<OWLClass> classExpressions = ontology.getReferencedClasses();

		Set<OWLAxiom> assertionsForPattern = new HashSet<OWLAxiom>();
		Term[] bodyTerms = clause.getBody();
		for( int k=0 ; k<bodyTerms.length ; k++ ){
			String predicate = bodyTerms[k].getName();
			Term[] variablesOfTerm = bodyTerms[k].getArguments();
			OWLIndividual indvA = factory.getOWLIndividual( URI.create( ontoURI + "#" +  variableMappingToIndividuals.get( variablesOfTerm[0] ) ));
			if( variablesOfTerm.length == 2 ){
				OWLIndividual indvB = factory.getOWLIndividual( URI.create( ontoURI + "#" + variableMappingToIndividuals.get( variablesOfTerm[1] ) ));
				
				String uriForNewObjectProp = checkURIInObjectProps( objectProperties, predicate ); 
				if( uriForNewObjectProp != null ){
					OWLObjectProperty objectProp = factory.getOWLObjectProperty( URI.create( uriForNewObjectProp + "#" + predicate ) );
					assertionsForPattern.add( factory.getOWLObjectPropertyAssertionAxiom( indvA , objectProp, indvB) );
					continue;
				}
				String uriForNewDataProp = checkURIInDataProps( dataProperties, predicate );
				if( uriForNewDataProp != null ){
					OWLDataProperty dataProp = factory.getOWLDataProperty( URI.create( uriForNewDataProp + "#" + predicate ) );
					assertionsForPattern.add( factory.getOWLDataPropertyAssertionAxiom( indvA , dataProp, "XX-XXX") );
				}
				else{
					OWLObjectProperty objectProp = factory.getOWLObjectProperty( URI.create( ontoURI + "#" + predicate ) );
					assertionsForPattern.add( factory.getOWLObjectPropertyAssertionAxiom( indvA , objectProp, indvB) );
				}
			}
			else{
				String uriForNewClass = checkURIInClasses( classExpressions, predicate );
				OWLDescription classExpression;
				if( uriForNewClass != null )
					classExpression = factory.getOWLClass( URI.create( uriForNewClass + "#" + predicate ) );
				else
					classExpression = factory.getOWLClass( URI.create( ontoURI + "#" + predicate ) );
				
				assertionsForPattern.add( factory.getOWLClassAssertionAxiom(indvA, classExpression) );

			}
		}
		return assertionsForPattern;
	}

	private static String checkURIInClasses(Set<OWLClass> classExpressions, String predicate) {
		for( OWLClass ontoClass : classExpressions )
			if( ontoClass.getURI().toString().contains( predicate ) )
				return ontoClass.getURI().getScheme() + "://" + ontoClass.getURI().getHost() + ontoClass.getURI().getPath();
		return null;
	}

	private static String checkURIInObjectProps(Set<OWLObjectProperty> objectProperties, String predicate) {
		for( OWLObjectProperty objPropExpr : objectProperties )
			if( objPropExpr.getURI().toString().contains( predicate ) )
				return objPropExpr.getURI().getScheme() + "://" + objPropExpr.getURI().getHost() + objPropExpr.getURI().getPath();
		return null;
	}
	
	private static String checkURIInDataProps(Set<OWLDataProperty> dataProperties , String predicate) {
		for( OWLDataProperty dataPropExpr : dataProperties )
			if( dataPropExpr.getURI().toString().contains( predicate ) )
				return dataPropExpr.getURI().getScheme() + "://" + dataPropExpr.getURI().getHost() + dataPropExpr.getURI().getPath();
		return null;
	}
	
	protected static boolean isContainedIn(String bodyTerm, Set<OWLEntity> ontologyEntities) {
		for( OWLEntity entity : ontologyEntities )
			if( entity.getURI().getFragment().equals( bodyTerm ) )
				return true;
		return false;
	}

	@Override
	public String getName() {
		return "C_w";
	}
}