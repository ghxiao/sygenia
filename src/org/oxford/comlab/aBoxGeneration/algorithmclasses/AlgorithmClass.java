package org.oxford.comlab.aBoxGeneration.algorithmclasses;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.oxford.comlab.Configuration;
import org.oxford.comlab.aBoxGeneration.ABoxInstantiation;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Term;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLOntology;

public interface AlgorithmClass {
	
	public String getName();
	public Set<ABoxInstantiation> instantiateQuery(Clause query, OWLOntology inputOntology);
	public ArrayList<Clause> buildUCQRewriting(String queryFile, String ontologyFile, Configuration config, OWLOntology ontology) throws Exception;
	public Set<OWLAxiom> instantiateQueryUsingVariableMapping(OWLOntology ontology, Clause clause, Map<Term, String> variableMappingToIndividuals);

}
