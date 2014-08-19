package eu.eurosentiment.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eu.eurosentiment.insight.StanfordNLP;
import eu.eurosentiment.sentiwordnet.SentiWordBags;
import eu.eurosentiment.sentiwordnet.SentiWordBags2;
import eu.monnetproject.clesa.core.lang.Language;
import eu.monnetproject.clesa.core.utils.Pair;
import eu.monnetproject.clesa.core.utils.TroveVectorUtils;
import eu.monnetproject.clesa.ds.clesa.CLESA;
import eu.utils.StandAloneAnnie;
//import eu.utils.StandAloneAnnie;
import gnu.trove.TIntDoubleHashMap;

/**
 * main class for the electronics corpus
 * @author sapna
 *
 */
public class EuroSentimentMainThreaded {
	private String text;
	private JSONArray annotations;
	private static SentiWordBags sentiWord;
	private static Set<String> sentiWords;
	private static final int NTHREADS = 5;
	private static Logger logger = Logger.getLogger(EuroSentimentMainThreaded.class);
	private static String log4JPropertyFile = "load/log4j.properties";
	private static SentiWordBags2 sentiWord2;
	private static List<String> classes = new ArrayList<String>();
	private static Map<String, TIntDoubleHashMap> classVectorMap = new HashMap<String, TIntDoubleHashMap>();


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

	public static int getLength(String tagText){
		StringTokenizer tokenizer = new StringTokenizer(tagText);
		return tokenizer.countTokens();
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
		sentiWord2 = new SentiWordBags2(sentiNetPath);
		sentiWords = sentiWord.getSentiWords();	

	}

	//only for data without aspect rating or no ratings at all
	public Map<String, String> getMentionSentence(CLESA clesa){//, String aelaFileName){	
		//logger.info("Getting mentionClassSentence for : " + aelaFileName);
		try{

			//text = StandAloneAnnie.getRefinedText(text);
		} catch(Exception e){
			logger.error("The sourceURL and document's content were null: while refining the text using standaloneannie, program would still continue skipping the current file.");
		}
		List<String> sentences = StanfordNLP.getSentences(text.trim());
		StringBuffer bu = new StringBuffer();
		for(String senten : sentences)
			bu.append(senten + " ");
		Map<String, String> mentionSentenceMap = new HashMap<String, String>();
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
				}
			}
			if(endOffset>=0 && startOffset >=0)				
				offset = startOffset + "----" + endOffset;
			for(String prop : annoMap.keySet()){
				if(prop.equalsIgnoreCase("mention")){
					String mention = ((String) annoMap.get(prop)).toLowerCase();					
					for(String sentence : sentences){
						String[] split = mention.trim().split("\t");
						String findMention = split[0].trim();
						if(sentence.contains(findMention))
							mentionSentenceMap.put(mention.trim() + "\t" + uri.trim(), sentence.trim());	

					}

				}

			}
		}

		return mentionSentenceMap;
	}

	public Map<String, Long> getScoreMapByParsingRawTripAdvisor(String aelaFileName, String rawDataPath) {
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

			JSONArray ratings = (JSONArray) js.get("ratings");

			for(Object aspect : ratings){
				Long rating = (Long) js.get(aspect);
				//	System.out.println(aspect + " " + rating);
				//System.out.println(rating);
				if(rating!=null)
					fieldValueMap.put((String) aspect, rating);			
			}
			//			for(String aspect : classes) {				
			//				Long rating = (Long) js.get(aspect);
			//			//	System.out.println(aspect + " " + rating);
			//				//System.out.println(rating);
			//				if(rating!=null)
			//					fieldValueMap.put(aspect, rating);			
			//			}
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



	private void parameterParserAela(String path) {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
			jsonObject = (JSONObject) parser.parse(new FileReader(path));
			setText(((String) jsonObject.get("text")).trim());			
			annotations = (JSONArray) jsonObject.get("annotations");			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch 4
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void getAspects(CLESA clesa, Set<String> labels){
		try {
			for(String line : labels){
				classes.add(line.trim().toLowerCase().trim());			//aspects are added to a list called class
			}
		} catch (Exception e) {
		}
		logger.info("Initiating clesa vector creation for classLabels");
		for(String classLabel : classes){
			TIntDoubleHashMap vector = clesa.getVector(new Pair<String, Language>(classLabel, Language.ENGLISH)); /*word vectors corresponding to each class label are prepared and saved				                                                                                                        beforehand to improve code efficiency, they are not required tobe generated on the fly*/
			classVectorMap.put(classLabel, vector);
		}
	}


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

	public static void start(String aelaOutputPath, String outputPath, String annotatedDataPath, String aspectFile, String gatePath, String sentiPath, String outputDir, String wnhome, String finalOutputFilePath) throws IOException {	
		//	EuroSentimentMainThreaded esAnno = new EuroSentimentMainThreaded();
		StanfordParser.getClauses("I am going to the market.");
		//	StandAloneAnnie.getRefinedText("I am going to the market.");
		logger.info("Initiating SentiWordnet");
		logger.info("Initiating SentiWordnet");
		EuroSentimentMainThreaded.initiateSWNet(sentiPath);
		StandAloneAnnie.setUp(gatePath);		//ANNIE is a tokenizer from Gate framework, setup initiates the tokenizer
		File dir = new File(aelaOutputPath);
		logger.info("Initiating CLESA load");
		CLESA clesa = new CLESA();             //CLESA is the class which implements semantic similarity. The component is re-used from EU project Monnet.
		//author: kartik Asooja , component is freely distributable and is available at github
		//esAnno.getAspects(aspectFile, clesa);
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
		//int flushAfter50 = 0;

		ExecutorService executor = Executors.newFixedThreadPool(NTHREADS);
		for(File file : listFiles){
			System.out.println(listFiles.length);
			EuroSentimentMainThreaded esAnnoThread = new EuroSentimentMainThreaded();
			if(file.isHidden())
				continue;			
			logger.info("fileNo.  " + i++);
			logger.info("fileName  " + file.getName());
			File fileThread = new File(file.getAbsolutePath());
			Runnable task = new FileProcess(esAnnoThread, fileThread, clesa, sentiWord2, outputFileWriter, logger, annotatedDataPath, tags);			
			executor.execute(task);//submit(task);	
		}		
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		outputFileWriter.close();
		oswriter.close();
		//BasicFileTools.writeFile(outputPath, buffer.toString().trim());
		System.out.println("threading finished");
		logger.info("DSSPA module ran successfully, intermediate output wriiten");
		clesa.close();
		LexiconCollector_keyphrase.start(outputDir, clesa, wnhome, finalOutputFilePath);
		logger.info("SSI module successful, final output written");
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text.toLowerCase();
	}


}
