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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class LabeledGraph<T,C,E> {

	public class Edge {
		protected T toElement;
		protected E edgeLabel;
		
		public Edge( E label, T to ){
	        edgeLabel = label;
	        toElement = to;
		}
		public T getToElement(){
			return toElement;
		}
		public E getEdgeLabel(){
			return edgeLabel;
		}
		public String toString(){
			return edgeLabel + " " + toElement;
		}
	}
	
    protected final Set<T> elements;
    protected final Map<T,Set<C>> labelsByNodes;
    protected final Map<T,Set<E>> edgeLabelsByNodes;
    protected final Map<T,Set<Edge>> successorsByNodes;
    protected final Map<T,Set<Edge>> predecessorsByNodes;

    public LabeledGraph() {
        elements = new HashSet<T>();
        labelsByNodes = new HashMap<T,Set<C>>();
        edgeLabelsByNodes = new HashMap<T,Set<E>>();
        successorsByNodes = new HashMap<T,Set<Edge>>();
        predecessorsByNodes = new HashMap<T,Set<Edge>>();
        
    }
    public void addLabel(T node, C nodeLabel){
    	Set<C> nodeLabels = labelsByNodes.get( node );
		if( nodeLabels == null ){
			nodeLabels = new HashSet<C>();
			labelsByNodes.put(node , nodeLabels);
		}
		nodeLabels.add(nodeLabel);
		elements.add(node);
    }
    public Set<C> getLabelsOfNode(T node){
    	return labelsByNodes.get( node );
    }
    public Set<E> getAllLabelsOfOutgoingEdges(T node){
    	return edgeLabelsByNodes.get( node );
    }
    public void addEdge(T from, T to, E label) {
        Set<Edge> successorEdges = successorsByNodes.get(from);
        if (successorEdges == null) {
        	successorEdges = new HashSet<Edge>();
            successorsByNodes.put(from, successorEdges);
        }
        Edge newEdge = new Edge(label, to);
        successorEdges.add(newEdge);
        
        Set<Edge> predecessorEdges = predecessorsByNodes.get(to);
        if (predecessorEdges == null) {
        	predecessorEdges = new HashSet<Edge>();
        	predecessorsByNodes.put(to, predecessorEdges);
        }
        Edge newEdgeInverse = new Edge(label, from);
        predecessorEdges.add(newEdgeInverse);
        
        Set<E> edgeLabels = edgeLabelsByNodes.get(from);
        if (edgeLabels == null) {
        	edgeLabels = new HashSet<E>();
        	edgeLabelsByNodes.put(from, edgeLabels);
        }
        edgeLabels.add(label);
        
        elements.add(from);
        elements.add(to);
    }
    public Set<T> getElements() {
        return elements;
    }
    public boolean hasCycles(){
		for( T var : elements )
			if( isReachableSuccessor( var, var ) )
				return true;
		return false;
	}
    public void invertEdge( T from, Edge edge, E invertedEdgeLabel ){
    	successorsByNodes.get( from ).remove( edge );
    	addEdge( edge.getToElement(), from, invertedEdgeLabel );
    }
    public boolean isReachableSuccessor(T fromNode,T toNode) {
//	    	if( fromNode.equals( toNode ))
//	    		return true;
        Set<T> result = new HashSet<T>();
        Queue<T> toVisit=new LinkedList<T>();
        toVisit.add(fromNode);
        while (!toVisit.isEmpty()) {
        	T current=toVisit.poll();
        	Set<Edge> successorEdges = getSuccessors( current );
            if( containsToNode( successorEdges , toNode ) )
            	return true;
            if( result.add(current) )
                toVisit.addAll( getAllToNodes( successorEdges ) );
        }
        return false;
    }
    private Set<T> getAllToNodes(Set<Edge> successorEdges) {
    	Set<T> allToNodes = new HashSet<T>();
    	for( Edge edge : successorEdges )
    		allToNodes.add( edge.getToElement() );
		return allToNodes;
	}
	private boolean containsToNode(Set<Edge> successorEdges, T toNode) {
		for( Edge edge : successorEdges )
			if( edge.getToElement().equals( toNode ))
				return true;
		return false;
	}
	public Set<Edge> getSuccessors(T node) {
        Set<Edge> result = successorsByNodes.get(node);
        if (result==null)
            result=Collections.emptySet();
        return result;
    }
	public Set<Edge> getPredecessors(T node) {
        Set<Edge> result = predecessorsByNodes.get(node);
        if (result==null)
            result=Collections.emptySet();
        return result;
    }
    public LabeledGraph<T,C,E> clone() {
    	LabeledGraph<T,C,E> result=new LabeledGraph<T,C,E>();
        for (Map.Entry<T,Set<Edge>> entry : successorsByNodes.entrySet()) {
            T from=entry.getKey();
            for (Edge successor : entry.getValue())
                result.addEdge(from,successor.getToElement(),successor.getEdgeLabel());
        }
        return result;
    }
	public void printGraph(){
		for( T var : getElements() ){
			System.out.println( "current var is: " + var  );
			for( Edge edge : getSuccessors( var ) ) 
				System.out.println( "successor var is " + edge.getToElement() + " with edge label: " + edge.getEdgeLabel() );
			for( Edge edge : getPredecessors( var ) ) 
				System.out.println( "predecessor var is " + edge.getToElement() + " with edge label: " + edge.getEdgeLabel() );
			
			System.out.println( );
		}			
	}
}