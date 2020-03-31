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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ArrayList<String> getWords(File fileNm) 
    {
       // System.out.println((fileNm));
        //File theFile = new File("/home/dafletemier/dafletemier/"+fileNm);
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
            System.err.format("Exception occurred trying to read words from file.");
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
      while ((line = reader.readLine()) != null)
      {
            File theFile = new File("/home/dafletemier/dafletemier/"+line);
            if(theFile.exists()){
            theFiles.add(theFile);
            fileNms.add(line);
            }
            else{
                System.err.format("File "+line+" doesn't exist\n");
            }
      }
      reader.close();
    }
    catch (Exception e)
    {
      System.err.format("Exception occurred trying to read passages.txt");
      e.printStackTrace();
    }

    if(theFiles.size()==0) //print error messag and exit if no files provided.
    {
        System.err.format("No passages were provided. Exiting\n");
        System.exit(0);
    }

    /*for (int counter = 0; counter < fileNms.size(); counter++) { 		      
        System.out.println(fileNms.get(counter)); 		
    }   	*/

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

     ArrayList<Worker> workersList = new ArrayList<Worker>();
     for (int i=0;i<fileNms.size();i++) {
        Worker aWorker = new Worker(samples.get(i), fileNms.get(i), i, workers[i], resultsOutputArray);
        aWorker.start();
        workersList.add(aWorker);
     }
     while(true){
        SearchRequest aRequest = new MessageJNI().readPrefixRequestMsg();
        //SearchRequest has requestID and prefix
        try {
            System.out.println("**prefix("+aRequest.requestID+") "+aRequest.prefix+" received");
            for (int i=0;i<fileNms.size();i++) {
            workers[i].put(aRequest);
            }
          } catch (InterruptedException e) {};
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
            System.out.println("Caught exception" +ex);
        }
    }
       
     }

    }
}