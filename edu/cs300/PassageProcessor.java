package edu.cs300;
import CtCILibrary.*;
import edu.cs300.LWResponse;
import edu.cs300.SearchRequest;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.*;

public class PassageProcessor{

   static {
        System.loadLibrary("system5msg");
    }

    /*Gets all words out of the given file and returns an arraylist of these words*/ 
    public static ArrayList<String> getWords(File fileNm) 
    {
        ArrayList<String> words = new ArrayList<String>();
        try{
        Scanner in = new Scanner(new BufferedReader(new FileReader(fileNm)));
        //in.useDelimiter("[\\s*.,:\";]+");
        in.useDelimiter("[^a-zA-Z\'-]+");
        while (in.hasNext()) { 
            String theWord = in.next();
            if (theWord.length() > 3){
            if(theWord.matches("[a-zA-Z]+")){
            words.add(theWord.toLowerCase()); 
            }
        }
        }
        in.close();
        }
        catch(Exception e){
            System.err.println("Exception occurred trying to read words from file.");
            System.exit(0);
        }
        return words;
    }


    public static void main(String args[]){ 
    ArrayList<File> theFiles = new ArrayList<File>();
    ArrayList<String> fileNms = new ArrayList<String>();
    try
    {
      BufferedReader reader = new BufferedReader(new FileReader("/home/dafletemier/dafletemier/passages.txt"));
      //BufferedReader reader = new BufferedReader(new FileReader("/home/dafletemier/dafletemier/passages.txt"));
      String line;
      //Here we read each line of passages.txt for a name of a text file. Then we add it to a list of files and a list of file names
      while ((line = reader.readLine()) != null)
      {
            File theFile = new File("/home/dafletemier/dafletemier/"+line);
            if(theFile.exists()){
            theFiles.add(theFile);
            fileNms.add(line);
            }
            else{
                System.err.println("File "+line+" doesn't exist");
            }
      }
      reader.close();
    }
    catch (Exception e)
    {
      System.err.println("Exception occurred trying to read passages.txt");
      e.printStackTrace();
    }

    if(theFiles.size()==0) //print error messag and exit if no files provided.
    {
        System.err.println("No passages were provided. Exiting");
        System.exit(0);
    }

    /*for (int counter = 0; counter < fileNms.size(); counter++) { 		      
        System.out.println(fileNms.get(counter)); 		
    }   	*/
    //Here we create an an array list of array lists that contains all the words in each passage
    ArrayList<ArrayList<String>> samples = new ArrayList<ArrayList<String>>();  
    for(int i=0; i<fileNms.size(); i++){
        samples.add(getWords(theFiles.get(i)));
        //System.out.println(samples.get(i));
    }
    ArrayBlockingQueue workers[] = new ArrayBlockingQueue[fileNms.size()];
    ArrayBlockingQueue resultsOutputArray=new ArrayBlockingQueue(fileNms.size()*10);
    for (int i=0;i<fileNms.size();i++) {
        workers[i] = new ArrayBlockingQueue(10);
     }
     //starting all threads and adding each worker to a list of workers
     ArrayList<Worker> workersList = new ArrayList<Worker>();
     for (int i=0;i<fileNms.size();i++) {
        Worker aWorker = new Worker(samples.get(i), fileNms.get(i), i, workers[i], resultsOutputArray);
        aWorker.start();
        workersList.add(aWorker);
     }
     while(true){
         //Read a prefix request message and put it on each worker. 
        SearchRequest aRequest = new MessageJNI().readPrefixRequestMsg();
        //SearchRequest has requestID and prefix
        try {
            System.out.println("**prefix("+aRequest.requestID+") "+aRequest.prefix+" received");
            for (int i=0;i<fileNms.size();i++) {
            workers[i].put(aRequest);
            }
          } catch (InterruptedException e) {};
          //If the request isn't telling PP to terminate, write the response messages by taking results off of the Array blocking queue
          if(aRequest.requestID != 0){
          int counter=0;
          while (counter<fileNms.size()){
            try {
              LWResponse results = (LWResponse)resultsOutputArray.take();
              new MessageJNI().writeLongestWordResponseMsg(results.prefixID, results.prefix, results.passageIndex, results.passageName,results.longestWord, fileNms.size(),results.present); 
              counter++;
            } catch (InterruptedException e) {};
          }
        }
        //If the SM told PP to terminate, join all threads and terminate.
        else {
        try{
        for(int i = 0; i < workersList.size(); i++)
        {
           workersList.get(i).join();
        }
        System.out.println("Terminating...");
        System.exit(0);
        }
        catch(Exception ex){
            System.err.println("Caught exception" +ex);
        }
    }
       
     }

    }
}