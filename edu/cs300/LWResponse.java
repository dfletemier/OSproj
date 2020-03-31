package edu.cs300;

public class LWResponse {

    int prefixID; 
    String prefix; 
    int passageIndex; 
    String passageName;
    String longestWord;
    int present;


  public LWResponse(int pfixID, String pfix, int pIndex, String pName,String lWord, int pres){
    this.prefixID = pfixID;
    this.prefix = pfix;
    this.passageIndex = pIndex;
    this.passageName = pName;
    this.longestWord = lWord;
    this.present = pres;
  }

 
}
