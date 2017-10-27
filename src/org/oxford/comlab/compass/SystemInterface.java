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

package org.oxford.comlab.compass;
import java.io.File;

/**
 * Systems need to implement this interface in order for the CompletenesAnalyser class to be able to communicate with them. The completeness analyser class needs to know how to 
 * load a test (TBox and Data), load a query to the system, run a query and finally clear the loaded scenario. This is important for the method to work correctly, i.e., previously 
 * loaded tests need to be cleared before next ones are loaded. In brief the method that is implemented in the CompletenesAnalyser class is the following:
 * 
 *  1: Call initializeSystem() ++
 *  2: Parse datasetFolder (see CompletenesAnalyser) to see how many queries there are
 *  3: For each query q
 *  4: 		load q to system
 *  5:  	parse Testing Unit folder for q
 *  6:		For each Testing Unit
 *  7:	 		loadTestToSystem() ++
 *  8:	 		runLoadedQuery() ++
 *  9:   		check if at least 1 answer was returned
 * 10:	 		clearRepository() ++
 * 11: shutdown() ++
 */
public interface SystemInterface {
	
	/**
	 * Gives the system the opportunity to perform any initialisation it wants/needs
	 */
	public void initializeSystem() throws Exception;
	
	/**
	 * Gives the system the opportunity to shutdown in case it needs to release resources etc.
	 */
	public void shutdown();

	/**
	 * Loads a test to the system. More precisely, it loads the TBox and the Testing Unit (ABox).
	 * 
	 * @param parser
	 * @param f
	 * @throws Exception
	 */
	public void loadTestToSystem(String ontologyFile, File aBox) throws Exception;
	
	/**
	 * The system that implements this interface should load the i-th query to the system. For "n" queries queryIndex runs from 0 to n-1
	 * 
	 * @param queriesPath
	 * @return
	 */
	public void loadQuery( int queryIndex ) throws Exception;
	
	/**
	 * The system that implements this interface should create and load an instance retrieval query specified by the symbol atomSymbol, while symbolType denotes whether the 
	 * input symbol is a concept or a role atom. The system should take atomSymbol and create a query in the format suitable for querying the system (e.g., SPARQL, SeRQL, etc.)
	 * 
	 *  For example if atomSymbol==hasParent, symbolType==2 and the system support SPARQL then the system can create the following query
	 *  
	 *  "SELECT DISTINCT ?X ?Y WHERE { ?X < atomSymbol > ?Y . }
	 * 
	 * @param queriesPath
	 * @return
	 */
	public void loadQuery( String atomSymbol, int symbolType ) throws Exception;
	
	/**
	 * This method is intended for running the previously loaded test and query and returning the number of answers that the system computed.
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public long runLoadedQuery( ) throws Exception;
	
	/**
	 * Clears the repository from the loaded test. Depending on the system this is an opportunity to close/release resources 
	 * that are related to query answering like query models.
	 * 
	 * @throws Exception
	 */
	public void clearRepository() throws Exception;
}
