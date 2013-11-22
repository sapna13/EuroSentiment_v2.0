package eu.eurosentiment.sentiwordnet;

//performs...

//author snegi

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import eu.utils.BasicFileTools;

public class SentiWordBags {


	private String posWordsString;
	private String negWordsString;
	private Set<String> posWords;
	private Set<String> negWords;
	private int lineStupid; 
	private Set<String> sentiWords;

	public SentiWordBags() {
		sentiWords = new HashSet<String>();
		createBags("resources/SentiWordNet_3.0.0_20130122.txt");
		//createBags("copy.txt");

	}

	public int getLineStupid(){
		return lineStupid;
	}

	public Set<String> getSentiWords(){
		return sentiWords;
	}

	public String getPosWordsString() {
		return posWordsString;
	}

	public String getNegWordsString() {
		return negWordsString;
	}

	private void createBags(String filePath) {
		StringBuffer posWordsBuffer = new StringBuffer();
		StringBuffer negWordsBuffer = new StringBuffer();
		BufferedReader reader = BasicFileTools.getBufferedReaderFile(filePath);
		String line = null;
		int i = 0;
		int posneg = 0;
		int pos = 0;
		int neg = 0;
		try {

			while((line = reader.readLine()) != null) {

				//System.out.println(++i);
				//if(line.contains("#"))
				//	continue;
				String[] strings = line.split("\t");
				//String pos = strings[0];
				//String id = strings[1];
				String posScoreString = strings[2];
				String negScoreString = strings[3];
				String synsetTerms = strings[4];
				String gloss = strings[5];
				//gloss = normalize(gloss);
				String[] terms = synsetTerms.split(" ");
				for(String term : terms) {					
					term = term.substring(0, term.indexOf("#"));
					double posScore = Double.parseDouble(posScoreString);
					double negScore = Double.parseDouble(negScoreString);
					if(posScore > 0.200){
						pos++;				
						sentiWords.add(term);
						//posWordsBuffer.append(term + " ");
						posneg++;						
						//SentiWordnetConcept concept = new SentiWordnetConcept(term, gloss);
						//sentiConcepts.add(concept);	
					}
					if(negScore > 0.200) {
						neg++;
						sentiWords.add(term);
						//negWordsBuffer.append(term + " ");
						posneg++;
						//SentiWordnetConcept concept = new SentiWordnetConcept(term, gloss);
						//sentiConcepts.add(concept);	
					}
				}				
			}
		} catch (NumberFormatException e) {		
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		//posWordsString = posWordsBuffer.toString().toLowerCase().trim();
		//negWordsString = negWordsBuffer.toString().toLowerCase().trim();
		//System.out.println();
		//System.out.println(pos);
		//System.out.println(neg);
		//System.out.println("1:  " + posWordsString);
		//System.out.println("2:  " + negWordsString);		
	}

	public static void main(String[] args) {
		SentiWordBags bags = new SentiWordBags();
		Set<String> words = bags.sentiWords;
		//	for(String word : words){
		//		System.out.println(word);
		//	}
		System.out.println(bags.getLineStupid());
	}

}
