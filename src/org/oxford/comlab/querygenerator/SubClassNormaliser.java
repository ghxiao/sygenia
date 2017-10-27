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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owl.model.OWLAntiSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLAxiomAnnotationAxiom;
import org.semanticweb.owl.model.OWLAxiomVisitor;
import org.semanticweb.owl.model.OWLClassAssertionAxiom;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLDataSubPropertyAxiom;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owl.model.OWLDisjointClassesAxiom;
import org.semanticweb.owl.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointUnionAxiom;
import org.semanticweb.owl.model.OWLEntityAnnotationAxiom;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owl.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLImportsDeclaration;
import org.semanticweb.owl.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyChainSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLObjectSubPropertyAxiom;
import org.semanticweb.owl.model.OWLOntologyAnnotationAxiom;
import org.semanticweb.owl.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLSameIndividualsAxiom;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owl.model.SWRLRule;

public class SubClassNormaliser implements OWLAxiomVisitor {
	
	Set<OWLSubClassAxiom> subClassAxioms;
	Set<OWLObjectSubPropertyAxiom> subPropertyAxioms;
	protected final OWLDataFactory factory;
	
	public SubClassNormaliser(OWLDataFactory factory) {
		this.factory = factory;
		subClassAxioms = new HashSet<OWLSubClassAxiom>();
		subPropertyAxioms = new HashSet<OWLObjectSubPropertyAxiom>();
	}
	
	public Set<OWLSubClassAxiom> getSubClassAxioms() {
		return subClassAxioms;
	}
	
	public Set<OWLObjectSubPropertyAxiom> getSubPropertyAxioms() {
		return subPropertyAxioms;
	}

	//@Override
	public void visit(OWLSubClassAxiom arg0) {
		subClassAxioms.add(arg0);
	}

	//@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom arg0) {
		
	}

	//@Override
	public void visit(OWLAntiSymmetricObjectPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLReflexiveObjectPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLDisjointClassesAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLDataPropertyDomainAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLImportsDeclaration arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLAxiomAnnotationAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLObjectPropertyDomainAxiom arg0) {
		OWLObjectPropertyDomainAxiom objDomainAx = (OWLObjectPropertyDomainAxiom) arg0;
		subClassAxioms.add( factory.getOWLSubClassAxiom( factory.getOWLObjectSomeRestriction(objDomainAx.getProperty(), factory.getOWLThing()), objDomainAx.getDomain()));		
	}

	//@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLDifferentIndividualsAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLDisjointDataPropertiesAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLDisjointObjectPropertiesAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLObjectPropertyRangeAxiom arg0) {
		OWLObjectPropertyRangeAxiom objRangeAx = (OWLObjectPropertyRangeAxiom) arg0;
		subClassAxioms.add( factory.getOWLSubClassAxiom( factory.getOWLObjectSomeRestriction(objRangeAx.getProperty().getInverseProperty(), factory.getOWLThing()), objRangeAx.getRange()));
	}

	//@Override
	public void visit(OWLObjectPropertyAssertionAxiom arg0) {
		
		
	}

	//@Override
	public void visit(OWLFunctionalObjectPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLObjectSubPropertyAxiom arg0) {
		subPropertyAxioms.add(arg0);
		
	}

	//@Override
	public void visit(OWLDisjointUnionAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLDeclarationAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLEntityAnnotationAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLOntologyAnnotationAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLSymmetricObjectPropertyAxiom arg0) {
		OWLObjectPropertyExpression prop = arg0.getProperty();
		subPropertyAxioms.add(factory.getOWLSubObjectPropertyAxiom(prop, prop.getInverseProperty()));
		subPropertyAxioms.add(factory.getOWLSubObjectPropertyAxiom(prop.getInverseProperty(), prop));
	}

	//@Override
	public void visit(OWLDataPropertyRangeAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLFunctionalDataPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLEquivalentDataPropertiesAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLClassAssertionAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLEquivalentClassesAxiom arg0) {
        Iterator<OWLDescription> iterator=arg0.getDescriptions().iterator();
        OWLDescription first=iterator.next();
        OWLDescription last=first;
        while (iterator.hasNext()) {
        	OWLDescription next=iterator.next();
        	subClassAxioms.add(factory.getOWLSubClassAxiom(last,next));
            last=next;
        }
        subClassAxioms.add(factory.getOWLSubClassAxiom(last,first));
	}

	//@Override
	public void visit(OWLDataPropertyAssertionAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLTransitiveObjectPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLIrreflexiveObjectPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLDataSubPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLSameIndividualsAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLObjectPropertyChainSubPropertyAxiom arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void visit(OWLInverseObjectPropertiesAxiom arg0) {
		OWLObjectPropertyExpression first = arg0.getFirstProperty();
		OWLObjectPropertyExpression second = arg0.getSecondProperty();
		subPropertyAxioms.add(factory.getOWLSubObjectPropertyAxiom(first, second.getInverseProperty()));
		subPropertyAxioms.add(factory.getOWLSubObjectPropertyAxiom(second, first.getInverseProperty()));
		subPropertyAxioms.add(factory.getOWLSubObjectPropertyAxiom(second.getInverseProperty(), first));
		subPropertyAxioms.add(factory.getOWLSubObjectPropertyAxiom(first.getInverseProperty(), second));
	
	}

	//@Override
	public void visit(SWRLRule arg0) {
		// TODO Auto-generated method stub
		
	}
}
