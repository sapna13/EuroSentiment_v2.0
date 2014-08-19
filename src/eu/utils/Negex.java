package eu.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import eu.eurosentiment.insight.MainClass;


/* Wendy Chapman's NegEx algorithm in Java.
 * Sentence boundaries serve as WINDOW for negation (suggested by Wendy Chapman) */

public class Negex {
    
	private static List<String> sortedRules;
	private static String triggersFilePath;

	static {	
		MainClass.loadConfig();
		triggersFilePath =  MainClass.triggersFilePath;
		File ruleFile = new File(triggersFilePath);
		Scanner sc;
		try {
			sc = new Scanner(ruleFile);
			List<String> rules = new ArrayList<String>();
			while (sc.hasNextLine()) 
				rules.add(sc.nextLine());
			sortedRules = sortRules(rules);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}			
	}

	public static List<String> sortRules(List<String> unsortedRules) {
		try {
			// Sort the negation rules by length to make sure
			// that longest rules match first.
			//	String temp = "";
			for (int i = 0; i<unsortedRules.size()-1; i++) {
				for (int j = i+1; j<unsortedRules.size(); j++) {
					String a = (String) unsortedRules.get(i);
					String b = (String) unsortedRules.get(j);
					if (a.trim().length()<b.trim().length()) {
						// Sorting into descending order by length of string.
						unsortedRules.set(i, b);
						unsortedRules.set(j, a);
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
		return unsortedRules;
	}

	public static boolean negCheck(String sentenceString, String phraseString, 
			boolean negatePossible) {
		//Sorter s = new Sorter();
		String sToReturn = "";
		String sScope	= "";
		//	String sentencePortion	= "";

		String filler = "_";
		boolean negPoss	= negatePossible;
		//	boolean negationScope = true;

		// Sort the rules by length in descending order.
		// Rules need to be sorted so the longest rule is always tried to match
		// first.
		// Some of the rules overlap so without sorting first shorter rules (some of them POSSIBLE or PSEUDO)
		// would match before longer legitimate negation rules.

		// There is efficiency issue here. It is better if rules are sorted by the 
		// calling program once and used without sorting in GennegEx.

		// Process the sentence and tag each matched negation
		// rule with correct negation rule tag.
		// 
		// At the same time check for the phrase that we want to decide
		// the negation status for and
		// tag the phrase with [PHRASE] ... [PHRASE]
		// In both the negation rules and in the  phrase replace white space
		// with "filler" string. (This could cause problems if the sentences
		// we study has "filler" on their own.)

		// Sentence needs one character in the beginning and end to match.
		// We remove the extra characters after processing.
		String sentence = "." + sentenceString + ".";

		// Tag the phrases we want to detect for negation.
		// Should happen before rule detection.
		String phrase = phraseString;
		Pattern pph = Pattern.compile(phrase.trim(), Pattern.CASE_INSENSITIVE);
		Matcher mph = pph.matcher(sentence);

		while (mph.find() == true ) {
			sentence  = mph.replaceAll(" [PHRASE]" + mph.group().trim().replaceAll(" ", filler)
					+ "[PHRASE]");
		}

		Iterator<String> iRule = sortedRules.iterator();
		while (iRule.hasNext()){
			String rule 		= (String) iRule.next();
			Pattern p 		= Pattern.compile("[\\t]+"); 	// Working.
			String[] ruleTokens 	= p.split(rule.trim());
			// Add the regular expression characters to tokens and asemble the rule again.
			String[] ruleMembers	= ruleTokens[0].trim().split(" ");
			String rule2		= "";
			for (int i=0; i<=ruleMembers.length-1; i++) {
				if (!ruleMembers[i].equals("")) {
					if (ruleMembers.length == 1) {
						rule2 = ruleMembers[i];
					}
					else {
						rule2 = rule2 + ruleMembers[i].trim() + "\\s+";					
					}
				}
			}
			// Remove the last s+
			if (rule2.endsWith("\\s+")) {
				rule2 = rule2.substring(0, rule2.lastIndexOf("\\s+"));
			}

			rule2 = "(?m)(?i)[[\\p{Punct}&&[^\\]\\[]]|\\s+](" + rule2 +")[[\\p{Punct}&&[^_]]|\\s+]" ;

			Pattern p2 		= Pattern.compile(rule2.trim());
			Matcher m		= p2.matcher(sentence);

			while (m.find() == true ) {
				sentence  = m.replaceAll(" " + ruleTokens[1].trim()
						+ m.group().trim().replaceAll(" ", filler) 
						+   ruleTokens[1].trim() + " ");
			}
		}


		// Exchange the [PHRASE] ... [PHRASE] tags for [NEGATED] ... [NEGATED]
		// based of PREN, POST rules and if flag is set to true
		// then based on PREP and POSP, as well.

		// Because PRENEGATION [PREN} is checked first it takes precedent over
		// POSTNEGATION [POST].
		// Similarly POSTNEGATION [POST] takes precedent over POSSIBLE PRENEGATION [PREP]
		// and [PREP] takes precedent over POSSIBLE POSTNEGATION [POSP].

		Pattern pSpace = Pattern.compile("[\\s+]");
		String[] sentenceTokens = pSpace.split(sentence);
		StringBuilder sb = new StringBuilder();


		// Check for [PREN]
		for (int i = 0; i<sentenceTokens.length; i++) {
			sb.append(" " + sentenceTokens[i].trim());
			if (sentenceTokens[i].trim().startsWith("[PREN]")) {

				for (int j = i+1; j<sentenceTokens.length; j++) {
					if (sentenceTokens[j].trim().startsWith("[CONJ]") ||
							sentenceTokens[j].trim().startsWith("[PSEU]") ||
							sentenceTokens[j].trim().startsWith("[POST]") ||
							sentenceTokens[j].trim().startsWith("[PREP]") ||
							sentenceTokens[j].trim().startsWith("[POSP]") ) {
						break;
					}

					if (sentenceTokens[j].trim().startsWith("[PHRASE]") ) {
						sentenceTokens[j] = sentenceTokens[j].trim().replaceAll("\\[PHRASE\\]", "[NEGATED]");
					}
				}
			}
		}

		sentence = sb.toString();
		pSpace = Pattern.compile("[\\s+]");
		sentenceTokens = pSpace.split(sentence);
		StringBuilder sb2 = new StringBuilder();

		// Check for [POST]
		for (int i = sentenceTokens.length-1; i>0; i--) {
			sb2.insert(0, sentenceTokens[i] + " ");
			if (sentenceTokens[i].trim().startsWith("[POST]")) {
				for (int j = i-1; j>0; j--) {
					if (sentenceTokens[j].trim().startsWith("[CONJ]") ||
							sentenceTokens[j].trim().startsWith("[PSEU]") ||
							sentenceTokens[j].trim().startsWith("[PREN]") ||
							sentenceTokens[j].trim().startsWith("[PREP]") ||
							sentenceTokens[j].trim().startsWith("[POSP]") ) {
						break;
					}

					if (sentenceTokens[j].trim().startsWith("[PHRASE]") ) {
						sentenceTokens[j] = sentenceTokens[j].trim().replaceAll("\\[PHRASE\\]", "[NEGATED]");
					}
				}
			}
		}

		sentence = sb2.toString();

		// If POSSIBLE negation is detected as negation.
		// negatePossible being set to "true" then check for [PREP] and [POSP].
		if (negPoss == true) {
			pSpace = Pattern.compile("[\\s+]");
			sentenceTokens = pSpace.split(sentence);

			StringBuilder sb3 = new StringBuilder();

			// Check for [PREP]
			for (int i = 0; i<sentenceTokens.length; i++) {
				sb3.append(" " + sentenceTokens[i].trim());
				if (sentenceTokens[i].trim().startsWith("[PREP]")) {

					for (int j = i+1; j<sentenceTokens.length; j++) {
						if (sentenceTokens[j].trim().startsWith("[CONJ]") ||
								sentenceTokens[j].trim().startsWith("[PSEU]") ||
								sentenceTokens[j].trim().startsWith("[POST]") ||
								sentenceTokens[j].trim().startsWith("[PREN]") ||
								sentenceTokens[j].trim().startsWith("[POSP]") ) {
							break;
						}

						if (sentenceTokens[j].trim().startsWith("[PHRASE]") ) {
							sentenceTokens[j] = sentenceTokens[j].trim().replaceAll("\\[PHRASE\\]", "[POSSIBLE]");
						}
					}
				}
			}

			sentence = sb3.toString();
			pSpace = Pattern.compile("[\\s+]");
			sentenceTokens = pSpace.split(sentence);
			StringBuilder sb4 = new StringBuilder();

			// Check for [POSP]
			for (int i = sentenceTokens.length-1; i>0; i--) {
				sb4.insert(0, sentenceTokens[i] + " ");
				if (sentenceTokens[i].trim().startsWith("[POSP]")) {
					for (int j = i-1; j>0; j--) {
						if (sentenceTokens[j].trim().startsWith("[CONJ]") ||
								sentenceTokens[j].trim().startsWith("[PSEU]") ||
								sentenceTokens[j].trim().startsWith("[PREN]") ||
								sentenceTokens[j].trim().startsWith("[PREP]") ||
								sentenceTokens[j].trim().startsWith("[POST]") ) {
							break;
						}

						if (sentenceTokens[j].trim().startsWith("[PHRASE]") ) {
							sentenceTokens[j] = sentenceTokens[j].trim().replaceAll("\\[PHRASE\\]", "[POSSIBLE]");
						}
					}
				}
			}

			sentence = sb4.toString();
		}

		// Remove the filler character we used.
		sentence = sentence.replaceAll(filler, " ");

		// Remove the extra periods at the beginning 
		// and end of the sentence.
		sentence = sentence.substring(0, sentence.trim().lastIndexOf('.'));
		sentence = sentence.replaceFirst(".", "");

		// Get the scope of the negation for PREN and PREP 
		if (sentence.contains("[PREN]") || sentence.contains("[PREP]")) {
			int startOffset = sentence.indexOf("[PREN]");
			if (startOffset == -1) {
				startOffset = sentence.indexOf("[PREP]");
			}

			int endOffset = sentence.indexOf("[CONJ]");
			if (endOffset == -1) {
				endOffset = sentence.indexOf("[PSEU]");
			}
			if (endOffset == -1) {
				endOffset = sentence.indexOf("[POST]");
			}
			if (endOffset == -1) {
				endOffset = sentence.indexOf("[POSP]");
			}
			if (endOffset == -1 || endOffset < startOffset) {
				endOffset = sentence.length() - 1;
			}
			sScope = sentence.substring(startOffset, endOffset+1);
		}

		// Get the scope of the negation for POST and POSP 
		if (sentence.contains("[POST]") || sentence.contains("[POSP]")) {
			int endOffset = sentence.lastIndexOf("[POST]");
			if (endOffset == -1) {
				endOffset = sentence.lastIndexOf("[POSP]");
			}

			int startOffset = sentence.lastIndexOf("[CONJ]");
			if (startOffset == -1) {
				startOffset = sentence.lastIndexOf("[PSEU]");
			}
			if (startOffset == -1) {
				startOffset = sentence.lastIndexOf("[PREN]");
			}
			if (startOffset == -1) {
				startOffset = sentence.lastIndexOf("[PREP]");
			}
			if (startOffset == -1 ) {
				startOffset = 0;
			}
			sScope = sentence.substring(startOffset, endOffset);
		}

		// Classify to: negated/possible/affirmed
		if (sentence.contains("[NEGATED]")) {
			return true;
			//sentence = sentence + "\t" + "negated" + "\t" + sScope;			
		}
		else if (sentence.contains("[POSSIBLE]")) {
			return true;
			//sentence = sentence + "\t" + "possible" + "\t" + sScope;
		}
		else {
			return false;
			//sentence = sentence + "\t" + "affirmed" + "\t" + sScope;
		}

		//sToReturn = sentence;

		//return sToReturn;
	}

	public static void main(String[] args) {
		boolean negatePossible	= true;
		String phrase = "like";
		String sentence = "I did not like the book.";
		boolean negCheck = negCheck(sentence, phrase, negatePossible);
	
		System.out.println(negCheck);
	} 

}

