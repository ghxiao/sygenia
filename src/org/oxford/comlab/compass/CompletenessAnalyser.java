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

package org.oxford.comlab.compass;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * This class implements a completeness analyser method for analysing the completeness of a query answering system over a TBox "T" and query "q". The 
 * methods provided use a-priori knowledge about the way that the testing units of the testing base have been organised on disk by the COMPASS class. 
 * Each testing unit is first parsed, then loaded to the system and finally query q is performed over the system for the TBox T and currently considered 
 * testing unit. If the system returns at least one answer the testing unit is considered successful otherwise failed and a message is printed. System need 
 * to implement the SystemInterface interface which is used by this class to perform operations on the tested system
 * @author gios
 *
 */
public class CompletenessAnalyser {
	
	private ArrayList<String> loadQueryFolders(String datasetFolder){

        File dir = new File(datasetFolder);
        File[] queryFolders = dir.listFiles();
        TreeSet<String> orderedListOfFiles = new TreeSet<String>();
        if (queryFolders == null) {
            // Either dir does not exist or is not a directory
        	System.out.println("Cannot open pre-load directory: " + datasetFolder );
        	System.exit( 0 );
        } else {
            for (int i=0; i<queryFolders.length; i++) {
                // Get filename of file or directory
                if (queryFolders[i].isFile())
                	continue;
                try{
                	orderedListOfFiles.add( queryFolders[i].getAbsolutePath() );
                }catch (OutOfMemoryError e){
                	System.out.println( "System run out of memory. Exiting...");
                	System.exit( 0 );
                }
            }
        }
        ArrayList<String> allQueryFolders = new ArrayList<String>(orderedListOfFiles);
        return allQueryFolders;
	}
	
	public void doCompletenessAnalysisExperimentAllMappings(SystemInterface cqSystem, String ontologyFile, String datasetFolder) {
		
		try{ 
			cqSystem.initializeSystem();
			ArrayList<String> allQueryFolders = loadQueryFolders( datasetFolder );
			long timeStart = System.currentTimeMillis();
	 		for ( int i = 0; i<allQueryFolders.size() ; i++ ) {

	 			long timeQueryStart = System.currentTimeMillis();
	 			System.out.println( "***  Examining query " + (i+1) + " *** " + allQueryFolders.get( i ) );

				cqSystem.loadQuery( i );

				String queryDatasetFolder = allQueryFolders.get( i );
				File testingUnitsDir = new File( queryDatasetFolder );
				File[] testingUnits = testingUnitsDir.listFiles();
				int failedABoxes = 0;
				long totalNumberOfABoxes = 0;
				for( int k=0 ; k<testingUnits.length ; k++ ){
					System.out.println( "Examining ABoxes for inference pattern " + testingUnits[k].getName() );
					
					File[] aBoxesForTU = testingUnits[k].listFiles();
			        if( aBoxesForTU == null ){
			    	   // Either dir does not exist or is not a directory
			    	   System.out.println("Cannot open load directory: " + queryDatasetFolder );
			    	   return;
			        }
			        else {
			        	for (int j=0; j<aBoxesForTU.length; j++) {
		
			        		totalNumberOfABoxes++;
			        		cqSystem.loadTestToSystem( ontologyFile, aBoxesForTU[j] );
				        	long certainAnswers = cqSystem.runLoadedQuery( );
				        	if( certainAnswers < 1 ){
//				        		System.err.println( "Failed at ABox: " + aBoxesForTU[j].getName() + " of inference pattern " + testingUnits[k].getName());
				        		System.out.println( "Failed at ABox: " + aBoxesForTU[j].getName() );
				        		failedABoxes++;
				        	}
				        	else if( certainAnswers > 1 )
				    			System.err.println( "A STRANGE CASE HAPPENED ON " + aBoxesForTU[j].getName() );

				        	cqSystem.clearRepository();
			            }
			        }
				}
	        	float percentage = (float) (totalNumberOfABoxes-failedABoxes)/totalNumberOfABoxes;
	        	System.out.println( "\nFailed in " + failedABoxes + " out of " + totalNumberOfABoxes + " testing units for " + testingUnits.length + " inference patterns" );
	        	System.out.println( "Completeness: " + percentage );
	        	System.out.println( "Analysis of query " + (i+1) + " finished in " + (System.currentTimeMillis()-timeQueryStart) + " ms\n\n");
			}
	 		System.out.println( "Overall analysis finished in " + (System.currentTimeMillis()-timeStart) + " ms");
	        System.out.println( );
		}catch (Exception e){
			e.printStackTrace();
			cqSystem.shutdown();
		}
	}
	
	public void doCompletenessAnalysisExperimentInjective(SystemInterface cqSystem, String ontologyFile, String datasetFolder) throws Exception {

		cqSystem.initializeSystem();
		
		ArrayList<String> allQueryFolders = loadQueryFolders( datasetFolder );

 		for ( int i = 0; i<allQueryFolders.size() ; i++ ) {

 			System.out.println( "***  Examining query " + (i+1) + " ***");
			
			cqSystem.loadQuery( i ); 

			String queryFolder = allQueryFolders.get( i );
			File dir = new File( queryFolder );
			System.out.println( "Examining Query: " + queryFolder );
			File[] testingUnits = dir.listFiles();
	        if (testingUnits == null) {
	    	   // Either dir does not exist or is not a directory
	    	   System.out.println("Cannot open load directory: " + queryFolder );
	    	   return;
	        } else {
	        	int failedPatterns = 0;
	        	for (int j=0; j<testingUnits.length; j++) {

	        		cqSystem.loadTestToSystem( ontologyFile, testingUnits[j] );
		        	long certainAnswers = cqSystem.runLoadedQuery( );
		        	if( certainAnswers < 1 ){
		        		System.out.println( "Failed at testing unit: " + testingUnits[j].getName() );
		        		failedPatterns++;
		        	}
		        	cqSystem.clearRepository();
	            }
	        	float percentage = (float) (testingUnits.length-failedPatterns)/testingUnits.length;
	        	System.out.println( "\nFailed in " + failedPatterns + " out of " + testingUnits.length + " testing units" );
	        	System.out.println( "Completeness: " + percentage );
	        }
	        System.out.println( );
		}
 		cqSystem.shutdown();
	}
}
