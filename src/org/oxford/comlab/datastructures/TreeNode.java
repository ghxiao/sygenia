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

package org.oxford.comlab.datastructures;

import java.util.HashSet;
import java.util.Set;

public class TreeNode<E> {
	
	private E nodeValue;
	private Set<TreeNode<E>> children;
	int nodeLevel;
	
	public TreeNode(E nodeLabel, int level){
		this.nodeValue = nodeLabel;
		this.children = new HashSet<TreeNode<E>>();
		this.nodeLevel = level;
	}
	
	public void addChild(TreeNode<E> childNode){
		this.children.add( childNode );
	}
	
	public Set<TreeNode<E>> getSuccessors(){
		return children;
	}
	public E getNodeValue(){
		return nodeValue;
	}
	public int getNodeLevel(){
		return nodeLevel;
	}
}
