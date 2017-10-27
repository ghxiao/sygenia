package org.oxford.comlab.requiem.rewriter;

import java.util.ArrayList;
import java.util.HashSet;

import org.oxford.comlab.requiem.optimizer.Optimizer;
import org.oxford.comlab.requiem.resolutiontracking.ResolutionTracker;
import org.oxford.comlab.requiem.resolutiontracking.VoidTracker;

public class Saturator {
	protected TermFactory m_termFactory;
	protected final Resolution m_resolution;
	protected ArrayList<String> m_clausesCanonicals;
	protected ArrayList<Clause> m_workedOffClauses, m_unprocessedClauses;

	public Saturator(TermFactory termFactory) {
		m_termFactory = termFactory;
		m_resolution = new Resolution(this);
		m_workedOffClauses = new ArrayList<Clause>();
		m_unprocessedClauses = new ArrayList<Clause>();
		m_clausesCanonicals = new ArrayList<String>();
	}
	
	public TermFactory getTermFactory(){
		return this.m_termFactory;
	}

	public ArrayList<Clause> unfoldNaively(ArrayList<Clause> clauses) {
		ArrayList<Clause> result = new ArrayList<Clause>();
		result = this.saturate(clauses, new SelectionFunctionUnfold(), "N", new VoidTracker());
		return result;
	}
	
	public ArrayList<Clause> unfoldGreedily(ArrayList<Clause> clauses, ResolutionTracker tracker) {
		HashSet<String> IDBPredicates = new HashSet<String>();
		ArrayList<Clause> result = new ArrayList<Clause>();
		
		for(Clause c: clauses){
    		result.add(c);
    		IDBPredicates.add(c.getHead().getName());
		}
		Optimizer optimizer = new Optimizer( m_termFactory );
		int i=0;
    	for(String p: IDBPredicates){
//    		System.out.println( i++ + " of " + IDBPredicates.size());
    		if(!p.equals("Q") && isUnfoldable(p, result)){
   				result = optimizer.condensate( result );
   				result = this.saturate(result, new SelectionFunctionUnfoldGreedy(p), "G", tracker );
    		}
    	}
		return result;
	}
	
	/** Decides if a predicate is unfoldable */
	protected boolean isUnfoldable(String p, ArrayList<Clause> clauses){
		for(Clause c: clauses)
			if(c.getHead().getName().equals(p) && c.getBody().length > 1)
				for(Term t: c.getBody())
					if(t.getName().equals(p))
						return false;
		return true;
	}

	public ArrayList<Clause> saturate(ArrayList<Clause> clauses, SelectionFunction selectionFunction, String mode, ResolutionTracker tracker) {
		this.m_unprocessedClauses = clauses;
		this.m_clausesCanonicals = new ArrayList<String>();
		this.m_workedOffClauses = new ArrayList<Clause>();
		while (!m_unprocessedClauses.isEmpty()) {

			Clause givenClause = m_unprocessedClauses.remove(0);
			if(!givenClause.isTautology()){
				selectionFunction.selectAtoms(givenClause);
				
				m_workedOffClauses.add(givenClause);
				m_clausesCanonicals.add(givenClause.m_canonicalRepresentation);
				
				ArrayList<Clause> results = m_resolution.generateResolvents(givenClause, m_workedOffClauses, tracker);
				for (Clause resolvent : results) {
					if ((!resolvent.isTautology()) && 
						(!isRedundant(resolvent, mode, tracker))) { //"N" for SC check, "F" for forward subsumption
						m_unprocessedClauses.add(resolvent);
						m_clausesCanonicals.add(resolvent.m_canonicalRepresentation);
					}
				}
			}
		}
		return prune(selectionFunction);
	}

	/**
	 * Checks if a given clause is redundant. 
	 * If version == "N", then the method returns true if there is another previously generated clause that is equivalent up to variable renaming to the given clause
	 * If version != "N", then the method returns true if there is another previously generated clause that subsumes the given clause
	 * @param tracker 
	 */
	protected boolean isRedundant(Clause clause, String version, ResolutionTracker tracker) {
		//Checks if a given clause is contained in m_clausesCanonicals based on its naive canonical representation
		if(m_clausesCanonicals.contains(clause.m_canonicalRepresentation))
			return true;
		else{
			for(Clause unprocessedClause: this.m_unprocessedClauses){
				if(version.equals("N")) {
					if(unprocessedClause.isEquivalentUpToVariableRenaming(clause))
						return true;
				}
				else {
					if(unprocessedClause.subsumes(clause, true) && tracker.isEligibleForSubsumptionBy( unprocessedClause, clause ) )
						return true;
				}
			}
			for(Clause workedOffClause: this.m_workedOffClauses){
				if(version.equals("N")) {
					if(workedOffClause.isEquivalentUpToVariableRenaming(clause))
						return true;
				}
				else {
					if(workedOffClause.subsumes(clause, true) && tracker.isEligibleForSubsumptionBy(workedOffClause, clause) )
						return true;
				}
			}
		}
		return false;
	}	
	
	/**
	 * Prunes the set of workedOffClauses based on a given selection function
	 * @param selectionFunction
	 * @return
	 */
	protected ArrayList<Clause> prune(SelectionFunction selectionFunction) {
		ArrayList<Clause> result = new ArrayList<Clause>();
		m_clausesCanonicals.clear();
		for (Clause c : this.m_workedOffClauses) 
			if (!selectionFunction.isToBePruned(c)) {
				result.add(c);
				m_clausesCanonicals.add(c.m_canonicalRepresentation);
			}
		return result;
	}
}