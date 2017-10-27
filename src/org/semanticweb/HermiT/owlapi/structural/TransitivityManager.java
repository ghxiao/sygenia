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

package org.semanticweb.HermiT.owlapi.structural;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.util.GraphUtils;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLObjectAllRestriction;
import org.semanticweb.owl.model.OWLObjectComplementOf;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;

/**
 * This is a method used by an old version of HermiT. It contains a bug fix.
 * @author gios
 *
 */
public class TransitivityManager extends OwlNormalization.RoleManager {
    protected final Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> m_subObjectProperties;
    protected final Collection<OWLObjectPropertyExpression[]> m_toldSubObjectProperties;
    protected final Set<OWLObjectPropertyExpression> m_transitiveObjectProperties;
    protected final Map<OWLObjectAllRestriction, OWLDescription> m_replacedDescriptions;

    public TransitivityManager() {
        m_subObjectProperties = new HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>>();
        m_toldSubObjectProperties = new ArrayList<OWLObjectPropertyExpression[]>();
        m_transitiveObjectProperties = new HashSet<OWLObjectPropertyExpression>();
        m_replacedDescriptions = new HashMap<OWLObjectAllRestriction, OWLDescription>();
    }

    public void addInclusion(OWLObjectPropertyExpression sub,
            OWLObjectPropertyExpression sup) {
        sub = sub.getSimplified();
        sup = sup.getSimplified();
        m_toldSubObjectProperties.add
            (new OWLObjectPropertyExpression[] { sub, sup });
        addInclusionEx(sub, sup);
        addInclusionEx(sub.getInverseProperty().getSimplified(),
                        sup.getInverseProperty().getSimplified());
    }

    public void addInclusion(List<OWLObjectPropertyExpression> chain,
                                OWLObjectPropertyExpression implied) {
        if (chain.size() == 1) {
            addInclusion(chain.get(0), implied);
        } else if (chain.size() == 2 &&
                    chain.get(0) == implied &&
                    chain.get(1) == implied) {
            makeTransitive(implied);
        } else {
            throw new RuntimeException(
                "Object property chains not supported by this role manager.");
        }
    }
    
    public void makeTransitive(OWLObjectPropertyExpression objectProperty) {
        m_transitiveObjectProperties.add(objectProperty.getSimplified());
        m_transitiveObjectProperties.add(objectProperty.getInverseProperty().getSimplified());
    }

    public void rewriteConceptInclusions(
            Collection<OWLDescription[]> inclusions, OWLDataFactory factory) {
        transitivelyClose();
        for (OWLDescription[] inclusion : inclusions) {
            for (int index = 0; index < inclusion.length; index++) {
                inclusion[index] = replaceDescriptionIfNecessary(inclusion[index], factory);
            }
        }
        for (Map.Entry<OWLObjectAllRestriction, OWLDescription> replacement : m_replacedDescriptions.entrySet()) {
            inclusions.add(new OWLDescription[] {
                    replacement.getValue().getComplementNNF(),
                    replacement.getKey() });
            OWLObjectPropertyExpression objectProperty = replacement.getKey().getProperty();
            for (OWLObjectPropertyExpression transitiveSubObjectProperty : getTransitiveSubObjectProperties(objectProperty)) {
                OWLObjectAllRestriction consequentAll = factory.getOWLObjectAllRestriction(
                        transitiveSubObjectProperty,
                        replacement.getKey().getFiller());
                OWLDescription consequentReplacement = m_replacedDescriptions.get(consequentAll);
                assert consequentReplacement != null;
                OWLObjectAllRestriction forallConsequentReplacement = factory.getOWLObjectAllRestriction(
                        transitiveSubObjectProperty, consequentReplacement);
                inclusions.add(new OWLDescription[] {
                        replacement.getValue().getComplementNNF(),
                        forallConsequentReplacement });
            }
        }
        m_replacedDescriptions.clear();
    }

    public Collection<OWLObjectPropertyExpression[]> getSimpleInclusions() {
        return m_toldSubObjectProperties;
    }

    protected void addInclusionEx(
            OWLObjectPropertyExpression subObjectProperty,
            OWLObjectPropertyExpression superObjectProperty) {
        Set<OWLObjectPropertyExpression> subObjectProperties = m_subObjectProperties.get(superObjectProperty);
        if (subObjectProperties == null) {
            subObjectProperties = new HashSet<OWLObjectPropertyExpression>();
            m_subObjectProperties.put(superObjectProperty,
                    subObjectProperties);
        }
        subObjectProperties.add(subObjectProperty);
    }

    protected OWLDescription replaceDescriptionIfNecessary(
            OWLDescription desc, OWLDataFactory factory) {
        if (desc instanceof OWLObjectAllRestriction) {
            OWLObjectAllRestriction objectAll = (OWLObjectAllRestriction) desc;
            OWLObjectPropertyExpression objectProperty = objectAll.getProperty();
            Set<OWLObjectPropertyExpression> transitiveSubObjectProperties = getTransitiveSubObjectProperties(objectProperty);
            if (!transitiveSubObjectProperties.isEmpty()) {
                OWLDescription replacement = getReplacementFor(objectAll,factory);
                for (OWLObjectPropertyExpression transitiveSubObjectProperty : transitiveSubObjectProperties) {
                    OWLObjectAllRestriction subObjectAll = factory.getOWLObjectAllRestriction(
                            transitiveSubObjectProperty,
                            objectAll.getFiller());
                    getReplacementFor(subObjectAll, factory);
                }
                return replacement;
            }
        }
        return desc;
    }
	/**
	 * A bug fixed here. Keys for normalisation of transitive roles were getting mixed with keys for normalisation of nested quantifiers. 
	 */
    protected OWLDescription getReplacementFor(
            OWLObjectAllRestriction objectAll, OWLDataFactory factory) {
        OWLDescription replacement = m_replacedDescriptions.get(objectAll);
        if (replacement == null) {
            replacement = factory.getOWLClass(URI.create("internal:all#"
            		+ "Tr_" + m_replacedDescriptions.size() ));
            if (objectAll.getFiller() instanceof OWLObjectComplementOf)
                replacement = replacement.getComplementNNF();
            m_replacedDescriptions.put(objectAll, replacement);
        }
        return replacement;
    }

    protected void transitivelyClose() {
        GraphUtils.transitivelyClose(m_subObjectProperties);
    }

    protected Set<OWLObjectPropertyExpression> getTransitiveSubObjectProperties(
            OWLObjectPropertyExpression objectProperty) {
        Set<OWLObjectPropertyExpression> result = new HashSet<OWLObjectPropertyExpression>();
        if (m_transitiveObjectProperties.contains(objectProperty))
            result.add(objectProperty);
        Set<OWLObjectPropertyExpression> subObjectProperties = m_subObjectProperties.get(objectProperty);
        if (subObjectProperties != null)
            for (OWLObjectPropertyExpression subObjectProperty : subObjectProperties)
                if (m_transitiveObjectProperties.contains(subObjectProperty))
                    result.add(subObjectProperty);
        return result;
    }
}
