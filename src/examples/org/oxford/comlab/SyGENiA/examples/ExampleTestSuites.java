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
import org.oxford.comlab.Configuration.TestSuiteType;
import org.oxford.comlab.aBoxGeneration.TestSuiteGenerator;

public class ExampleTestSuites {
	
	public static void main(String[] args) throws Exception{
		
		String userDir = System.getProperty( "user.dir" );
		
		/** 
		 * Create a configuration for the ABox generation algorithm. See Configuration class for more details. 
		 */
		Configuration c = new Configuration();
		c.testSuiteType = TestSuiteType.STRICT;
		
		/** 
		 * Extract Test Suites for every query of the LUBM TBox 
		 */
		buildTestSuiteForAllLUBMQueries( userDir, c );

		/** 
		 * Extract Test Suite for the 4 queries of this Galen module 
		 */
		buildTestSuiteForAllGalenQueries( userDir, c );

	}
	
	private static void buildTestSuiteForAllLUBMQueries(String path, Configuration c) throws Exception {
		
		path = path.replace('\\', '/' );			//for windows to replace \ with /
		path = "/" + path;							//for windows. physical paths do not start with /, e.g., D:/..., so we need to make it /D:/.. 
		path = path.replaceAll( "//", "/" );		//but if it is linux then we will have //user/, so we need to change // to /.
		
		String ontologyFile = "file:" + path + "/examples/ontologies/LUBM/univ-bench.owl";
		System.out.println( "extracting Testing Bases for " + ontologyFile );
		
		TestSuiteGenerator testingBaseGenerator = new TestSuiteGenerator( c );		
		
		String queryFile;
		for( int i=1 ; i<=14 ; i++ ){
			if ( c.testSuiteType == TestSuiteType.GENERAL && i==9 ){
				System.out.println( "No GTB for query 9 of LUBM can be created" );
				continue;
			}
			System.out.println( "Query " + i );
			
			if( i < 10 )
				queryFile = path + "/examples/ontologies/LUBM/Queries/Query_0" + i + ".txt";
			else 
				queryFile = path + "/examples/ontologies/LUBM/Queries/Query_" + i + ".txt";

			testingBaseGenerator.createTestSuite( ontologyFile, queryFile );
		}
	}
	
	private static void buildTestSuiteForAllGalenQueries(String path, Configuration c) throws Exception {
		
		String ontologyFile = "file:" + path + "/examples/ontologies/Galen/moduleU_JIA_procedures.owl";
		System.out.println( "extracting Testing Bases for " + ontologyFile );
		
		TestSuiteGenerator testingBaseGenerator = new TestSuiteGenerator( c );		
		
		String queryFile;
		for( int i=1 ; i<=4 ; i++ ){
			
			System.out.println( "Query " + i );
			queryFile = path + "/examples/ontologies/Galen/Queries/Query_0" + i + ".txt";
			testingBaseGenerator.createTestSuite( ontologyFile, queryFile );
		}
	}
	
}
