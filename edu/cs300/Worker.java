package edu.cs300;
import CtCILibrary.*;
import java.util.concurrent.*;
import java.util.ArrayList;
class Worker extends Thread{
  @SuppressWarnings({"rawtypes", "unchecked"})
  Trie textTrieTree;
  ArrayBlockingQueue prefixRequestArray;
  ArrayBlockingQueue resultsOutputArray;
  String id;
  int num;
  int numPassages;
  String passageName;

  public Worker(ArrayList<String> words,String id, int theID,ArrayBlockingQueue prefix, ArrayBlockingQueue results){
    this.textTrieTree=new Trie(words);
    this.prefixRequestArray=prefix;
    this.resultsOutputArray=results;
    this.id=id;
    this.num = theID; 
    this.passageName=id;//put name of passage here
  }
  //we might need to parse from the results output array

  public void run() {
    System.out.println("Worker-"+this.num+" ("+this.passageName+") thread started ...");
    while (true){
      try {
        SearchRequest req=(SearchRequest)this.prefixRequestArray.take();
        //break out of the while loop to escape run (so we can join threads) if request id is 0
        if(req.requestID == 0) break; 
        String prefix = req.prefix;
        String lngest = this.textTrieTree.longestWord(prefix);
        int fnd;
        if(lngest != null) fnd =1;
        else fnd = 0;
        if (fnd == 0){
          System.out.print("Worker-"+this.num+" "+req.requestID+":"+ prefix+" ==> not found \n");
          
          LWResponse response = new LWResponse(req.requestID, prefix, this.num, this.passageName, "----", fnd);
          this.resultsOutputArray.add(response);
        } else{
          System.out.print("Worker-"+this.num+" "+req.requestID+":"+ prefix+" ==> "+lngest+"\n");
          LWResponse response = new LWResponse(req.requestID, prefix, this.num, this.passageName, lngest, fnd);
          this.resultsOutputArray.add(response);
         
        }
      } catch(InterruptedException e){
        System.out.println(e.getMessage());
      }
    }
  }

}
