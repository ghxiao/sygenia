package org.oxford.comlab.requiem;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.oxford.comlab.requiem.parser.ELHIOParser;
import org.oxford.comlab.requiem.resolutiontracking.ResolutionTracker;
import org.oxford.comlab.requiem.rewriter.Clause;
import org.oxford.comlab.requiem.rewriter.Rewriter;
import org.oxford.comlab.requiem.rewriter.TermFactory;

	
public class Requiem {
		
	protected static final TermFactory m_termFactory = new TermFactory(); 
	protected static final ELHIOParser m_parser = new ELHIOParser(m_termFactory);
	protected static final Rewriter m_rewriter = new Rewriter();
	
	protected Clause m_query;
	protected String m_mode;

//	public static void main(String[] args) throws Exception{
//		
//		String path = "/media/SHARE/JustificationTests/";
//		
//		String queryFileName = "Query_00.txt";
//		String ontologyName = "exponentialNonStrict.owl";
//				
//		String[] reqQrgs = new String[5];
//		reqQrgs[0] = path + queryFileName;
//		reqQrgs[1] = "file:" + path + ontologyName;
//		reqQrgs[2] = queryFileName + "_rewriting.txt";
//		reqQrgs[3] = "G";
//		reqQrgs[4] = "true";
//
//	}
	
	public Map<Clause, Set<Clause>> getDependencies() {
		return null;
	}
	
	/**
	 * @param args
	 * 0 - query file
	 * 1 - ontology file
	 * 2 - output path
	 * 3 - mode (N|F|G)
	 * @param tracker 
	 */
	public ArrayList<Clause> computeRewriting(String[] args, ResolutionTracker tracker) throws Exception{
		
		if(args.length == 5 && (args[3].equals("N") || args[3].equals("F") || args[3].equals("G"))){
			String queryFile = args[0];
			String ontologyFile = args[1];
//			String outputFilePrefix = args[2];
			m_mode = args[3];
			boolean printToFile = Boolean.parseBoolean( args[4] );
			
			ArrayList<Clause> original = m_parser.getClauses(ontologyFile);
//			for( Clause cl : original )
//				System.out.println( cl );
//			System.exit( 0 );
	    	m_query = ELHIOParser.getQueryFromFile(queryFile);
	    	if( m_query != null )
	    		original.add( m_query );
	    	else
	    		throw new Exception("Invalid query.");

			ArrayList<Clause> rewriting = m_rewriter.rewrite(original, m_mode, tracker);

	    	if( printToFile )
	    		printRewritingToFile(rewriting, queryFile, ontologyFile);
	    	
	    	return rewriting;
		}
		else
			throw new Exception("Use: java Requiem query.txt ontology.owl outPath mode(N|F|G)");
	}

	private static void printResultToFile(String outputFilestr, String queryFile, String ontologyFile, int rewritingSize, String query, ArrayList<Clause> rewriting) throws Exception{
		int counter = 0;
		File outputFile = new File(outputFilestr + counter + ".txt");
		while (outputFile.exists()) {
			counter++;
			outputFile = new File(outputFilestr + counter + ".txt");
		}
		
        FileWriter out = new FileWriter(outputFile);
        out.write("==================SUMMARY==================\n");
        out.write("Ontology file:             " + ontologyFile.substring(ontologyFile.lastIndexOf("/") + 1) + "\n"); 
        out.write("Query:                     " + query + "\n");
//        out.write("Running time:              " + time + " milliseconds \n");
		out.write("Size of the rewriting (queries):     " + rewritingSize + "\n");
		int size = 0;
		for(Clause c: rewriting){
			size += c.toString().length();
		}
		out.write("Size of the rewriting (symbols):     " + size + "\n");
		
		//System.out.println("Size of the rewriting (symbols): " + size + "\n");
		out.write("==================SUMMARY==================\n");
		Collections.sort(rewriting, new Comparator<Clause>(){
			public int compare(Clause c1, Clause c2){
			    return c1.m_canonicalRepresentation.compareTo(c2.m_canonicalRepresentation);
			}
		});
		int i = 0;
		for(Clause c: rewriting)
			out.write(i++ + ": " + c.m_canonicalRepresentation + "\n");
        out.close();
	}

	public Clause getQuery() {
		return m_query;
	}

	public void printRewritingToFile(ArrayList<Clause> rewrittenClauses, String queryFile, String ontologyFile) throws Exception {
		printResultToFile(queryFile + "_rewriting.txt" + "RQM" + m_mode + "-", queryFile, ontologyFile, rewrittenClauses.size(), m_query.m_canonicalRepresentation, rewrittenClauses);		
	}
}