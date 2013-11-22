package eu.eurosentiment.process;

import eu.eurosentiment.insight.StanfordNLP;
import eu.eurosentiment.sentiwordnet.SentiWordBags;
import eu.monnetproject.clesa.core.lang.Language;
import eu.monnetproject.clesa.core.utils.Pair;
import eu.monnetproject.clesa.core.utils.TroveVectorUtils;
import eu.monnetproject.clesa.ds.clesa.CLESA;
import eu.utils.BasicFileTools;
import eu.utils.StandAloneAnnie;
import eu.utils.VectorUtils;
import gnu.trove.TIntDoubleHashMap;

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
 * main class for the hotel corpus
 * @author sapna
 *
 */
public class EuroSentimentMainHotel {
	private String text;
	private JSONArray annotations;
	private static List<String> classes = new ArrayList<String>();
	private static Map<String, TIntDoubleHashMap> classVectorMap = new HashMap<String, TIntDoubleHashMap>();
	private StanfordNLP nlp = new StanfordNLP();

	private static SentiWordBags sentiWord = new SentiWordBags();
	private static Set<String> sentiWords = sentiWord.getSentiWords();




	public EuroSentimentMainHotel(String path) {
		parameterParserAela(path);
	}


	public static boolean containsSenti(String tagText){
		StringTokenizer tokenizer = new StringTokenizer(tagText);
		while(tokenizer.hasMoreTokens()){

			String token = tokenizer.nextToken();

			if(sentiWords.contains(token)){
				return true;
			}
		}
		return false;		
	}

	public static int getLength(String tagText){
		StringTokenizer tokenizer = new StringTokenizer(tagText);
		return tokenizer.countTokens();
	}

	static{
		CLESA clesa = new CLESA();
		classes.add("value");classes.add("rooms");classes.add("location");classes.add("check in front desk");
		classes.add("service");classes.add("business service");
		for(String classLabel : classes){
			TIntDoubleHashMap vector = clesa.getVector(new Pair<String, Language>(classLabel, Language.ENGLISH));
			classVectorMap.put(classLabel, vector);
		}
		clesa.close();
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

			Long overallRating = (Long) jsonObject.get("Overall");			
			Long value = (Long) jsonObject.get("Value");
			Long service = (Long) jsonObject.get("Service");
			Long rooms = (Long) jsonObject.get("Rooms");
			Long frontDesk = (Long) jsonObject.get("Check in / front desk");
			Long cleanliness =  (Long) jsonObject.get("Cleanliness");
			Long location = (Long) jsonObject.get("Location");
			Long businessService = (Long) jsonObject.get("BusinessService");


			//Long overallRating = Long.parseInt((((String) jsonObject.get("Overall")).trim()));			
			//			Long value = Long.parseInt((((String) jsonObject.get("Value")).trim()));
			//			Long service = Long.parseInt((((String) jsonObject.get("Service")).trim()));
			//			Long rooms = Long.parseInt((((String) jsonObject.get("Rooms")).trim()));
			//			Long frontDesk = Long.parseInt((((String) jsonObject.get("Check in / front desk")).trim()));
			//			Long cleanliness = Long.parseInt((((String) jsonObject.get("Cleanliness")).trim()));
			//			Long location = Long.parseInt((((String) jsonObject.get("Location")).trim()));
			//			Long businessService = Long.parseInt((((String) jsonObject.get("BusinessService")).trim()));

			fieldValueMap.put("overall", overallRating);
			fieldValueMap.put("value", value);
			fieldValueMap.put("service", service);
			fieldValueMap.put("rooms", rooms);
			fieldValueMap.put("check in front desk", frontDesk);
			fieldValueMap.put("cleanliness", cleanliness);
			fieldValueMap.put("location", location);
			fieldValueMap.put("business service", businessService);
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

		VectorUtils<String> utils = new VectorUtils<String>();
		int i = 0;

		HashSet<String> mentionClassSentence = new HashSet<String>();

		text = StandAloneAnnie.getRefinedText(text);
		List<String> sentences = nlp.getSentences(text.trim());
		StringBuffer bu = new StringBuffer();
		for(String senten : sentences){
			bu.append(senten + " ");
		}

		Map<String, String> mentionClassMap = new HashMap<String, String>();

		for(Object annotation : annotations){
			@SuppressWarnings("unchecked")
			Map<String, Object> annoMap = (Map<String, Object>) annotation;
			int endOffset  = -1;
			int startOffset = -1;
			String offset = null;
			HashMap<String, Object> annoMapCopy = new HashMap<String, Object>(annoMap);

			for(String prop : annoMapCopy.keySet()){				
				if(prop.equalsIgnoreCase("endOffset"))
					endOffset = Integer.parseInt(annoMap.get(prop).toString().trim());
				if(prop.equalsIgnoreCase("startOffset"))
					startOffset = Integer.parseInt(annoMap.get(prop).toString().trim());
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
						mentionClassMap.put(mention.trim(), classLabelWithMaxScore.trim());
					} else {
						//		System.out.print(i++  + "   " + mention);
						//		System.out.println("\t\t" + classLabelWithMaxScore + "\t\t");		
					}
				}
			}
		}
		for(String mention : mentionClassMap.keySet()){
			for(String sentence : sentences){
				if(sentence.contains(mention.trim())){
					String content = mention.trim() + "-----" + 
							mentionClassMap.get(mention.trim()).trim() + "-----" + sentence.trim();
					mentionClassSentence.add(content);				
				}
			}
		}			



		return mentionClassSentence;
		//		for(String s : mentionClassSentence){
		//			
		//			
		//			System.out.println(s);
		//		}				
	}





	public static void start(String aelaOutputPath, String outputPath, String annotatedDataPath) {
		//String aelaDataPath = "es/TripAdvisor_AELAOutput_10k";


		//String aelaDataPath = "es/TripAdvisorAELAOutput_1500_1";
	


		//String tripRawDataPath = "es/RawTripAdvisorData";
		File dir = new File(aelaOutputPath);
		CLESA clesa = new CLESA();
		StringBuffer buffer = new StringBuffer();
		List<String> tags = new ArrayList<String>();
		tags.add("JJ");
		tags.add("ADJP");
		tags.add("VBN");

		int i = 0;

		File[] listFiles = dir.listFiles();
		for(File file : listFiles){
			System.out.println("fileNo.   " + i++);
			System.out.println("fileName   " + file.getName());

			try {
				EuroSentimentMainHotel esAnno = new EuroSentimentMainHotel(file.getAbsolutePath());			
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
								//	System.out.println(buffer);
							}
						}
					}							
				}
			} catch(Exception e){
				System.out.println("Skipped" + file.getName());
			}
		}
		clesa.close();


		BasicFileTools.writeFile(outputPath, buffer.toString().trim());
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
