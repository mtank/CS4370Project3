
/************************************************************************************
 * @file LinHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an array of buckets.
 */
public class LinHashMap <K, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int    nKeys;
        K []   key;
        V []   value;
        Bucket next;
        @SuppressWarnings("unchecked")
        Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
    } // Bucket inner class

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of home buckets (a power of 2, e.g., 4)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();
        mod1   = initSize;
        mod2   = 2 * mod1;
        
        //initialize the buckets in hTable to null buckets
        for(int t=0; t<initSize; t++)
            hTable.add(new Bucket(null));
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();
        for(int x = 0; x < hTable.size(); x++){//loop through the buckets
            Bucket temp = hTable.get(x);
            for(int z=0; z<temp.nKeys; z++){//loop through the values/keys
                enSet.add(new AbstractMap.SimpleEntry<>(temp.key[z], temp.value[z])); //add the key and value to the HashSet
            }
        }
            
        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        
        int i = h (key);
        if(i<split)
            i=h2(key);
        Bucket tmp = hTable.get(i);
        //make sure the bucket isn't empty
        //because that would be a major waste of time
        if(tmp.nKeys == 0)
            return null;
        
        //go through my buckets in table entry i
        while(tmp != null){
            for(int m=0; m<tmp.nKeys; m++){
                if(key.equals(tmp.key[m]))
                    return tmp.value[m]; //found it
            }//for loop over values in bucket
            tmp = tmp.next; // key not in this bucket so try the next one
        }//while bucket != null

        return null;//if we make it this far, it ain't in the table so return null
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        int i = h (key);
        if(i<split)
            i=h2(key);
        Bucket temp = hTable.get(i);
        if(temp.nKeys < SLOTS){
            //simple insert; no split needed
            //yay!
            temp.key[temp.nKeys] = key;
            temp.value[temp.nKeys] = value;
            temp.nKeys++;
        }else{
            //sadly we may need to split
            //boo
            hTable.add(new Bucket(null));
            //mod1++;
            //move pointer
            while(temp.next != null){
                temp = temp.next;
            }
            //check in the last bucket of the chain
            if(temp.nKeys < SLOTS){
                temp.key[temp.nKeys] = key;
                temp.value[temp.nKeys] = value;
                temp.nKeys++;
            }else{
                temp.next = new Bucket(null);
                temp = temp.next;
                temp.key[temp.nKeys]=key;
                temp.value[temp.nKeys]=value;
                temp.nKeys++;
            }//else
            //time for the actually splitting
//            for(int p=0; p<hTable.size();p++){
//                temp=hTable.get(p);
//                while(temp!=null){
//                    int oldNKeys = temp.nKeys;
//                    temp.nKeys=0;//so it does a simple insert when we insert
//                    for(int b=0; b<oldNKeys;b++){
//                        put(temp.key[b], temp.value[b]);
//                    }
//                }//while the bucket isn't null
//            }//for over hTable
            int numKeys = 0;
            for(int y =0; y<hTable.size();y++){
                Bucket bkt = hTable.get(y);
                do{
                    numKeys = numKeys + bkt.nKeys;
                    bkt=bkt.next;
                }while(bkt !=null);
            }
            double alpha = ((double)numKeys)/(SLOTS * mod1);
            if(alpha >= 1){
                Bucket temp2 = new Bucket(null);//replace the split
                Bucket temp3 = new Bucket(null);//the new bucket
                temp = hTable.get(split);//the bucket to split
                for(int p=0; p<temp.nKeys; p++){
                    int z = h2(temp.key[p]);
                    if(z == split){
                        if(temp2.next ==null){
                            temp2.next = new Bucket(null);
                            temp2 = temp2.next;
                        }
                        temp2.key[temp2.nKeys] = temp.key[p];
                        temp2.value[temp2.nKeys] = temp.value[p];
                        temp2.nKeys++;
                    }else{
                        if(temp3.next == null){
                            temp3.next = new Bucket(null);
                            temp3 = temp3.next;
                        }
                        temp3.key[temp3.nKeys] = temp.key[p];
                        temp3.value[temp3.nKeys] = temp.value[p];
                    }
                }
                if(split == mod1 -1 ){
                    mod1= mod1*2;
                    mod2= mod1*2;
                    split=0;
                }else{
                    split++;
                }
            }   
        }//else
        return null;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table. 
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * (mod1 + split);
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Linear Hashing)");
        out.println ("-------------------------------------------");

        for(int x=0; x<hTable.size();x++){
            out.print(x + ":");
            Bucket tmp = hTable.get(x);
            boolean chain = false; //assume no chain
            if(tmp.next!=null)
                chain=true;//if next exist, there is a chain
            if(chain){
                out.print("[ ");
                for(int z=0; z<SLOTS;z++){
                    out.print(tmp.key[z]);
                    if(SLOTS!=z+1)
                        out.print(", ");//there is another item
                    else
                        out.print(" ] --> ");//end of bucket, but another one is coming
                }//for
                out.print("[ ");
                for(int z=0;z<SLOTS;z++){
                    out.print(tmp.next.key[z]);
                    if(SLOTS!=z+1)
                        out.print(" ]");
                }
            }else{
                //only one bucket
                out.print("[ ");
                for(int z=0; z<SLOTS; z++){
                    out.print(tmp.key[z]);
                    if(SLOTS != z+1)
                        out.print(", ");
                }//for
                out.print(" ]");
            }
            out.println();
        }//for over hTable
        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key)
    {
        return key.hashCode () % mod1;
    } // h

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key)
    {
        return key.hashCode () % mod2;
    } // h2

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class, 11);
        int nKeys = 30;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 2) ht.put (i, i * i);
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // LinHashMap class

