package CS286;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyNaiveBayes {

	static ArrayList<String> hamSubjects = new ArrayList<String>();
	static ArrayList<String> spamSubjects = new ArrayList<String>();
	static List<String> trainHamSubjects = new ArrayList<String>();
	static List<String> trainSpamSubjects = new ArrayList<String>();
	static List<String> testHamSubjects = new ArrayList<String>();
	static List<String> testSpamSubjects = new ArrayList<String>();
	static Map<String,Integer> spamWordCount = new HashMap<String, Integer>();
	static Map<String,Integer> hamWordCount = new HashMap<String, Integer>();
	static Map<String,Double> spamWordProbabilityCount = new HashMap<String, Double>();
	static Map<String,Double> hamWordProbabilityCount = new HashMap<String, Double>();
	static int totalSpam=0,totalHam=0,totalVocab=0;
	static double spamProb=0.0,hamProb=0.0;
	static Set<String> uniqueWordSet = new HashSet<String>(); 
	final static double PARTITION = 0.6;
	
	public static void main(String[] args) {
		String type = args[0];
		if(type.equals("build")){
			String hamDirectory = args[1];
			String spamDirectory = args[2];
			String modelDirectory = args[3];
			listFilesForFolder(new File(hamDirectory),"ham");
			listFilesForFolder(new File(spamDirectory),"spam");
			createUniqueMap();
			createAndDumpModel(modelDirectory);
			calculateAccuracy();
		}else{
			String modelDirectory = args[1];
			String mailFile = args[2];
			loadModel(new File(modelDirectory));
			predict(mailFile);
		}

	}
	
	public static void predict(String mailFile){
		File fileEntry = new File(mailFile);
		BufferedReader br = null;
		String subject="";
		String output = "";
    	try {
    		br = new BufferedReader(new FileReader(fileEntry));
    	    String line = br.readLine();
    	    while (line != null) {
    	        if(line.contains("Subject:")){
    	        	subject = line.substring(9, line.length()-1);
    	        	output = calculateProbability(subject);
    	        }
    	        line = br.readLine();
    	    }
    	    System.out.println("classify="+output);
    	}catch (IOException e){
    		e.printStackTrace();
    	}
	}
	
	public static String calculateProbability(String subject){
		String word[] = subject.split(" ");
		String output = "";
		double calSpamProb=1.0,calHamProb=1.0;
		for(int i=0;i<word.length;i++){
			calSpamProb *= findPercent(word[i],"spam");
			calHamProb *= findPercent(word[i],"ham");
		}
		calSpamProb = spamProb*calSpamProb;
		calHamProb = hamProb*calHamProb;
		
		if(calSpamProb > calHamProb){
			output = "spam";
		}else{
			output = "ham";
		}
		
		return output;
		
	}
	
	public static double findPercent(String word,String type){
		double percent = 0;
		if(type.equals("spam")){
			if(spamWordProbabilityCount.containsKey(word)){
				percent = spamWordProbabilityCount.get(word);
			}else{
				percent = 1/(double)(spamWordProbabilityCount.size()+totalVocab);
			}
		}else{
			if(hamWordProbabilityCount.containsKey(word)){
				percent = hamWordProbabilityCount.get(word);
			}else{
				percent = 1/(double)(hamWordProbabilityCount.size()+totalVocab);
			}
		}
		return percent;
	}
	
	public static void loadModel(File modelDirectory){
		BufferedReader br = null;
		boolean bVal = false;
		File fileEntry = new File(modelDirectory+"/spamFilter.txt");
    	try {
    		br = new BufferedReader(new FileReader(fileEntry));
    	    String line = br.readLine();
    	    if(line!=null)
    	    	spamProb = Double.parseDouble(line);
    	    line = br.readLine();
    	    if(line!=null)
    	    	hamProb = Double.parseDouble(line);
    	    line = br.readLine();
    	    if(line!=null)
    	    	totalVocab = Integer.parseInt(line);
    	    
    	    line = br.readLine();
    	    while (line != null) {
    	        if(line.contains("spam")){
    	        	bVal = true;
    	        }else if(line.contains("ham")){
    	        	bVal = false;
    	        }else{
    	        	if(bVal){
    	        		String word[] = line.split(" ");
    	        		if(word.length>1){
    	        			spamWordProbabilityCount.put(word[0], Double.parseDouble(word[1]));
    	        		}
    	        	}else{
    	        		String word[] = line.split(" ");
    	        		if(word.length>1){
    	        			hamWordProbabilityCount.put(word[0], Double.parseDouble(word[1]));
    	        		}
    	        	}
    	        }
    	        line = br.readLine();
    	    }
    	}catch (IOException e){
    		e.printStackTrace();
    	}

	}
	
	public static void createUniqueMap(){
		for(String spamLine : trainSpamSubjects)
			extractWords(spamLine,"spam");
		
		for(String hamLine : trainHamSubjects)
			extractWords(hamLine,"ham");
				
	}
	
	public static void extractWords(String line, String type){
		String[] words = line.split(" ");
		
		if(type.equals("spam")){
			for(int i=0;i<words.length;i++){
				String tmp = words[i].trim();
				if(spamWordCount.get(tmp) != null){
					spamWordCount.put(tmp,spamWordCount.get(tmp)+1);
				}else{
					spamWordCount.put(tmp,1);
				}
				uniqueWordSet.add(tmp);
			}
		}else{
			
			for(int i=0;i<words.length;i++){
				String tmp = words[i].trim();
				if(hamWordCount.get(tmp) != null){
					hamWordCount.put(tmp,hamWordCount.get(tmp)+1);
				}else{
					hamWordCount.put(tmp,1);
				}
				uniqueWordSet.add(tmp);
			}
			
		}
		
	}
	
	public static void listFilesForFolder(File folder, String type) {
		
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry, type);
	        }else{
	        	BufferedReader br = null;
	        	try {
	        		br = new BufferedReader(new FileReader(fileEntry));
	        	    String line = br.readLine();
	        	    while (line != null) {
	        	    	if(line.contains("Subject:"))
	        	    	{
	        	    		String subject = line.substring(9, line.length()-1);
	        	    		if(type=="spam"){
	        	    			spamSubjects.add(subject);
	        	    		}else{
	        	    			hamSubjects.add(subject);
	        	    		}
	        	    		
	        	    		break;
	        	    	}
	        	        line = br.readLine();
	        	    }
	        	} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
	        	    try {
						br.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        	}
	        }
	    }
	    
	    
	    if(type.equals("spam")){
	    	int spamSubjectsarrayListSize = (int) Math.floor(spamSubjects.size()*PARTITION);	 	    
	    	
	 	    trainSpamSubjects = spamSubjects.subList(0, spamSubjectsarrayListSize);
	 	    totalSpam = trainSpamSubjects.size();
	 	    testSpamSubjects = spamSubjects.subList(trainSpamSubjects.size(), spamSubjects.size());

	    }else{
	    	int hamSubjectsarrayListSize = (int) Math.floor(hamSubjects.size()*PARTITION);
		    
		    trainHamSubjects = hamSubjects.subList(0, hamSubjectsarrayListSize);
		    totalHam = trainHamSubjects.size(); 
		    testHamSubjects = hamSubjects.subList(trainHamSubjects.size(), hamSubjects.size());
	    	
	    }
	    	    
	    
	}
	
	public static void createAndDumpModel(String hamDirectory){
		File file = new File(hamDirectory+"/spamFilter.txt");
		PrintWriter writer = null;
		try {
			file.createNewFile();
			writer = new PrintWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int spamWordCounter = spamWordCount.size();
		int hamWordCounter = hamWordCount.size();
		
		int totalUniqueWords = uniqueWordSet.size();
		
		spamProb = totalSpam/(double)(totalSpam+totalHam);
		hamProb = totalHam/(double)(totalSpam+totalHam);
		writer.println(spamProb);
		writer.println(hamProb);
		writer.println(totalUniqueWords);
		
		
		Iterator spamWordCountIterator = spamWordCount.entrySet().iterator();
		writer.println("spam");
		while(spamWordCountIterator.hasNext()){
			Map.Entry pair = (Map.Entry)spamWordCountIterator.next();
			
			String word = (String)pair.getKey();
			double temp = (double)(((Integer)pair.getValue()+1)/((double)(totalUniqueWords+spamWordCounter)));

			writer.println(word+" "+temp);
			spamWordProbabilityCount.put(word, temp);
			
		}
		writer.println("ham");
		Iterator hamWordCountIterator = hamWordCount.entrySet().iterator();
		while(hamWordCountIterator.hasNext()){
			Map.Entry pair = (Map.Entry)hamWordCountIterator.next();
			
			String word = (String)pair.getKey();
			double temp = (double)(((Integer)pair.getValue()+1)/((double)(totalUniqueWords+hamWordCounter)));

			writer.println(word+" "+temp);
			hamWordProbabilityCount.put(word, temp);
		}
	}
	
	public static void calculateAccuracy(){
		int spamCount=0,hamCount=0;
		double spamPer=0.0,hamPer=0.0,accuracy=0.0;
		
		
		for(String subject:testSpamSubjects){
			String output = calculateProbability(subject);
			if(output.equals("spam"))
				spamCount++;
		}
		
		for(String subject:testHamSubjects){
			String output = calculateProbability(subject);
			if(output.equals("ham"))
				hamCount++;
		}
		
		accuracy = (spamCount+hamCount)/(double)(testSpamSubjects.size()+testHamSubjects.size());
		System.out.println("accuracy="+accuracy*100+"%");
	}

}
