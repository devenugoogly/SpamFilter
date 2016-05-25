package CS286;
import scala.Tuple2;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.feature.HashingTF;
import org.apache.spark.mllib.regression.LabeledPoint;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.spark.SparkConf;
public class JavaNaiveBayes {
	
	static ArrayList<String> hamSubjects = new ArrayList<String>();
	static ArrayList<String> spamSubjects = new ArrayList<String>();
	static Map<String,Integer> spamWordCount = new HashMap<String, Integer>();
	static Map<String,Integer> hamWordCount = new HashMap<String, Integer>();
	
	public static void main(String[] args) {
		SparkConf sparkConf = new SparkConf().setAppName("SpamFilter");
		JavaSparkContext ctx = new JavaSparkContext(sparkConf);
		
		String type = args[0];
		final HashingTF tf = new HashingTF(100);	
		if(type.equals("build")){
			String hamDirectory = args[1];
			String spamDirectory = args[2];
			String modelDirectory = args[3];

			listFilesForFolder(new File(hamDirectory),"ham");
			listFilesForFolder(new File(spamDirectory),"spam");
			createUniqueMap();
				
			JavaRDD<String> ham = ctx.parallelize(hamSubjects);
			JavaRDD<String> spam = ctx.parallelize(spamSubjects);
			JavaRDD<LabeledPoint> hamSamples = ham.map(new Function<String, LabeledPoint>() {
      				public LabeledPoint call(String subject) {
        				return new LabeledPoint(0, tf.transform(Arrays.asList(subject.split(" "))));
      				}
    			});		
			
			JavaRDD<LabeledPoint> spamSamples = spam.map(new Function<String, LabeledPoint>() {
  				public LabeledPoint call(String subject) {
    				return new LabeledPoint(1, tf.transform(Arrays.asList(subject.split(" "))));
  				}
			});

			JavaRDD<LabeledPoint> trainingData = hamSamples.union(spamSamples);
			JavaRDD<LabeledPoint>[] tmp = trainingData.randomSplit(new double[]{0.6,0.4},12345);
			
			JavaRDD<LabeledPoint> training = tmp[0];
			JavaRDD<LabeledPoint> test = tmp[1];

			final NaiveBayesModel model = NaiveBayes.train(training.rdd(), 1.0);
			
			model.save(ctx.sc(), "file:///"+modelDirectory);
			
			JavaPairRDD<Double, Double> predictionAndLabel =test.mapToPair(new PairFunction<LabeledPoint, Double, Double>() {
				public Tuple2<Double, Double> call(LabeledPoint p) {
					return new Tuple2<Double, Double>(model.predict(p.features()), p.label());
				}
			});
			
			double accuracy = predictionAndLabel.filter(new Function<Tuple2<Double, Double>, Boolean>() {
				public Boolean call(Tuple2<Double, Double> pl) {
					return pl._1().equals(pl._2());
				}
			}).count() / (double) test.count();
			
			double acc = accuracy*100;
			
			System.out.println("accuracy="+acc+"%");
		}else if(type.equals("predict")){
			String modelDirectory = args[1];
			final NaiveBayesModel model = NaiveBayesModel.load(ctx.sc(),"file:///"+modelDirectory);
			String mailFile = args[2];
			String subject = extractSubjectLine(mailFile);
			ArrayList<String> subjectLineArray = new ArrayList<String>();
			subjectLineArray.add(subject);
			JavaRDD<String> subjectLine = ctx.parallelize(subjectLineArray);
            JavaRDD<LabeledPoint> test = subjectLine.map(new Function<String, LabeledPoint>() {
            	public LabeledPoint call(String subject) {
            			return new LabeledPoint(0, tf.transform(Arrays.asList(subject.split(" "))));
                	}
             });

            
			JavaRDD<Double> predictionAndLabel =test.map(new Function<LabeledPoint, Double>() {
				public Double call(LabeledPoint p) {
					return model.predict(p.features());
                }
            });
			
			if(predictionAndLabel.first().equals(1.0)){
				System.out.println("classify=ham");
			}else{
				System.out.println("classify=spam");
			}
			
		}
	}

	public static String extractSubjectLine(String mailFile){
		File fileEntry = new File(mailFile);
		BufferedReader br = null;
		String subject="";
    		try {
    			br = new BufferedReader(new FileReader(fileEntry));
    	    		String line = br.readLine();
    	    		while (line != null) {
    	        		if(line.contains("Subject:")){
    	        			subject = line.substring(9, line.length()-1);
    	        		}
    	        	line = br.readLine();
    	    		}
    		}catch (IOException e){
    			e.printStackTrace();
    		}
    		return subject;
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
	        		e.printStackTrace();
	        	} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	    }
	}

	public static void createUniqueMap(){
		
		for(String spamLine : spamSubjects)
			extractWords(spamLine,"spam");
		
		for(String hamLine : hamSubjects)
			extractWords(hamLine,"ham");

		
	}

	public static void extractWords(String line, String type){
		String[] words = line.split(" ");
		
		if(type.equals("spam")){
			for(int i=0;i<words.length;i++){
				if(spamWordCount.get(words[i]) != null){
					spamWordCount.put(words[i],spamWordCount.get(words[i])+1);					
				}else{
					spamWordCount.put(words[i],1);
				}
			}
		}else{
			
			for(int i=0;i<words.length;i++){
				if(hamWordCount.get(words[i]) != null){
					hamWordCount.put(words[i],hamWordCount.get(words[i])+1);
				}else{
					hamWordCount.put(words[i],1);
				}
			}
			
		}
		
	}
}