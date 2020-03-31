package CtCILibrary;

import java.util.ArrayList;
import java.util.Queue;
import java.util.*;



/* Implements a trie. We store the input list of words in tries so
 * that we can efficiently find words with a given prefix. 
 */ 
public class Trie
{
    // The root of this trie.
    private TrieNode root;
    int length;
    String longestWString;
    /* Takes a list of strings as an argument, and constructs a trie that stores these strings. */
    public Trie(ArrayList<String> list) {
        root = new TrieNode();
        for (String word : list) {
            root.addWord(word);
        }
        length = 0;
    }  
    

    /* Takes a list of strings as an argument, and constructs a trie that stores these strings. */    
    public Trie(String[] list) {
        root = new TrieNode();
        for (String word : list) {
            root.addWord(word);
        }
        length = 0;
    }    

    /* Checks whether this trie contains a string with the prefix passed
     * in as argument.
     */
    public boolean contains(String prefix,boolean exact) {
        TrieNode lastNode = root;
        int i = 0;
        for (i = 0; i < prefix.length(); i++) {
            lastNode = lastNode.getChild(prefix.charAt(i));
            if (lastNode == null) {
                return false;	 
            }
        }
        return !exact || lastNode.terminates();
    }

    public String DFS(TrieNode current, Queue<Character>queue, int depth){ 
        TrieNode check = current;
        for(int i = 0; i < 26; i++){
           check = current;
           char theChar = (char)(i+97);
            check = check.getChild(theChar);
            
            System.out.println(theChar);
          if(check != null){
            //should be ascii value of the i 
           // queue.add(check.getChar());
            queue.add(theChar);
            depth = depth + 1;

        if (check.terminates()){
            String result = "";
            //for(int j = 0; j < depth; j++){
            while(queue.peek()!=null){
                result = result + Character.toString((char)queue.remove()); 
            }
            if (depth > length){
                length = depth;
                longestWString = result;
            }
        }
        else  return DFS(check, queue, depth);
        }
        }
        return longestWString;
      }

      public void DFS(TrieNode current, String theWord, int depth){ 
        TrieNode check = current;
        for(int i = 0; i < 26; i++){
           check = current;
           char theChar = (char)(i+97);
            check = check.getChild(theChar);
          if(check != null){
            if (check.terminates()){
                //longest word must be <= 100 chars
                if (depth >= length && depth<=100){
                    length = depth;
                    longestWString = theWord +Character.toString(theChar);
                }
            }
            else DFS(check, theWord + Character.toString(theChar), depth + 1);
        }
        }
        
      }


    public String longestWord(String prefix)
    {
        //Queue<Character> queue = new LinkedList<>();
        longestWString = "";
        length = 0;
        TrieNode lastNode = root;
        int i = 0;
        for (i = 0; i < prefix.length(); i++) {
            lastNode = lastNode.getChild(prefix.charAt(i));
            if (lastNode == null) {
                return null;	 
            }
        }
        //at this point we know that there is a word with this prefix
        DFS(lastNode, "", 0);
        //return prefix + DFS(lastNode, queue, 0);
        return prefix +longestWString; 
    }
    
    public boolean contains(String prefix) {
    	return contains(prefix, false);
    }
    
    public TrieNode getRoot() {
    	return root;
    }
}
