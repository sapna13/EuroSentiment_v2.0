package eu.eurosentiment.insight;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.monnetproject.clesa.core.utils.BasicFileTools;



public class TripAdvisorData {

	public static void main(String[] args){
		Map<String, List<Review>> reviews = readReviews("data");
		StringBuffer buffer = new StringBuffer();
		for(String key : reviews.keySet()){
			for(Review review : reviews.get(key)){

				String content = review.getContent();
				Map<String, List<String>> attributeMap = getTagText(content);

				Map<String, Integer> scoreMap = new HashMap<String, Integer>();
				scoreMap.put("location", review.getLocation());
				scoreMap.put("cleanliness", review.getCleanliness());
				scoreMap.put("front desk", review.getFrontDesk());
				scoreMap.put("room", review.getRooms());
				scoreMap.put("overall", review.getOverall());
				scoreMap.put("value", review.getValue());
				scoreMap.put("service", review.getService());		

				for(String attribute : scoreMap.keySet()){
					List<String> attributeStrings = attributeMap.get(attribute);
					if(attributeStrings!=null)
						for(String attributeString : attributeStrings){
							if(!(scoreMap.get(attribute) < 0)){
								buffer.append(attribute + "\t");							
								buffer.append(attributeString + "\t" + scoreMap.get(attribute)+"\n");
							}
						}
					//buffer.append("\n");
				}			

			}
		}

		BasicFileTools.writeFile("table.txt", buffer.toString().trim());

	}

	public static Map<String, List<String>> getTagText(String content) {
		Map<String, List<String>> map = new HashMap<String, List<String>>();		
		List<String> attributes = new ArrayList<String>();
		attributes.add("location");
		attributes.add("service");
		attributes.add("value");
		attributes.add("cleanliness");
		attributes.add("room");
		attributes.add("overall");
		attributes.add("front desk");

		for(String attribute : attributes){
			String[] splits = content.split("<" +attribute + ">");
			for(String split : splits){
				try{
					String attributeString = split.substring(0, split.indexOf("<\\" + attribute +">"));
					List<String> attributeStrings = map.get(attribute);
					if(attributeStrings == null){
						attributeStrings = new ArrayList<String>();
					} 
					attributeStrings.add(attributeString);
					map.put(attribute, attributeStrings);					
					//		System.out.println(locationString);
				} catch(Exception e){
				}

			}
		}
		return map;
	}


	private static Map<String,List<Review>> readReviews(String dirPath) {
		/*
		 * 1 - Given a Directory. Get the list with all .dat files.
		 * 2 - For each .dat file:
		 *   2.1 - Extract all fields
		 * 3 - The output map key is the filename and the value is a
		 * Review object with author and text.
		 */
		Map<String,List<Review>> reviews = new HashMap<String,List<Review>>();		
		File dir = new File(dirPath);
		for(File file: dir.listFiles()) {
			List<Review> reviewsFromFile = new ArrayList<Review>();
			if(file.getName().endsWith(".dat")){
				try {
					BufferedReader in = new BufferedReader(new FileReader(file));
					String line;
					Review review = new Review();			

					while ((line = in.readLine()) != null) {

						line = line.trim();
						if(line.startsWith("<Author>")) {
							review.setAuthor(line.replace("<Author>",""));
						}
						if(line.startsWith("<Content>")) {
							review.setContent(line.replace("<Content>","").replace("\"",""));
						}

						if(line.startsWith("<Date>")) {
							//review.setDate(Date.valueOf(line.replace("<Date>","").replace("\"","")));
						}

						if(line.startsWith("<No. Reader>")) {
							review.setNoOfReaders(Integer.parseInt(line.replace("<No. Reader>","").replace("\"","")));
						}
						if(line.startsWith("<No. Helpful>")) {
							review.setNoOfHelpful(Integer.parseInt(line.replace("<No. Helpful>","").replace("\"","")));
						}
						if(line.startsWith("<Overall>")) {
							review.setOverall(Integer.parseInt(line.replace("<Overall>","").replace("\"","")));
						}

						if(line.startsWith("<Value>")) {
							review.setValue(Integer.parseInt(line.replace("<Value>","").replace("\"","")));
						}

						if(line.startsWith("<Rooms>")) {
							review.setRooms(Integer.parseInt(line.replace("<Rooms>","").replace("\"","")));
						}

						if(line.startsWith("<Location>")) {
							review.setLocation(Integer.parseInt(line.replace("<Location>","").replace("\"","")));
						}

						if(line.startsWith("<Cleanliness>")) {
							review.setCleanliness(Integer.parseInt(line.replace("<Cleanliness>","").replace("\"","")));
						}

						if(line.startsWith("<Check in / front desk>")) {
							review.setFrontDesk(Integer.parseInt(line.replace("<Check in / front desk>","").replace("\"","")));
						}

						if(line.startsWith("<Service>")) {
							review.setService(Integer.parseInt(line.replace("<Service>","").replace("\"","")));
						}

						if(line.startsWith("<Business service>")) {
							review.setBusinessService(Integer.parseInt(line.replace("<Business service>","").replace("\"","")));
							reviewsFromFile.add(review);
							review = new Review();
						}
					}
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				reviews.put(file.getName(), reviewsFromFile);
			}
		}


		return reviews;
	}

}