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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.oxford.comlab.aBoxGeneration.ABoxInstantiation;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Term;
import org.oxford.comlab.requiem.rewriter.Variable;
import org.semanticweb.owl.model.OWLOntology;

/**
 * 
 * @author gios
 *
 * This class helps generate a (q,\T)-testing suite for the class of strongly faithful algorithms. That is it tries to build a UCQ rewriting by unfolding the 
 * potential Datalog-program up to a point and then generates the TB by an injective instantiation of the UCQ---that is, using all possible instantiation of 
 * the CQs. For this reason it overrides the instantiatedQuery inherited by the class of weakly faithful algorithms.
 */
public class StronglyFaithful extends WeaklyFaithful {

	@Override
	public Set<ABoxInstantiation> instantiateQuery(Clause query, OWLOntology inputOntology) {
		ArrayList<Variable> queryVars = query.getVariables();
		Map<Term,String> variableMappingToIndividuals = new HashMap<Term,String>();
		String distinguishedVariables = "";
		String nonDistinguishedVariables = "";
		for( int j=0 ; j<queryVars.size() ; j++ ){
			Variable queryVariable = queryVars.get( j );
			variableMappingToIndividuals.put( queryVariable, "a_" + j );
			if( query.getDistinguishedVariables().contains( queryVariable ) )
				distinguishedVariables = distinguishedVariables + "a_" + j + ";";
			else
				nonDistinguishedVariables = nonDistinguishedVariables + "a_" + j + ";";
		}
		distinguishedVariables = distinguishedVariables.replace("?", "");
		nonDistinguishedVariables = nonDistinguishedVariables.replace("?", "");
		return Collections.singleton( new ABoxInstantiation( distinguishedVariables, nonDistinguishedVariables, instantiateQueryUsingVariableMapping(inputOntology, query, variableMappingToIndividuals)) );
	}
	
	@Override
	public String getName() {
		return "C_s";
	}
}
