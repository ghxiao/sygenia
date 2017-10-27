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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.oxford.comlab.Configuration;
import org.oxford.comlab.datastructures.Tree;
import org.oxford.comlab.datastructures.TreeNode;
import org.oxford.comlab.requiem.optimizer.Optimizer;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Resolution;
import org.oxford.comlab.requiem.rewriter.Saturator;
import org.oxford.comlab.requiem.rewriter.Term;
import org.oxford.comlab.requiem.rewriter.TermFactory;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLOntology;

/**
 * 
 * @author gios
 *
 * This class helps generate a (q,\T)-test suite for the class of First-Order Reproducible. The returned rewriting (which can be a Datalog-rewriting) is unfolded up 
 * to a point in order to remove any auxiliary terms not in the input ontology and then the program is instantiated injectively. For this reason it inherits the 
 * injective instantiation method of the class of strongly faithful algorithms, but overrides the inherited buildUCQRewriting method. 
 */
public class FOReproducible extends StronglyFaithful {
	
	private static TermFactory m_termFactory = new TermFactory(); 
	private static Saturator m_saturator = new Saturator( m_termFactory );
	private static Resolution m_resolution = new Resolution( m_saturator );
	
	private Map<Clause,Set<Clause>> blocks;

	@Override
	public ArrayList<Clause> buildUCQRewriting(String queryFile, String ontologyFile, Configuration config, OWLOntology ontology) throws Exception {
		
		ArrayList<Clause> rewriting = computeRew(queryFile, ontologyFile, config);
		ontologyEntities = ontology.getReferencedEntities();
		
		Optimizer optimizer = new Optimizer(  new TermFactory() );
		Clause queryClause = requiemRewriter.getQuery();
		ArrayList<Clause> unfoldedDatalogProgram = unfoldDatalogRules(queryClause, rewriting );
		unfoldedDatalogProgram = optimizer.querySubsumptionCheck( unfoldedDatalogProgram, tracker );

		rewriting.addAll( unfoldedDatalogProgram );
		
		rewriting = cleanUpRewriting(rewriting);
		if(  Boolean.parseBoolean( config.storeRewritingsToFile.toString().toLowerCase() ) )
			requiemRewriter.printRewritingToFile( rewriting, reqArgs[0], reqArgs[1]);
		
    	return rewriting;
	}
	
	public ArrayList<Clause> cleanUpRewriting(ArrayList<Clause> rewriting){
		ArrayList<Clause> finalClauses = new ArrayList<Clause>( rewriting );

		//Clean-up clauses that contain atoms which are not in the original ontology or their head is not Q. 
		Term originalQueryHead = requiemRewriter.getQuery().getHead();
		Set<String> originalQuerySignature = new HashSet<String>();
		for (Term bodyTerm : requiemRewriter.getQuery().getBody())
			originalQuerySignature.add( bodyTerm.getName() );
		for (Clause cl : rewriting) {
			Term[] bodyTerms = cl.getBody();
			boolean recursiveQuery = false;
			for (int i=0 ; i<bodyTerms.length ; i++) {
				if (!isContainedIn( bodyTerms[i].getName() , ontologyEntities ))
					if (!cl.getHead().getName().equals(bodyTerms[i].getName())) {
						if (!originalQuerySignature.contains( bodyTerms[i].getName() ) ) {
							finalClauses.remove( cl );
							break;
						}
					}
					else if (cl.getHead().getName().equals(bodyTerms[i].getName())) {
						recursiveQuery=true;
						break;
					}
			}
			if (!recursiveQuery && !cl.getHead().getName().equals( originalQueryHead.getName()) && !isContainedIn( cl.getHead().getName(), ontologyEntities) )
				finalClauses.remove( cl );
			//prune duplicates
			for (Clause cl2 : finalClauses) {
				if (!cl2.equals(cl) && cl2.toString().equals(cl.toString())) {
					finalClauses.remove( cl );
					break;
				}
			}
		}
		return finalClauses;
	}
	
	private ArrayList<Clause> unfoldDatalogRules(Clause goal, ArrayList<Clause> datalogProgramClauses ) {
		System.out.println( "unfolding started" );
		Term[] body = new Term[1];
        body[0] = goal.getHead();
    	Clause rootClause = new Clause(new Term[]{ goal.getHead() }, goal.getHead() );
    	
    	TreeNode<Clause> root = new TreeNode<Clause>( rootClause, 1 );
		Tree<Clause> tree = new Tree<Clause>( root );
		blocks = new HashMap<Clause,Set<Clause>>();
		blocks.put(root.getNodeValue(), new HashSet<Clause>() );
    	expandSLDTee(root, datalogProgramClauses);

    	ArrayList<Clause> rewrittenClauses = new ArrayList<Clause>();
    	tree.getTreeNodes(tree.getRootNode(), rewrittenClauses);
    	System.out.println( "unfolding completed" );
    	return rewrittenClauses;
	}
	
	private void expandSLDTee(TreeNode<Clause> currentNode, ArrayList<Clause> datalogProgramClauses) {
		int currentLevel = currentNode.getNodeLevel();
		Term[] currentNodeTerms = currentNode.getNodeValue().getBody();
		for( int index=0 ; index<currentNodeTerms.length ; index++ )
			for( Clause cl : datalogProgramClauses ){
//				if( blocks.get( currentNode.getNodeValue() ).contains( cl ) || !(containsSomeAuxTerm(currentNode.getNodeValue(), ontologyEntities) || containsSomeAuxTerm(cl, ontologyEntities)) )
				if( blocks.get( currentNode.getNodeValue() ).contains( cl ) || !containsSomeAuxTerm(currentNode.getNodeValue(), ontologyEntities) )
					continue;
				
				if( cl.getHead().getArity() == currentNodeTerms[index].getArity() && cl.getHead().getName().equals(currentNodeTerms[index].getName())){
					Clause newClause = m_resolution.resolve(currentNode.getNodeValue(), cl, index);
					
					Term[] newClauseBodyTerms = newClause.getBody();
					boolean recursiveClauseUsed=false;
					for( int j=0 ; j<newClauseBodyTerms.length ; j++ ){
						if( newClauseBodyTerms[j].getName().equals(currentNodeTerms[index].getName())){
							recursiveClauseUsed=true;
							break;
						}
					}
					if( recursiveClauseUsed )
						continue;
					TreeNode<Clause> newNode = new TreeNode<Clause>( newClause , currentLevel+1 ); 
					currentNode.addChild( newNode );
					Set<Clause> blocksForNewNode = new HashSet<Clause>();
					blocksForNewNode.addAll( blocks.get( currentNode.getNodeValue() ) );
					blocksForNewNode.add( cl );
					blocks.put(newClause, blocksForNewNode);

					expandSLDTee( newNode, datalogProgramClauses );
				}
			}
	}
	
	private boolean containsSomeAuxTerm(Clause cl, Set<OWLEntity> ontologyEntities) {
		for( Term bodyTerm : cl.getBody() )
			if( !isContainedIn( bodyTerm.getName(), ontologyEntities ) )
				return true;
		return false;
	}

	@Override
	public String getName() {
		return "C_f";
	}
	
}
