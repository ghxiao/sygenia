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

import java.util.ArrayList;
import java.util.Set;

import org.oxford.comlab.datastructures.Tree;
import org.oxford.comlab.datastructures.TreeNode;

/**
 * This class implements the construction of all non-isomorphic instantiations for a given list of query variables. 
 */
public class AllVariableMappings<E> extends Tree<E> {
	
	public AllVariableMappings(TreeNode<E> rootNode) {
		super(rootNode);
	}

	public void readVariableInstantiations(TreeNode<E> root, ArrayList<String> idvCombos){
		buildIndvCombinations( root, idvCombos, "" );
	}
	
	private void buildIndvCombinations(TreeNode<E> currentNode, ArrayList<String> idvCombos, String str){
		Set<TreeNode<E>> children = currentNode.getSuccessors();
		str += currentNode.getNodeValue() + ";";
		for( TreeNode<E> child : children ){
			buildIndvCombinations( child, idvCombos, str);
		}
		if( children.isEmpty() )
			idvCombos.add( str );
	}
	
	public void createAllVariableInstantiations(TreeNode<String> currentNode, String[] individuals, int numOfPossibleSuccessors) {
		int currentNodeLevel = currentNode.getNodeLevel(); 
		if( currentNodeLevel == individuals.length )
			return;

		for( int i=0 ; i<numOfPossibleSuccessors ; i++ ){
			TreeNode<String> newNode = new TreeNode<String>( individuals[i] , currentNodeLevel + 1 );

			currentNode.addChild( newNode );
			if( reachedFarRightChild( i , numOfPossibleSuccessors ) )
				createAllVariableInstantiations( newNode, individuals, numOfPossibleSuccessors + 1 );
			else
				createAllVariableInstantiations( newNode, individuals, numOfPossibleSuccessors );
		}
	}
	private boolean reachedFarRightChild( int i, int numberOfChildren ){
		return (i == numberOfChildren - 1) ? true : false;
	}
	public void printSubTree(TreeNode<E> treeNode){
		System.out.println( treeNode.getNodeValue() );
		for(TreeNode<E> child : treeNode.getSuccessors() )
			printSubTree( child );
	}
}
