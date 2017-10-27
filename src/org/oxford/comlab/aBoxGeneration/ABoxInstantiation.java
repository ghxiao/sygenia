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

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owl.model.OWLAxiom;

public class ABoxInstantiation {

	private String distinguishedVariableMapping;
	private String nonDistinguishedVariableMapping;
	private Set<OWLAxiom> ABoxAssertions;
	
	public ABoxInstantiation(String distinguishedVarString,String nonDistinguishedVarString, Set<OWLAxiom> testingUnitAssertions) {
		distinguishedVariableMapping = distinguishedVarString;
		nonDistinguishedVariableMapping = nonDistinguishedVarString;
		ABoxAssertions = new HashSet<OWLAxiom>( testingUnitAssertions );
	}

	public String getDistinguishedVariableMapping() {
		return distinguishedVariableMapping;
	}

	public Set<OWLAxiom> getABoxAssertions() {
		return ABoxAssertions;
	}

	public String getNonDistinguishedVariableMapping() {
		return nonDistinguishedVariableMapping;
	}
}
