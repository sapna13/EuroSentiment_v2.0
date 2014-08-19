package eu.eurosentiment.process;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.insight.negdet.Negex;
import eu.eurosentiment.insight.StanfordNLP;
import eu.eurosentiment.sentiwordnet.SentiWordBags2;
import eu.eurosentiment.sentiwordnet.SentiWordBags2.PolPair;
import eu.monnetproject.clesa.ds.clesa.CLESA;

public class FileProcess implements Runnable {
	private EuroSentimentMainThreaded esAnno;
	private File file;
	private CLESA clesa;
	private SentiWordBags2 sentiWord2 = null;
	private BufferedWriter outputFileWriter = null;
	private static Logger logger = null;
	private String annotatedDataPath;
	private List<String> tags = null;
	

	FileProcess(EuroSentimentMainThreaded esAnno, File file, CLESA clesa, SentiWordBags2 sentiWord2,
			BufferedWriter outputFileWriter, Logger logger, String annotatedDataPath, List<String> tags){
		this.esAnno = esAnno;
		this.file = file;
		this.clesa = clesa;
		this.sentiWord2 = sentiWord2;
		this.outputFileWriter = outputFileWriter;
		FileProcess.logger = logger;
		this.annotatedDataPath = annotatedDataPath;
		this.tags = tags;
	}		

	@Override
	public void run() {
		esAnno.parse(file.getAbsolutePath());				
		Map<String, Long> scoreMap = esAnno.getScoreMapByParsingRawTripAdvisor(file.getName(), annotatedDataPath); //aspect-rating mapping
		boolean overall = false;
		if(scoreMap==null){
			overall = true;
		}
		Set<String> mentionClassSentences = null;

		if(scoreMap!=null){
			esAnno.getAspects(clesa, scoreMap.keySet());
			mentionClassSentences = esAnno.getMentionClassSentence(clesa);//, file.getName()); //produces 'mention---class---sentence'
			if(scoreMap.containsKey("overall") && scoreMap.size() == 1)
				overall = true;
		}
		int flushAfter50 = 0;

		if(overall) {
			try{ 
				String[] clauseWords = null;
				double normalisedScore = 0.0;
				double negScore = 0.0;
				double posScore = 0.0;
				String mentionClass = "na";					

				Map<String, PolPair> polMap = sentiWord2.getSentiWordPolMap();				
				Map<String, String> mentionURLSentenceMap =  esAnno.getMentionSentence(clesa); //mention + url is mapped to the sentence
				for(String mentionURL: mentionURLSentenceMap.keySet()){
					String mention = mentionURL.split("\t")[0]; //in the map, mention + url is mapped to the sentence
					String URL = mentionURL.split("\t")[1];
					String sentence = mentionURLSentenceMap.get(mentionURL);
					List<String> clauses = StanfordParser.getClauses(sentence);
					Set<String> clauseSet = new HashSet<String>(clauses);

					for(String clause: clauseSet){
						if(clause.contains(mention)){
							clauseWords = clause.split(" ");
							for(String word: clauseWords){
								String tagText = null;
								word = StanfordParser.lemmatizeWord(word);
								if(polMap.containsKey(word)){
									PolPair polPair = polMap.get(word);
									negScore = polPair.getNegativeScore();
									posScore = polPair.getPositiveScore();

									if(negScore>posScore){
										if(negScore>0.40){
											normalisedScore = -1; //fixing the negative polarity to absolute value -1 
											tagText = word;
											outputFileWriter.write(mention + "\t"+ URL + "\t" + mentionClass + "\t" + tagText + "\t" + normalisedScore+"\n");
										}

									}
									else if(posScore>negScore){
										if(posScore>0.40){
											normalisedScore = 1;	//fixing the negative polarity to absolute value -1 
											tagText = word;
											outputFileWriter.write(mention + "\t"+ URL + "\t" + mentionClass + "\t" + tagText + "\t" + normalisedScore+"\n");
										}
									}


									if(flushAfter50 > 200){
										System.out.println("Flushed");
										outputFileWriter.flush();										
										flushAfter50 = 0;;
									}


								}
								flushAfter50++;
							}
						}
					}
				}
			} catch(Exception e){
				System.out.println(e);
				logger.debug("Skipped" + file.getName());
			}


		} 
		else {

			try{ 
				for(String mentionClassSentence : mentionClassSentences){
					String[] split = mentionClassSentence.split("-----");
					String mention  = split[0];
					String mentionClass = split[1];
					String sentence = split[2];				
					Map<String, List<String>> tagTextMap = StanfordNLP.getTagText(sentence, tags);			//POS tag with text					
					for(String tag : tags){
						List<String> tagTexts = tagTextMap.get(tag);
						for(String tagText : tagTexts){
							boolean senti = EuroSentimentMainThreaded.containsSenti(tagText);  //checks if the extracted sentiment word appears in the sentiwordnet
							if(senti){		
								if(EuroSentimentMainThreaded.getLength(tagText)<4){ 
									long score = scoreMap.get(mentionClass) ;  //normalising score 13-2-2014	
									double normalisedScore = 0.0;
									if(score<=2 && score >=1)
										normalisedScore = 0.5;
									if(score>=3)
										normalisedScore = 1;
									if(score<=0 && score >= -2)
										normalisedScore = -0.5; 
									if(score<=-2)
										normalisedScore = -1;
									if(Negex.negCheck(sentence, tagText, false))
										score = score*(-1);

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

			} catch(Exception e){
				System.out.println(e);
				logger.debug("Skipped" + file.getName());
			}
		}	

	}

}

