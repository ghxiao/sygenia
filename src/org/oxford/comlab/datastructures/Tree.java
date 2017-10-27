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
package org.oxford.comlab.datastructures;

import java.util.ArrayList;

public class Tree<E> {

	private TreeNode<E> rootNode;
	
	public Tree(TreeNode<E> rootNode){
		this.rootNode = rootNode;
	}
	
	public TreeNode<E> getRootNode(){
		return rootNode;
	}
	
	public void getTreeNodes(TreeNode<E> treeNode, ArrayList<E> nodes){
		if( !treeNode.equals( this.rootNode ) )
			nodes.add( treeNode.getNodeValue() );
		for(TreeNode<E> child : treeNode.getSuccessors() )
			getTreeNodes( child, nodes );
	}
	
}
