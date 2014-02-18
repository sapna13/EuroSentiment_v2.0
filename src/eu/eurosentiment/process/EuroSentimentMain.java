package eu.eurosentiment.process;

import eu.eurosentiment.insight.StanfordNLP;
import eu.eurosentiment.sentiwordnet.SentiWordBags;
import eu.monnetproject.clesa.core.lang.Language;
import eu.monnetproject.clesa.core.utils.Pair;
import eu.monnetproject.clesa.core.utils.TroveVectorUtils;
import eu.monnetproject.clesa.ds.clesa.CLESA;
import eu.utils.BasicFileTools;
import eu.utils.StandAloneAnnie;
//import eu.utils.StandAloneAnnie;
import gnu.trove.TIntDoubleHashMap;
import edu.insight.negdet.Negex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class EuroSentimentMain {
	private String text;
	private JSONArray annotations;
	private static List<String> classes = new ArrayList<String>();
	private static Map<String, TIntDoubleHashMap> classVectorMap = new HashMap<String, TIntDoubleHashMap>();
	private static SentiWordBags sentiWord;
	private static Set<String> sentiWords;
	private static Logger logger = Logger.getLogger(EuroSentimentMain.class);
	private static String log4JPropertyFile = "load/log4j.properties";

	public void parse(String path) {
		parameterParserAela(path);			
	}

	static{
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(log4JPropertyFile));
			PropertyConfigurator.configure(p);

		} catch (IOException e) {

		}
	}

	public static boolean containsSenti(String tagText){
		StringTokenizer tokenizer = new StringTokenizer(tagText); 
		while(tokenizer.hasMoreTokens()){
			String token = tokenizer.nextToken();
			if(sentiWords.contains(token))
				return true;			
		}
		return false;		
	}

	public static void initiateSWNet(String sentiNetPath){
		sentiWord = new SentiWordBags(sentiNetPath);
		sentiWords = sentiWord.getSentiWords();		 
	}

	public static int getLength(String tagText){
		StringTokenizer tokenizer = new StringTokenizer(tagText);
		return tokenizer.countTokens();
	}

	private void getAspects(String aspectFile, CLESA clesa){
		BufferedReader br = BasicFileTools.getBufferedReaderFile(aspectFile);
		String line = null;
		try {
			while((line=br.readLine())!=null){
				classes.add(line.trim().toLowerCase().trim());			//aspects are added to a list called class
			}
		} catch (IOException e) {
			logger.debug("IO Exception, Error while reading aspectFile "  + aspectFile);			
		}
		logger.info("Initiating clesa vector creation for classLabels");
		for(String classLabel : classes){
			TIntDoubleHashMap vector = clesa.getVector(new Pair<String, Language>(classLabel, Language.ENGLISH)); /*word vectors corresponding to each class label are prepared and saved
			                                                                                                        beforehand to improve code efficiency, they are not required tobe generated on the fly*/
			classVectorMap.put(classLabel, vector);
		}
	}

	private void parameterParserAela(String path) { //reads the json output of AELA, extracts text and entities
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
			FileReader fileReader = new FileReader(path);
			jsonObject = (JSONObject) parser.parse(fileReader);			
			Set<String> keySet = jsonObject.keySet();
			jsonObject.entrySet();
			JSONObject js = new JSONObject();			
			for(String key : keySet)
				js.put((Object) key.toLowerCase(), jsonObject.get(key));		
			setText(((String) js.get("text")).trim());			
			annotations = (JSONArray) js.get("annotations");	
			fileReader.close();			
		} catch (FileNotFoundException e) {
			logger.debug("File Not Found : " + path);
		} catch (IOException e) {
			logger.debug("Error while reading "  + path);			
		} catch (ParseException e) {
			logger.debug("Error while parsing json:  "  + path);			
		}
	}



	private Map<String, Long> getScoreMapByParsingRawTripAdvisor(String aelaFileName, String rawDataPath) {
		aelaFileName = aelaFileName.replace(".txt", "").trim();

		Map<String, Long> fieldValueMap = new HashMap<String, Long>();
		String filePath = rawDataPath + "/" + aelaFileName + ".json";		
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {	
			FileReader fileReader = new FileReader(filePath);
			jsonObject = (JSONObject) parser.parse(fileReader);			
			Set<String> keySet = jsonObject.keySet();		
			JSONObject js = new JSONObject();			
			for(String key : keySet)
				js.put((Object) key.toLowerCase().trim(), jsonObject.get(key));

			for(String aspect : classes) {				
				Long rating = (Long) js.get(aspect);
			//	System.out.println(aspect + " " + rating);
				//System.out.println(rating);
				if(rating!=null)
					fieldValueMap.put(aspect, rating);			
			}
			fileReader.close();
			return fieldValueMap;
		} catch (FileNotFoundException e) {
			logger.debug("File Not Found :" + aelaFileName + " in " + rawDataPath);
		} catch (IOException e) {
			logger.debug("Error while reading : " + aelaFileName + " in " + rawDataPath);
		} catch (ParseException e) {
			logger.debug("Error while parsing : " + aelaFileName + " in " + rawDataPath);
		}
		return null;
	}


	/* each element of the returned set is a string comprising of the following info:
	 * 1. Mention = entity mention annotated by AELA
	 * 2. Class = Aspect for the entity mention, determined on the basis of highest semantic similarity between each aspect and the mention
	 * 3. sentence = sentence in which the mention was found
	 */

	public Set<String> getMentionClassSentence(CLESA clesa){//, String aelaFileName){	
		//logger.info("Getting mentionClassSentence for : " + aelaFileName);
		HashSet<String> mentionClassSentence = new HashSet<String>();
		try{
			text = StandAloneAnnie.getRefinedText(text);
		} catch(Exception e){
			logger.error("The sourceURL and document's content were null: while refining the text using standaloneannie, program would still continue skipping the current file.");
		}
		List<String> sentences = StanfordNLP.getSentences(text.trim());
		StringBuffer bu = new StringBuffer();
		for(String senten : sentences)
			bu.append(senten + " ");
		Map<String, String> mentionClassMap = new HashMap<String, String>();
		for(Object annotation : annotations){
			@SuppressWarnings("unchecked")
			Map<String, Object> annoMap = (Map<String, Object>) annotation;
			int endOffset  = -1;
			int startOffset = -1;
			String offset = null;
			HashMap<String, Object> annoMapCopy = new HashMap<String, Object>(annoMap);
			String uri = "";
			for(String prop : annoMapCopy.keySet()){				
				if(prop.equalsIgnoreCase("endOffset"))
					endOffset = Integer.parseInt(annoMap.get(prop).toString().trim());
				if(prop.equalsIgnoreCase("startOffset"))
					startOffset = Integer.parseInt(annoMap.get(prop).toString().trim());
				if(prop.equalsIgnoreCase("uri")){
					JSONArray uris = (JSONArray) annoMap.get(prop);
					uri = (String) uris.get(0);
					//	System.out.println(uri);
				}
			}
			if(endOffset>=0 && startOffset >=0)				
				offset = startOffset + "----" + endOffset;
			for(String prop : annoMap.keySet()){
				if(prop.equalsIgnoreCase("mention")){
					//double maxScore = Double.NEGATIVE_INFINITY;
					double maxScore = 0.021;
					boolean beat = false;
					String classLabelWithMaxScore  = null;
					String mention = ((String) annoMap.get(prop)).toLowerCase();
					TIntDoubleHashMap mentionVector = clesa.getVector(new Pair<String, Language>(mention, Language.ENGLISH));
					for(String classLabel : classes){
						double score = 0.0;
						score = TroveVectorUtils.cosineProduct(mentionVector, classVectorMap.get(classLabel));	/*semantic similarity computation								
						                                                                                         between aspect/classes and entity mentions */
						if(score >= maxScore){                                                                      
							maxScore = score;
							beat = true;
							classLabelWithMaxScore = classLabel;
						}
					}						
					if(classLabelWithMaxScore==null)
						classLabelWithMaxScore = " ";
					if(beat) {
						//	System.out.print(i++  + "   " + mention);
						maxScore = Math.round(maxScore * 100.0) / 100.0;
						mentionClassMap.put(mention.trim() + "\t" + uri.trim(), classLabelWithMaxScore.trim());
					} else {
						//		System.out.print(i++  + "   " + mention);
						//		System.out.println("\t\t" + classLabelWithMaxScore + "\t\t");		
					}
				}
			}
		}
		for(String mention : mentionClassMap.keySet()){
			for(String sentence : sentences){
				String[] split = mention.trim().split("\t");
				String findMention = split[0].trim();
				if(sentence.contains(findMention)){
					String content = mention.trim() + "-----" + 
							mentionClassMap.get(mention.trim()).trim() + "-----" + sentence.trim();
					mentionClassSentence.add(content);				
				}
			}
		}			
		return mentionClassSentence;
	}

	public void refresh(){
		text = null;
		annotations = null;
	}

	/* annotatedDataPath: Original data in json format
	 * sentiPath: path for sentiwordnet
	 */
	public static void start(String aelaOutputPath, String outputPath, String annotatedDataPath, 
			String aspectFile, String gatePath, String sentiPath, String outputDir, String wnhome, String finalOutputFilePath) throws IOException {		
		EuroSentimentMain esAnno = new EuroSentimentMain();
		logger.info("Initiating SentiWordnet");
		EuroSentimentMain.initiateSWNet(sentiPath);
		StandAloneAnnie.setUp(gatePath);		//ANNIE is a tokenizer from Gate framework, setup initiates the tokenizer
		File dir = new File(aelaOutputPath);
		logger.info("Initiating CLESA load");
		CLESA clesa = new CLESA();             //CLESA is the class which implements semantic similarity. The component is re-used from EU project Monnet.
		// author: kartik Asooja , component is freely distributable and is available at github
		esAnno.getAspects(aspectFile, clesa);
		List<String> tags = new ArrayList<String>();    /*list to store POS tags/phrases we are interested in extracting. 
		                                                  We consider them to be carrying the sentiment information */
		tags.add("JJ");   //JJ = Adjective
		tags.add("ADJP");
		tags.add("VBN");
		tags.add("VB");     //13-2-2014
		int i = 0;    //
		File[] listFiles = dir.listFiles();       //Files have same names in the AELA output and the original corpus
		FileOutputStream fostream = new FileOutputStream(outputPath); 
		OutputStreamWriter oswriter = new OutputStreamWriter(fostream); 
		BufferedWriter outputFileWriter = new BufferedWriter(oswriter);   
		//		bwriter.write("Use steps for"); bwriter.newLine(); bwriter.write("immediate floor."); bwriter.newLine(); bwriter.write("Avoid lift traffic.");   bwriter.close(); oswriter.close(); fostream.close(); 
		int flushAfter50 = 0;
		//	File outputFilePath = new File(outputPath);
		for(File file : listFiles){
			if(file.isHidden())
				continue;
			//			System.out.println("fileNo.   " + i++);
			//			System.out.println("fileName   " + file.getName());

			logger.info("fileNo.   " + i++);
			logger.info("fileName   " + file.getName());
			try {
				esAnno.parse(file.getAbsolutePath());				
				Set<String> mentionClassSentences = esAnno.getMentionClassSentence(clesa);//, file.getName()); //produces 'mention---class---sentence'
				Map<String, Long> scoreMap = esAnno.getScoreMapByParsingRawTripAdvisor(file.getName(), annotatedDataPath); //aspect-rating mapping
				for(String mentionClassSentence : mentionClassSentences){
					String[] split = mentionClassSentence.split("-----");
					String mention  = split[0];
					String mentionClass = split[1];
					String sentence = split[2];				
					Map<String, List<String>> tagTextMap = StanfordNLP.getTagText(sentence, tags);			//POS tag with text					
					for(String tag : tags){
						List<String> tagTexts = tagTextMap.get(tag);
						for(String tagText : tagTexts){
							boolean senti = containsSenti(tagText);  //checks if the extracted sentiment word appears in the sentiwordnet
							if(senti){		
								if(getLength(tagText)<4){ 
									long score = scoreMap.get(mentionClass) ;  //normalising score 13-2-2014
									if(Negex.negCheck(sentence, tagText, false))
										score = 5-score;	
									double normalisedScore = 0.0;
									if(score>=3)
										normalisedScore = 1;
									if(score<=-3)
										normalisedScore = -1;
									if(score<=2 && score >=1)
										normalisedScore = 0.5;
									if(score<=-1 && score >= -2)
										normalisedScore = 0.5;

									//StringBuffer buffer = new StringBuffer();
									//buffer.append(mention + "\t"+ mentionClass + "\t" + tagText + "\t" + normalisedScore+"\n"); //mentionClass= aspect
									outputFileWriter.write(mention + "\t"+ mentionClass + "\t" + tagText + "\t" + normalisedScore+"\n");
									if(flushAfter50 > 200){
										System.out.println("Flushed");
										outputFileWriter.flush();										
										flushAfter50 = 0;;
									}
									flushAfter50++;
								}
							}
						}
					}							
				}
				esAnno.refresh();
			} catch(Exception e){
				System.out.println(e);
				logger.debug("Skipped" + file.getName());
			}
		}
		outputFileWriter.close();
		oswriter.close();
		//BasicFileTools.writeFile(outputPath, buffer.toString().trim());
		logger.info("DSSPA modue ran successfully, output wriiten to intermediate file");

		LexiconCollector_keyphrase.start(outputDir, clesa, wnhome, finalOutputFilePath);
		logger.info("Sentiwordnet Synsets identified, final output written");

		clesa.close();

	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text.toLowerCase();
	}

	public class Barrier {
		/** Number of objects being waited on */
		private int counter;
		/** Constructor for Barrier
		 * 
		 * @param n Number of objects to wait on
		 */
		public Barrier(int n) {
			counter = n;
		}
		/** Wait for objects to complete */
		public synchronized void barrierWait() {
			while(counter > 0) {
				try {
					wait();
				} catch (InterruptedException e) {}
			}
		}
		/** Object just completed */
		public synchronized void barrierPost() {
			counter--;
			if(counter == 0) {
				notifyAll();
			}
		}
		public boolean isFinished() {
			return counter ==0 ? true : false;
		}
	}


}
