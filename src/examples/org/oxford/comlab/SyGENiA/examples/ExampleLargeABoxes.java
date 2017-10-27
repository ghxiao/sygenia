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

package org.oxford.comlab.SyGENiA.examples;

import org.oxford.comlab.Configuration;
import org.oxford.comlab.Configuration.AssertionDuplicationStrategy;
import org.oxford.comlab.Configuration.TestSuiteType;
import org.oxford.comlab.aBoxGeneration.LargeABoxGenerator;

/**
 * This class gives examples on HOW the PERFORMANSS class can be used to generate large ABoxes for given TBoxes and conjunctive queries. Firstly, we create a configuration 
 * that is needed by the generation method. There are mainly two important parameters that need to be specified. This configuration specifies the number of individuals that the method will consider when creating assertions (the size of the pool
 * of individuals).  
 * 
 * @author gios
 *
 */
public class ExampleLargeABoxes {
	
	public static void main(String[] args) throws Exception{
		
		String userDir = System.getProperty( "user.dir" );
		System.out.println( "working in directory: " + userDir );

		/** Some Configuration need to be passed */
		Configuration c = new Configuration();
		c.testSuiteType = TestSuiteType.STRICT;
		
		/** This controls the number of individuals in the pool that the algorithm will use to generate assertions */
		c.totalNumberOfIndividuals = 20000;
		
		/** We can use as a threshold for the generation process the number of assertions that have been generated so far or 
		 * the total number of certain answers that have been generate */
//		c.assertionDuplicationStrategy = AssertionDuplicationStrategy.TOTAL_NUMBER_OF_ANSWERS;
		c.assertionDuplicationStrategy = AssertionDuplicationStrategy.TOTAL_NUMBER_OF_ASSERTIONS;
		
		buildABoxesForAllLUBMQueries( userDir, c );
		buildABoxesForAllGALENQueries( userDir, c );
	}

	private static void buildABoxesForAllLUBMQueries(String path, Configuration c) throws Exception {
	
		String ontologyFile = "file:" + path + "/examples/ontologies/LUBM/univ-bench.owl";
		String personalisedPath = path + "/examples/ontologies/LUBM/Queries";
		
		LargeABoxGenerator syntheticGenerator = new LargeABoxGenerator( c );

		String queryFile;
		
		long numberOfAssertionsOfABoxForEachQuery = 1000;
		
		queryFile = personalisedPath + "/Query_01.txt";
		System.out.println( "Building ABox for query 1 with " +  numberOfAssertionsOfABoxForEachQuery + " of assertions" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_02.txt";
		System.out.println( "\nBuilding ABox for query 2" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_03.txt";
		System.out.println( "\nBuilding ABox for query 3" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_04.txt";
		System.out.println( "\nBuilding ABox for query 4" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_05.txt";
		System.out.println( "\nBuilding ABox for query 5" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_06.txt";
		System.out.println( "\nBuilding ABox for query 6" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_07.txt";
		System.out.println( "\nBuilding ABox for query 7" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_08.txt";
		System.out.println( "\nBuilding ABox for query 8" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_09.txt";
		System.out.println( "\nBuilding ABox for query 9" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_10.txt";
		System.out.println( "\nBuilding ABox for query 10" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_11.txt";
		System.out.println( "\nBuilding ABox for query 11" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_12.txt";
		System.out.println( "\nBuilding ABox for query 12" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_13.txt";
		System.out.println( "\nBuilding ABox for query 13" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );

		queryFile = personalisedPath + "/Query_14.txt";
		System.out.println( "Building ABox for query 14" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfAssertionsOfABoxForEachQuery );
		
	}
	
	private static void buildABoxesForAllGALENQueries(String path, Configuration c) throws Exception {
		
		String ontologyFile = "file:" + path + "/examples/ontologies/Galen/moduleU_JIA_procedures.owl";
		String personalisedPath = path + "/examples/ontologies/Galen/Queries";
		
		LargeABoxGenerator syntheticGenerator = new LargeABoxGenerator( c );

		String queryFile;
		
//		long[] numberOfElements = {500, 1000, 2000, 4000};
		long[] numberOfElements = {1000};
		
		queryFile = personalisedPath + "/Query_01.txt";
		System.out.println( "\nBuilding ABox for query 1" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfElements[0] );

		queryFile = personalisedPath + "/Query_02.txt";
		System.out.println( "\nBuilding ABox for query 2" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfElements[0] );

		queryFile = personalisedPath + "/Query_03.txt";
		System.out.println( "\nBuilding ABox for query 3" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfElements[0] );

		queryFile = personalisedPath + "/Query_04.txt";
		System.out.println( "\nBuilding ABox for query 4" );
		syntheticGenerator.createSyntheticABox( ontologyFile, queryFile, numberOfElements[0] );
	}
}
