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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



/**
 * main class for the electronics corpus
 * @author sapna
 *
 */
public class EuroSentimentMain {
	private String text;
	private JSONArray annotations;
	private static List<String> classes = new ArrayList<String>();
	private static Map<String, TIntDoubleHashMap> classVectorMap = new HashMap<String, TIntDoubleHashMap>();
	private static SentiWordBags sentiWord;
	private static Set<String> sentiWords;

	public void parse(String path) {
		parameterParserAela(path);			
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
				classes.add(line.trim().toLowerCase());			
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(String classLabel : classes){
			TIntDoubleHashMap vector = clesa.getVector(new Pair<String, Language>(classLabel, Language.ENGLISH));
			classVectorMap.put(classLabel, vector);
		}
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



	private Map<String, Long> getScoreMapByParsingRawTripAdvisor(String aelaFileName, String rawDataPath) {
		aelaFileName = aelaFileName.replace(".txt", "").trim();

		Map<String, Long> fieldValueMap = new HashMap<String, Long>();
		String filePath = rawDataPath + "/" + aelaFileName + ".json";		
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
			jsonObject = (JSONObject) parser.parse(new FileReader(filePath));
			for(String aspect : classes){
				Long rating = (Long) jsonObject.get(aspect);
				//System.out.println(rating);
				if(rating!=null)
					fieldValueMap.put(aspect, rating);
			}
			return fieldValueMap;
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
		return null;
	}



	public Set<String> getMentionClassSentence(CLESA clesa, String aelaFileName){		
		HashSet<String> mentionClassSentence = new HashSet<String>();
		text = StandAloneAnnie.getRefinedText(text);
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
						score = TroveVectorUtils.cosineProduct(mentionVector, classVectorMap.get(classLabel));									
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
		EuroSentimentMain esAnno = new EuroSentimentMain();
		EuroSentimentMain.initiateSWNet(sentiPath);
		StandAloneAnnie.setUp(gatePath);		
		File dir = new File(aelaOutputPath);
		CLESA clesa = new CLESA();
		esAnno.getAspects(aspectFile, clesa);

		StringBuffer buffer = new StringBuffer();
		List<String> tags = new ArrayList<String>();

		tags.add("JJ");
		tags.add("ADJP");
		tags.add("VBN");

		int i = 0;

		File[] listFiles = dir.listFiles();
		for(File file : listFiles){
			if(file.isHidden())
				continue;
			System.out.println("fileNo.   " + i++);
			System.out.println("fileName   " + file.getName());

			try {
				esAnno.parse(file.getAbsolutePath());				
				Set<String> mentionClassSentences = esAnno.getMentionClassSentence(clesa, file.getName());
				Map<String, Long> scoreMap = esAnno.getScoreMapByParsingRawTripAdvisor(file.getName(), annotatedDataPath);
				for(String mentionClassSentence : mentionClassSentences){
					String[] split = mentionClassSentence.split("-----");
					String mention  = split[0];
					String mentionClass = split[1];
					String sentence = split[2];				
					Map<String, List<String>> tagTextMap = StanfordNLP.getTagText(sentence, tags);								
					for(String tag : tags){
						List<String> tagTexts = tagTextMap.get(tag);
						for(String tagText : tagTexts){
							boolean senti = containsSenti(tagText);
							if(senti){		
								if(getLength(tagText)<4)
									buffer.append(mention + "\t"+ mentionClass + "\t" + tagText + "\t" + scoreMap.get(mentionClass)+"\n");
							}
						}
					}							
				}
				esAnno.refresh();
			} catch(Exception e){
				System.out.println("Skipped" + file.getName());
			}
		}
		BasicFileTools.writeFile(outputPath, buffer.toString().trim());
		LexiconCollector_keyphrase.start(outputDir, clesa, wnhome, finalOutputFilePath);

		clesa.close();

	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text.toLowerCase();
	}

}
