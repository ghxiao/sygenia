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

package org.oxford.comlab.requiem.resolutiontracking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.oxford.comlab.requiem.rewriter.Clause;

public abstract class ResolutionTracker {
	
	protected Map<Clause, Set<Clause>> dependencyGraph = new HashMap<Clause,Set<Clause>>(); 
	
	public void trackDependencyForGeneratedClause(Clause resolvent, Clause givenClause, Clause workedOffClause) {

		if( !dependencyGraph.containsKey( givenClause ) )
			dependencyGraph.put( givenClause, new HashSet<Clause>( ) );
		
		if( !dependencyGraph.containsKey( workedOffClause ) )
			dependencyGraph.put( workedOffClause, new HashSet<Clause>( ) );
		
    	Set<Clause> dependenciesOfResolvent = new HashSet<Clause>();
    	if( dependencyGraph.get( givenClause ).isEmpty() )
    		dependenciesOfResolvent.add( givenClause );
    	else
    		dependenciesOfResolvent.addAll( dependencyGraph.get( givenClause ) );
    	
    	if( dependencyGraph.get( workedOffClause ).isEmpty() )
    		dependenciesOfResolvent.add( workedOffClause );
    	else
    		dependenciesOfResolvent.addAll( dependencyGraph.get( workedOffClause ) );
    	
    	dependencyGraph.put( resolvent , dependenciesOfResolvent );
	}
	
	public abstract boolean isEligibleForSubsumptionBy( Clause candidateSubsumer, Clause candidateSubsumee );
	
	public Map<Clause, Set<Clause>> getDependencyGraph() {
		return dependencyGraph;
	}
}