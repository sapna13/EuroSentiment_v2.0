package eu.eurosentiment.insight;

//starts the module, determines the domain of input corpus and calls the respective module 
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import eu.eurosentiment.process.EuroSentimentMain;;

public class MainClass {
	private static String aelaOutputPath;
	private static String rawDataPath;
	private static String intermediateOutput;
	private static String gatePath;
	private static String aspectFile;
	private static String sentiWordNetFile;
	private static String outputDir;
	private static String wnhome;
	private static String finalLexicon;


	private static Logger logger = Logger.getLogger(MainClass.class);

	private static Properties config = new Properties();

	private  static void loadConfig(){
		try {
			config.load(new FileInputStream("load/eu.eurosentiment.annotation.properties"));
			outputDir = config.getProperty("outputDir");
			aelaOutputPath = config.getProperty("aelaOutputPath").trim();
			rawDataPath = config.getProperty("rawDataPath").trim();
			intermediateOutput = outputDir + "/" + config.getProperty("intermediateOutput").trim();		
			gatePath = config.getProperty("gatePath").trim();
			aspectFile = config.getProperty("aspectFile").trim();
			sentiWordNetFile = config.getProperty("sentiWordNetFile").trim();
			wnhome = config.getProperty("WNHOME");
			finalLexicon = outputDir + "/" + config.getProperty("finalLexicon");
		} catch (FileNotFoundException e) {
			logger.debug("");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	public static void main(String[] args) throws IOException{
		logger.setLevel(Level.INFO);		
		loadConfig();
		// Now set its level. Normally you do not need to set the
		// level of a logger programmatically. This is usually done
		// in configuration files.
		logger.info("Corpus Reader Started");
		EuroSentimentMain.start(aelaOutputPath,intermediateOutput,rawDataPath, aspectFile, gatePath, sentiWordNetFile,
				outputDir, wnhome, finalLexicon);
	}

}



