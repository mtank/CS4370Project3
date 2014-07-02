
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.Boolean.*;
import static java.lang.System.out;

/****************************************************************************************
 * This class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus join.  The insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table
       implements Serializable
{
    /** Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /** Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key. 
     */
    private final String [] key;

    /** Index into tuples (maps key to tuple number).
     */
    private final Map <KeyType, Comparable []> index;

    //----------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList <> ();
        index     = new TreeMap <> ();       // also try BPTreeMap, LinHashMap or ExtHashMap
    } // constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuple      the list of tuples containing the data
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key,
                  List <Comparable []> _tuples)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        index     = new TreeMap <> ();       // also try BPTreeMap, LinHashMap or ExtHashMap
    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param name        the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     */
    public Table (String name, String attributes, String domains, String _key)
    {
        this (name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    //----------------------------------------------------------------------------------
    // Public Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes  the attributes to project onto
     * @return  a table of projected tuples
     */
    public Table project (String attributes)
    {
        out.println ("RA> " + name + ".project (" + attributes + ")");
        String [] attrs     = attributes.split (" ");
        Class []  colDomain = extractDom (match (attrs), domain);
        String [] newKey    = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs;

        List <Comparable []> rows = new ArrayList<>();

        //extract the columns needed from the tuples and add them to the list rows
        tuples.stream().forEach((tuple) -> {
            rows.add(this.extract(tuple, attrs));
        });
        
        return new Table (name + count++, attrs, colDomain, newKey, rows);
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t -> t[movie.col("year")].equals (1977))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public Table select (Predicate <Comparable []> predicate)
    {
        //Michael wrote this masterful code
        out.println ("RA> " + name + ".select (" + predicate + ")");

        List <Comparable []> rows = null;

        //data is in tuples
        try{
            //try to run the tuples through a stream
            //filter based on the predicate
            //and add to a list
            //may not need to b rows=... and can add it to the list in the collect statement
            rows = tuples.stream().filter(predicate).collect(Collectors.toList());
        }catch(Exception e){
            //unexpected exception because I don't know much on streams
            //output a message saying were as well as a stack trace
            System.out.println("Error in select(Predicate <Comparable []> predicate)");
            e.printStackTrace();
        }
        return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value).  Use an index
     * (Map) to retrieve the tuple with the given key value.
     *
     * @param keyVal  the given key value
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table select (KeyType keyVal)
    {
        //Michael wrote this amazingly awesome code as well (other than what Dr. Miller wrote)
        out.println ("RA> " + name + ".select (" + keyVal + ")");

        List <Comparable []> rows = null;

        try{
            //getting the tuple [] that satisfies the keyVal
            Comparable [] tempTuples = index.get(keyVal);
            if(tempTuples.length > 0){
                //if the keyVal exists, initialize the List rows and add the tuple [] to rows
                rows = new ArrayList <> ();
                rows.add(tempTuples);
            }
        }catch(Exception e){
            //potentially unexpected exception
            //same jazz as above with method name and stack trace
           System.out.println("Error in select(KeyType keyVal)");
           e.printStackTrace();
        }

        return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * @param table2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public Table union (Table table2)
    {
        out.println ("RA> " + name + ".union (" + table2.name + ")");
        if (! compatible (table2)) return null;
        
        Table resultTable = new Table (name + count++, attribute, domain, key);
        
        // insert tuples of current table
        tuples.stream().forEach((tuple) -> {
            resultTable.insert(tuple);
        });
        
        // checks table2 for unique tuples
        table2.tuples.stream().forEach((tuple1) -> {
            boolean exists = false;
            for (Comparable[] tuple : tuples) {
                if (tuple1 == tuple) {
                    exists = true;                
                }
            }
            // adds tuples to the resultTable
            if (!exists) {
                resultTable.insert(tuple1);
            }
        });
        
        return resultTable;
    } // union

    /************************************************************************************
     * Take the difference of this table and table2.  Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * @param table2  The rhs table in the minus operation
     * @return  a table representing the difference
     */
    public Table minus (Table table2)
    {
        out.println ("RA> " + name + ".minus (" + table2.name + ")");
        if (! compatible (table2)) return null;

        Table resultTable = new Table (name + count++, attribute, domain, key);
        
        for (Comparable[] tuple : tuples) {
            boolean exists = false;
            for (Comparable[] tuple1 : table2.tuples) {
                // checks if the tuple exists in table 2
                if (tuple == tuple1) {
                    exists = true;
                    break;
                }
            }
            // if the tuple doesn't exist in table 2, it's added to the resultTable
            if (!exists) {
                resultTable.insert(tuple);
            }
        }
        
        return resultTable;
    } // minus

    /************************************************************************************
     * Join this table and table2 by performing an equijoin.  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.
     *
     * #usage movie.join ("studioNo", "name", studio)
     * #usage movieStar.join ("name == s.name", starsIn)
     *
     * @param attribute1  the attributes of this table to be compared (Foreign Key)
     * @param attribute2  the attributes of table2 to be compared (Primary Key)
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {
        out.println ("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", "
                                               + table2.name + ")");

        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");
        
        boolean conTNU = TRUE;
        
        //see if the input was formatted properly and if not we print an error
        if(t_attrs.length != u_attrs.length){
            conTNU = false;
            System.out.println("your attributes were crap and did not have the same number for either table.");
            return null;
        }
        
        //creates a length value for domains and attributes to create a temp result table
        int attrLen = this.attribute.length + table2.attribute.length;
        int domLen = this.domain.length + table2.domain.length;
        //int keyLen = this.key.length + table2.key.length;
        
        //use the new lengths and create temp attr array and temp domain array
        String [] jtAttrs = new String[attrLen];
        Class [] jtDom = new Class[domLen];
        //String [] jtKey = new String[keyLen];
        
        //adds the attrs from the first table to our new attribute list
        int attrPos = 0;
        for (String attribute1 : this.attribute) {
            jtAttrs[attrPos] = attribute1;
            attrPos++;
        }
        //this one fills the new attr arraylist with the attributes from the second table
        //,but renames them if they are the same as one in the first table
        for (String attribute1 : table2.attribute) {
            for (int i = 0; i < this.attribute.length; i++) {
                if (attribute1.equals(this.attribute[i])) {
                    String tempAttr = attribute1 + '2';
                    jtAttrs[attrPos] = tempAttr;
                    for(int k = 0; k < u_attrs.length; k++){
                        if(u_attrs[k].equals(attribute1)){
                            u_attrs[k] = tempAttr;
                            break;
                        }
                    }
                    break;
                } else if (i == (this.attribute.length - 1)) {
                    jtAttrs[attrPos] = attribute1;
                }
            }
            attrPos++;
        }
        
        //fill the new domain arraylist with the domains from the first and second table
        int domPos = 0;
        for (Class domain1 : this.domain) {
            jtDom[domPos] = domain1;
            domPos++;
        }
        for (Class domain1 : table2.domain) {
            jtDom[domPos] = domain1;
            domPos++;
        }
        
        //create our temporary result table using the combined attribute and domain arrays
        Table result = new Table ((name + count++), jtAttrs, jtDom, key);
	Table tempTable = result;
        
        //cartesian product of the 2 arrays
        for(int i = 0; i < this.tuples.size(); i++){
            for(int m = 0; m < table2.tuples.size(); m++){
                Comparable[] tempTup = new Comparable[result.attribute.length];
                System.arraycopy(this.tuples.get(i), 0, tempTup, 0, this.tuples.get(i).length);
                System.arraycopy(table2.tuples.get(m), 0, tempTup, this.tuples.get(i).length, table2.tuples.get(m).length);
                tempTable.insert(tempTup);
            }
        }

	//fill our result table
	for (int j = 0; j < t_attrs.length; j++) {
	    //grab our column from first table
	    int temp1 = tempTable.col(t_attrs[j]);
	    int temp2 = tempTable.col(u_attrs[j]);e
	    for (int m = 0; m < tempTable.tuples.size(); m++) {
		//grab each tuple from our cartesian table
		Comparable[] tempTuple = tempTable.tuples.get(m);
		Comparable tempVal = tempTuple[tempTable.col(u_attrs[j])];
		Table temp = tempTable.select(p -> p[temp1].equals(temp2));
		//tempTable = temp;
		out.println ();
		temp.print();
		for (int h = 0; h > temp.tuples.size(); h++) {
		    result.insert(temp.tuples.get(h));
		}
	    }

	}

        return result;
    } // join

    /************************************************************************************
     * Return the column position for the given attribute name.
     *
     * @param attr  the given attribute name
     * @return  a column position
     */
    public int col (String attr)
    {
        for (int i = 0; i < attribute.length; i++) {
           if (attr.equals (attribute [i])) return i;
        } // for

        return -1;  // not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup  the array of attribute values forming the tuple
     * @return  whether insertion was successful
     */
    public boolean insert (Comparable [] tup)
    {
        out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            index.put (new KeyType (keyVal), tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /************************************************************************************
     * Get the name of the table.
     *
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName

    /************************************************************************************
     * Print this table.
     */
    public void print ()
    {
        out.println ("\n Table " + name);
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        for (Map.Entry <KeyType, Comparable []> e : index.entrySet ()) {
            out.println (e.getKey () + " -> " + Arrays.toString (e.getValue ()));
        } // for
        out.println ("-------------------");
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory. 
     *
     * @param name  the name of the table to load
     */
    public static Table load (String name)
    {
        Table tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream (new FileInputStream (DIR + name + EXT));
            tab = (Table) ois.readObject ();
            ois.close ();
        } catch (IOException ex) {
            out.println ("load: IO Exception");
            ex.printStackTrace ();
        } catch (ClassNotFoundException ex) {
            out.println ("load: Class Not Found Exception");
            ex.printStackTrace ();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save ()
    {
        try {
            ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (DIR + name + EXT));
            oos.writeObject (this);
            oos.close ();
        } catch (IOException ex) {
            out.println ("save: IO Exception");
            ex.printStackTrace ();
        } // try
    } // save

    //----------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (Table table2)
    {
        if (domain.length != table2.domain.length) {
            out.println ("compatible ERROR: table have different arity");
            return false;
        } // if
        for (int j = 0; j < domain.length; j++) {
            if (domain [j] != table2.domain [j]) {
                out.println ("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column  the array of column names
     * @return  an array of column index positions
     */
    private int [] match (String [] column)
    {
        int [] colPos = new int [column.length];

        for (int j = 0; j < column.length; j++) {
            boolean matched = false;
            for (int k = 0; k < attribute.length; k++) {
                if (column [j].equals (attribute [k])) {
                    matched = true;
                    colPos [j] = k;
                } // for
            } // for
            if ( ! matched) {
                out.println ("match: domain not found for " + column [j]);
            } // if
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t       the tuple to extract from
     * @param column  the array of column names
     * @return  a smaller tuple extracted from tuple t 
     */
    private Comparable [] extract (Comparable [] t, String [] column)
    {
        Comparable [] tup = new Comparable [column.length];
        int [] colPos = match (column);
        for (int j = 0; j < column.length; j++) tup [j] = t [colPos [j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the type of
     * each value to ensure it is from the right domain. 
     *
     * @param t  the tuple as a list of attribute values
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     */
    private boolean typeCheck (Comparable [] t)
    { 
	if (t.length != attribute.length) return false;
        
	for (int i = 0; i < domain.length; i++) {
	    
	    // checks t's type and compares it to the current domain
	    if (t[i].getClass().equals(domain[i].getClass())) {
		
		return false;

	    } // if
	} // for

	return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos the column positions to extract.
     * @param group  where to extract from
     * @return  the extracted domains
     */
    private Class [] extractDom (int [] colPos, Class [] group)
    {
        Class [] obj = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            obj [j] = group [colPos [j]];
        } // for

        return obj;
    } // extractDom

} // Table class

