package eu.eurosentiment.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class StanfordNLP {


	private static StanfordCoreNLP pipeline = new StanfordCoreNLP();

	// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
	private static	Properties props = new Properties();
	static {
		props.put("annotators", "tokenize, ssplit");
	}
	private static StanfordCoreNLP sentPipeline = new StanfordCoreNLP(props);

	public static String getAdjp(Tree node){
		StringBuffer buffer = new StringBuffer();
		if(!node.isLeaf()){
			Label label = node.label();
			if(label.toString().equalsIgnoreCase("ADJP")){
				List<Tree> leaves = node.getLeaves();		
				for(Tree leave : leaves)
					buffer.append(leave.label());					
				//System.out.println(buffer);
			}
		}
		return buffer.toString().trim();
	}

	private static ArrayList<Tree> extractTag(Tree t, String tag) 
	{
		ArrayList<Tree> wanted = new ArrayList<Tree>();
		if (t.label().value().equals(tag) )
		{
			wanted.add(t);
			for (Tree child : t.children())
			{
				ArrayList<Tree> temp = new ArrayList<Tree>();
				temp=extractTag(child, tag);
				if(temp.size()>0)
				{
					int o=-1;
					o=wanted.indexOf(t);
					if(o!=-1)
						wanted.remove(o);
				}
				wanted.addAll(temp);
			}
		}
		else
			for (Tree child : t.children())
				wanted.addAll(extractTag(child, tag));
		return wanted;
	}

	public static Annotation getAnnotation(String text){
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation); 		
		return annotation;
	}

	public static Map<String, List<String>> getTagText(String text, List<String> tags){
		Map<String, List<String>> tagTextMap = new HashMap<String, List<String>>();
		Annotation annotation = new Annotation(text);		
		pipeline.annotate(annotation); 
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		if (sentences != null && sentences.size() > 0) {
			CoreMap sentence = sentences.get(0);
			Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
			for(String tag : tags){
				ArrayList<Tree> tagTexts = extractTag(tree, tag);
				List<String> list = tagTextMap.get(tag);
				if(list == null) {			
					tagTextMap.put(tag, new ArrayList<String>());			
					for(Tree tagTextTree : tagTexts){
						String tagTextString = Sentence.listToString(tagTextTree.yield());
						tagTextMap.get(tag).add(tagTextString);
					}		
				}		
			}
		}
		return tagTextMap;
	}


	public static Map<String, List<String>> getTagText(Annotation annotation, List<String> tags){
		Map<String, List<String>> tagTextMap = new HashMap<String, List<String>>();
		//Annotation annotation = new Annotation(text);		
		//pipeline.annotate(annotation); 
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		if (sentences != null && sentences.size() > 0) {
			CoreMap sentence = sentences.get(0);
			Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
			for(String tag : tags){
				ArrayList<Tree> tagTexts = extractTag(tree, tag);
				List<String> list = tagTextMap.get(tag);
				if(list == null) {			
					tagTextMap.put(tag, new ArrayList<String>());			
					for(Tree tagTextTree : tagTexts){
						String tagTextString = Sentence.listToString(tagTextTree.yield());
						tagTextMap.get(tag).add(tagTextString);
					}		
				}		
			}
		}
		return tagTextMap;
	}



	public static List<CoreMap> getSentences(String text){

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);
		// run all Annotators on this text
		sentPipeline.annotate(document);
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {		
			System.out.println(sentence.toString());
		}
		return sentences;
	}

	public static void main(String[] args) throws IOException {
		String text = "two teen couples go to a church party , drink and then drive . they get into an accident .";
		List<CoreMap> sentences = getSentences(text);
		//		StanfordCoreNLP pipeline = new StanfordCoreNLP();
		//		String text = "two teen couples go to a church party , drink and then drive . they get into an accident .";
		//		Annotation annotation = new Annotation(text);		
		//		pipeline.annotate(annotation); 
		//		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		//
		//		if (sentences != null && sentences.size() > 0) {
		//			CoreMap sentence = sentences.get(0);
		//			Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
		//			ArrayList<Tree> nps = extractTag(tree, "NP");
		//			//ArrayList<Tree> adjps = extractTag(tree, "ADJP");			
		//			for(Tree np : nps){
		//				String npString = Sentence.listToString(np.yield());
		//				System.out.println("NP : " + npString);
		//			}			
		//			for(Tree adjp : adjps){
		//				String adjpString = Sentence.listToString(adjp.yield());
		//				System.out.println("ADJP : " + adjpString);
		//			}		

	}			
}
