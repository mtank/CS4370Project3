 
/*****************************************************************************************
 * @file  TestTupleGenerator.java
 *
 * @author   Sadiq Charaniya, John Miller
 */

import static java.lang.System.out;
import java.util.Random;

/*****************************************************************************************
 * This class tests the TupleGenerator on the Student Registration Database defined in the
 * Kifer, Bernstein and Lewis 2006 database textbook (see figure 3.6).  The primary keys
 * (see figure 3.6) and foreign keys (see example 3.2.2) are as given in the textbook.
 */
public class TestTupleGenerator
{
    /*************************************************************************************
     * The main method is the driver for TestGenerator.
     * @param args  the command-line arguments
     */
    public static void main (String [] args)
    {
        TupleGenerator test = new TupleGeneratorImpl ();

        test.addRelSchema ("Student",
                           "id name address status",
                           "Integer String String String",
                           "id",
                           null);
        
        test.addRelSchema ("Professor",
                           "id name deptId",
                           "Integer String String",
                           "id",
                           null);
        
        test.addRelSchema ("Course",
                           "crsCode deptId crsName descr",
                           "String String String String",
                           "crsCode",
                           null);
        
        test.addRelSchema ("Teaching",
                           "crsCode semester profId",
                           "String String Integer",
                           "crcCode semester",
                           new String [][] {{ "profId", "Professor", "id" },
                                            { "crsCode", "Course", "crsCode" }});
        
        test.addRelSchema ("Transcript",
                           "studId crsCode semester grade",
                           "Integer String String String",
                           "studId crsCode semester",
                           new String [][] {{ "studId", "Student", "id"},
                                            { "crsCode", "Course", "crsCode" },
                                            { "crsCode semester", "Teaching", "crsCode semester" }});

        String [] tables = { "Student", "Professor", "Course", "Teaching", "Transcript" };
        
        int tups [] = new int [] { 10000, 1, 1, 1, 1 };
    
        long sumJoin = 0;
        int runCount = 1;

        for (int i = 0; i < 20; i++) {
            Comparable[][][] resultTest = test.generate(tups);

//        for (int i = 0; i < resultTest.length; i++) {
//            out.println (tables [i]);
//            for (int j = 0; j < resultTest [i].length; j++) {
//                for (int k = 0; k < resultTest [i][j].length; k++) {
//                    out.print (resultTest [i][j][k] + ",");
//                } // for
//                out.println ();
//            } // for
//            out.println ();
//        } // for
            String student = "id name address status";
            String domain = "Integer String String String";

            String transAttr = "studId crsCode semester grade";
            String transDom = "Integer String String String";

            Table table1 = new Table("Student", student, domain, "id");
            Table table2 = new Table("Transcript", transAttr, transDom, "studId");

            //inserts each tuple into the table
            for (int k = 0; k < resultTest[0].length; k++) {
                table1.insert(resultTest[0][k]);
            }

            for (int x = 0; x < resultTest[4].length; x++) {
                table2.insert(resultTest[4][x]);
            }
	    
	    Random gen = new Random();
	    int idM = gen.nextInt(1000000);
	    KeyType tempType = new KeyType(idM);
	    
            long startTime = System.currentTimeMillis() * 10^5;
	    System.out.println(startTime);
            //Table result1 = table1.indexJoin("id", "studId", table2);
	    //System.out.println(endTime);
             Table result2 = table1.select(tempType);
             long endTime = System.currentTimeMillis() * 10^5;
	    

            sumJoin = sumJoin + (endTime - startTime);
            System.out.println("run time: " + (endTime - startTime));
            System.out.println("run number: " + runCount);
            runCount++;
	    
        }

        System.out.println("Join search avg: " + sumJoin / 20);
        System.out.println("Join search std dev: " + (sumJoin / 20) / Math.sqrt(20));
    }
    } // TestTupleGenerator

