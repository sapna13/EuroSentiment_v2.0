package eu.eurosentiment.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.eurosentiment.synset.SynsetIdentification;
import eu.utils.BasicFileTools;

public class LexiconCollector {

	public static void main(String[] args) throws IOException {
		String dirPath = "src/main/resources/output/new";
		Map<String, Double> mentionPhraseScoreMap = new HashMap<String, Double>();
		File dir = new File (dirPath);
		File[] files = dir.listFiles();
		for(File file : files){
			BufferedReader reader = BasicFileTools.getBufferedReader(file);
			String line = null;
			try {
				while((line=reader.readLine())!=null){
					String[] split = line.split("\t");
					String mention = split[0].replace(","," ");
					String sentimentPhrase = split[2].replace(","," ");
					String scoreString = split[3];
					if(!scoreString.contains("null") && mention.length()>2){
						try{
							Double score = Double.parseDouble(split[3]);
							if(mentionPhraseScoreMap.get(mention+"-----"+sentimentPhrase) == null){								
								mentionPhraseScoreMap.put(mention+"-----"+sentimentPhrase, score);								
							} else {
								Double savedScore = mentionPhraseScoreMap.get(mention+"-----"+sentimentPhrase);
								double avgScore = (savedScore + score) / 2; 
								mentionPhraseScoreMap.put(mention+"-----"+sentimentPhrase, avgScore);
							}					
						} catch(Exception e){
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		StringBuffer buffer = new StringBuffer();

		Map<String, List<String>> mentionPhraseMap = new HashMap<String, List<String>>();
		
		for(String mentionPhrase : mentionPhraseScoreMap.keySet()){
			String[] split = mentionPhrase.split("-----");
			String mention = split[0].toLowerCase();
			String phrase = split[1].toLowerCase();
			if(mentionPhraseMap.get(mention)==null)
				mentionPhraseMap.put(mention, new ArrayList<String>());
			mentionPhraseMap.get(mention).add(phrase);			
		}	
		
		SynsetIdentification.loadConfig("load/eu.monnetproject.clesa.CLESA.properties");
	
//		System.out.println(synsetId);
//		BasicFileTools.writeFile("output.txt", synsetId);
//		
		
		
		for(String mention : mentionPhraseMap.keySet()){
			List<String> phrases = mentionPhraseMap.get(mention);
			for(String phrase : phrases){
				Double score = mentionPhraseScoreMap.get(mention + "-----" + phrase);
				
				String sentimentPhrase = phrase;
				String context = mention + " hotel";
				
				String synsetId = SynsetIdentification.getSynsetId(sentimentPhrase, context);
				String line = mention + "\t,\t" + phrase + "\t,\t" + score + "\t,\t" + synsetId;
				buffer.append(line);
				System.out.println(line);
				buffer.append("\n");
			}
		}
		
//		for(String mentionPhrase : mentionPhraseScoreMap.keySet()){
//			String[] split = mentionPhrase.split("-----");
//			String mention = split[0].toLowerCase();
//			String phrase = split[1].toLowerCase();
//			Double score = mentionPhraseScoreMap.get(mentionPhrase);
//			buffer.append(mention + "\t,\t" + phrase + "\t,\t" + score);
//			buffer.append("\n");
//		}

		BasicFileTools.writeFile("src/main/resources/finalOutput_new_avgScored_keyMention.txt", buffer.toString().trim());

	}	


}
