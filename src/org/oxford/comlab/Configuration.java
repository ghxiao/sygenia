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

package org.oxford.comlab;

public class Configuration {
	public TestSuiteType testSuiteType = TestSuiteType.STRICT;
	public AssertionDuplicationStrategy assertionDuplicationStrategy = AssertionDuplicationStrategy.TOTAL_NUMBER_OF_ASSERTIONS;
	public StoreRewritingsToFile storeRewritingsToFile = StoreRewritingsToFile.FALSE;
	public long totalNumberOfIndividuals = 1000; 
	public QAAlgorithmClass qaAlgorithmClass = QAAlgorithmClass.strongly_faithful;
	
    /**
     * Determines the time of testing units that the test suite is going to contain.  
     */
    public static enum TestSuiteType {

    	/** Extracts a set of ABoxes called a Strict Test Suite (STB)---see ISWC paper. This set can be used to test a query answering 
    	 * system for completeness. If the system can handle this set of ABoxes it is guaranteed to be complete for any input dataset */
        STRICT,
        
        /** Extracts a set of ABoxes called General Test Suite (GTB)---see ISWC paper. This is a superset of STBs that contains more
         * ABoxes in order to provide a more accurate completeness degree.*/
        GENERAL
    }
    
    /**
     * Sets the Class of Query Answering systems for which we are generating the Testing Base  
     */
    public static enum QAAlgorithmClass {

        weakly_faithful,
        
        strongly_faithful,
        
        firstOrder_reproducible
    }
    
    /**
     * In case the PERFORMANCE class is used for extracting one large ABox and not testing bases one should be able to control the scaling-up/duplication process. 
     */
    public static enum AssertionDuplicationStrategy {

    	/** 
    	 * The duplication process stops when a specific maximum number of assertions, specified by the user, has been reached. Note that the 
    	 * actual assertions generated at the end might not match with the number of assertions requested. The method might be off-target at 
    	 * about 1-6 assertions depending on the query and the computed rewriting. 
    	 */
        TOTAL_NUMBER_OF_ASSERTIONS,
        
    	/** 
    	 * The duplication process stops when a specific maximum number of certain answers, specified by the user, has been reached. There 
    	 * same remark as above applies for the nunber of actual certain answers generated in total. 
    	 */
        TOTAL_NUMBER_OF_ANSWERS
    }
    /**
     * Determines whether the computed UCQ rewritings for q and T are to be written on disk.
     */
    public static enum StoreRewritingsToFile {
    	TRUE,
    	
    	FALSE
    }
}
