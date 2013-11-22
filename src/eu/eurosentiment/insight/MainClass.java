package eu.eurosentiment.insight;

//starts the module, determines the domain of input corpus and calls the respective module 
import java.io.*;

import eu.eurosentiment.process.EuroSentimentMainElectronics;
import eu.eurosentiment.process.EuroSentimentMainHotel;


public class MainClass {
	public static void main(String[] args){
		File corpusPath = new File("resources");
		File[] corpusFolders =  corpusPath.listFiles();
		for (File corpus:corpusFolders){
			if (corpus.getName().equals("ElectronicsJson"))
			{
				String aelaOutputPathElec = "resources/AELAoutputElectronics";
				String rawDataPathElec = "resources/ElectronicsJson";
				String outputPathElec = "Outputs";
				
				EuroSentimentMainElectronics.start(aelaOutputPathElec,outputPathElec,rawDataPathElec);
				
			}
			
			else
				
			{
				String aelaOutputPathHot = "resources/AELAoutputHotel";
				String rawDataPathHot = "resources/HotelJson";
				String outputPathHot = "Outputs";
				EuroSentimentMainHotel.start(aelaOutputPathHot,outputPathHot,rawDataPathHot);
				
			}
			
		}
		
		
	}
	

}
