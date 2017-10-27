package org.oxford.comlab.requiem.rewriter;
import java.util.ArrayList;

import org.oxford.comlab.requiem.optimizer.Optimizer;
import org.oxford.comlab.requiem.resolutiontracking.ResolutionTracker;


public class Rewriter {

	protected static final TermFactory m_termFactory = new TermFactory(); 
	protected static final Saturator m_saturator = new Saturator(m_termFactory);
	protected static final Optimizer m_optimizer = new Optimizer(m_termFactory);
	
	public ArrayList<Clause> rewrite(ArrayList<Clause> clauses, String mode, ResolutionTracker tracker) throws Exception{
		
		ArrayList<Clause> saturation = new ArrayList<Clause>();
		ArrayList<Clause> rewriting = new ArrayList<Clause>();
	    	
    	//Prune irrelevant clauses based on the dependency graph from the query
    	clauses = m_optimizer.pruneWithDependencyGraph("Q", clauses);
    	
    	System.out.println("Clausified input ontology ("+ clauses.size() +" clauses)");
    	
    	//Saturation
    	saturation = m_saturator.saturate(clauses, new SelectionFunctionSaturate(), mode, tracker);
    	System.out.println("Saturation completed ("+ saturation.size() +" clauses)");
//    	for( Clause cl : saturation )
//    		System.out.println( cl );
    	
    	//Unfolding
    	if(mode.equals("N") || mode.equals("F"))
    		rewriting = m_saturator.unfoldNaively(saturation);
    	else
    		rewriting = m_saturator.unfoldGreedily(saturation, tracker);
    	System.out.println("Unfolding completed ("+ rewriting.size() +" clauses)");
    	
    	//Optimize
    	if(!mode.equals("N")){
	    	//Prune irrelevant clauses based on the dependency graph from the query
		    rewriting = m_optimizer.pruneWithDependencyGraph("Q", rewriting);
		    //Prune clauses containing AUX EDB predicates
	    	rewriting = m_optimizer.pruneAUX(rewriting);
			//Prune subsumed clauses
		    rewriting = m_optimizer.querySubsumptionCheck(rewriting, tracker);
		    //replace each query with its condensation
		    rewriting = m_optimizer.condensate(rewriting);
    	}
    	System.out.println("Pruning completed ("+ rewriting.size() +" clauses)");
    	
    	return rewriting;	    
	}	
}