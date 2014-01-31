package eu.eurosentiment.process;
import org.json.simple.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.*;
import org.apache.log4j.Logger;


/**
 * A class to convert raw manually annotated electronics review into desired Json format with
 * the fields Text and different aspects
 * @author sapna
 *
 */

public class ElectronicsJsonConverter {
	public static void main(String[] args) throws IOException{

		File directory = new File("C:/Users/Sony/Desktop/electronicsSplit"); 
		File[] allFiles = directory.listFiles();
		Set<String> aspects = new HashSet<String>();

		for (int j = 0; j < allFiles.length; j++)
		{
			File pathCurrentFile = allFiles[j];
			String fileName = pathCurrentFile.getName().replace(".txt", "");
			String JSONfilePath= "C:/Users/Sony/Desktop/ElectronicsJson/"+fileName+".json" ;


			FileWriter JSONfile = new FileWriter(JSONfilePath);
			JSONObject jsonObject = createJsonObj(pathCurrentFile, aspects);
			JSONfile.write(jsonObject.toJSONString());
			JSONfile.flush();
			JSONfile.close();
		}		
		int i = 0;
		for(String aspect : aspects)
			System.out.println(i++ + "  " + aspect);		
	}

	public static JSONObject createJsonObj(File file, Set<String> aspects) throws IOException{   
		BufferedReader br = new BufferedReader(new FileReader(file));
		String aspectRegex1 = "([\\s\\w/]+)\\[([+-]\\d)\\]([,#]+)";
		String aspectRegex2 = "([\\s\\w/]+)\\[([+-]\\d)\\](\\[\\w+\\])([,#]+)";
		Pattern aspectPattern = Pattern.compile(aspectRegex1);
		StringBuffer buffer = new StringBuffer();
		Map<String, Integer> aspectValueMap = new HashMap<String, Integer>();
		try {
			String rawText = null;
			while ((rawText = br.readLine()) != null) {    

				Matcher matcher = aspectPattern.matcher(rawText);
				if(matcher.find()){
					String aspect = matcher.group(1);
					int aspectValue = Integer.parseInt(matcher.group(2).trim());
					aspects.add(aspect);
					//System.out.println(aspect + "  " + aspectValue);
					aspectValueMap.put(aspect, aspectValue);

					String newSentence = rawText.replaceAll(aspectRegex1," ");
					newSentence = newSentence.replaceAll(aspectRegex2," ");
					buffer.append(newSentence.trim() + "  ");
				}
				else {
					String newSentence = rawText.replaceAll(aspectRegex2," ");
					newSentence = newSentence.replace("##"," ");					
					buffer.append(newSentence.trim() + "  ");
				}
			}



		} catch (Exception ex) {
			System.out.println(ex);
			return null;
		} finally { 
			if(br != null) 
				br.close();}

		JSONObject obj = new JSONObject();

//		aspectValueMap.put("text", buffer.toString().trim());
		obj.putAll(aspectValueMap);
		obj.put("Text", buffer.toString().trim());
		return obj; 
	}

}


