package eu.eurosentiment.insight;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;

import eu.eurosentiment.process.EuroSentimentMain;

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
	private static String log4JPropertyFile = "load/log4j.properties";
	public static String triggersFilePath;
	
	private static Properties config = new Properties();

	public static void loadConfig(){
		try {			
			Properties p = new Properties();
			try {
			    p.load(new FileInputStream(log4JPropertyFile));
			    PropertyConfigurator.configure(p);
			//    logger.info("Wow! I'm configured!");
			} catch (IOException e) {
			    //DAMN! I'm not
			}
			config.load(new FileInputStream("load/eu.eurosentiment.annotation.properties"));
			outputDir = config.getProperty("outputDir");
			aelaOutputPath = config.getProperty("aelaOutputPath").trim();
			rawDataPath = config.getProperty("rawDataPath").trim();
			intermediateOutput = config.getProperty("intermediateOutput").trim();		
			gatePath = config.getProperty("gatePath").trim();
			aspectFile = config.getProperty("aspectFile").trim();
			sentiWordNetFile = config.getProperty("sentiWordNetFile").trim();
			wnhome = config.getProperty("WNHOME");
			finalLexicon = config.getProperty("finalLexicon").trim();	
			triggersFilePath = config.getProperty("triggersFilePath").trim();
		} catch (FileNotFoundException e) {
			logger.debug("File Not Found " + e);
			e.printStackTrace();
		} catch (IOException e) {
			logger.debug("IOException " + e);			
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



