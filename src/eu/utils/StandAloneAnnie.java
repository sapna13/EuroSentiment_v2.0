/*
 *  StandAloneAnnie.java
 *
 *
 * Copyright (c) 2000-2001, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  hamish, 29/1/2002
 *
 *  $Id: StandAloneAnnie.java,v 1.6 2006/01/09 16:43:22 ian Exp $
 */

package eu.utils;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ANNIEConstants;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import gate.util.Out;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class illustrates how to use ANNIE as a sausage machine
 * in another application - put ingredients in one end (URLs pointing
 * to documents) and get sausages (e.g. Named Entities) out the
 * other end.
 * <P><B>NOTE:</B><BR>
 * For simplicity's sake, we don't do any exception handling.
 */
public class StandAloneAnnie  {

	/** The Corpus Pipeline application to contain ANNIE */
	private static SerialAnalyserController annieController;
	private static StandAloneAnnie annie = new StandAloneAnnie();

	/**
	 * Initialise the ANNIE system. This creates a "corpus pipeline"
	 * application that can be used to run sets of documents through
	 * the extraction system.
	 */
	private void initAnnie() throws GateException {
		Out.prln("Initialising ANNIE...");
		// create a serial analyser controller to run ANNIE with
		annieController =
				(SerialAnalyserController) Factory.createResource(
						"gate.creole.SerialAnalyserController", Factory.newFeatureMap(),
						Factory.newFeatureMap(), "ANNIE_" + Gate.genSym()
						);
		// load each PR as defined in ANNIEConstants
		for(int i = 0; i < ANNIEConstants.PR_NAMES.length; i++) {
			FeatureMap params = Factory.newFeatureMap(); // use default parameters
			ProcessingResource pr = (ProcessingResource)
					Factory.createResource(ANNIEConstants.PR_NAMES[i], params);

			// add the PR to the pipeline controller
			annieController.add(pr);
		} // for each ANNIE PR

		Out.prln("...ANNIE loaded");
	} // initAnnie()

	/** Tell ANNIE's controller about the corpus you want to run on */
	public void setCorpus(Corpus corpus) {
		annieController.setCorpus(corpus);
	} // setCorpus

	/** Run ANNIE */
	public void execute() throws GateException {
		//	Out.prln("Running ANNIE...");
		annieController.execute();
		//	Out.prln("...ANNIE complete");
	} // execute()

	
	public static void setUp(String gatePath){
		// initialise the GATE library
		Out.prln("Initialising GATE...");
		// Gate.init();
		setUpAnnie(gatePath);
		// Load ANNIE plugin
		File gateHome = Gate.getGateHome();
		File pluginsHome = new File(gateHome, "plugins");
		try {
			Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "ANNIE").toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Out.prln("...GATE initialised");
		// initialise ANNIE (this may take several minutes)

		try {
			annie.initAnnie();
		} catch (GateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void setUpAnnie(String serverGatePath) {
		
		
		
		//String serverGatePath = "/home/karaso/Exp/gate-7.1-build4485-BIN/";
		//String serverGatePath = "/Applications/GATE_Developer_7.1";
		if (!Gate.isInitialised()) {
			//File gateHome = new File("/Users/kartik/Downloads/gate-7.1-build4485-BIN/"); //new File(grailsApplication.config.gate.home.toString());
			File gateHome = new File(serverGatePath); //new File(grailsApplication.config.gate.home.toString());

			if (Gate.getGateHome()==null)
				Gate.setGateHome(gateHome);
			//if (Gate.getPluginsHome()==null)
			//Gate.setPluginsHome(new File(""));
			if (Gate.getUserConfigFile()==null){
				Gate.setUserConfigFile(new File(gateHome, serverGatePath));
				//Gate.setUserConfigFile(new File(gateHome, "/Users/kartik/Downloads/gate-7.1-build4485-BIN/"));
			}
			Gate.runInSandbox(true);
			try {
				Gate.init();				    
			} catch (Exception e) {
				System.out.println("problem during initalizing GATE");
				e.printStackTrace();
			}
		}
	}

	public static String getRefinedText(String text){
		// create a GATE corpus and add a document for each command-line
		// argument
		Corpus corpus;
		StringBuffer buffer = null;
		try {
			corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
			//URL u = new URL(args[i]);
			FeatureMap params = Factory.newFeatureMap();
			params.put("stringContent", text);
			params.put("preserveOriginalContent", new Boolean(true));
			params.put("collectRepositioningInfo", new Boolean(true));
			Document doci =  (Document)
					Factory.createResource("gate.corpora.DocumentImpl", params);
			corpus.add(doci);
			// tell the pipeline about the corpus and run it
			annie.setCorpus(corpus);
			annie.execute();
			AnnotationSet anno = doci.getAnnotations();

			Set<String> allTypes = new HashSet<String>();	
			allTypes.add("Token");

			allTypes.add("SpaceToken");
			AnnotationSet set = anno.get(allTypes);
			buffer = new StringBuffer();
			List<Annotation> list = gate.Utils.inDocumentOrder(set);
			for(Annotation ann : list){
				//System.out.println(ann);				
				String tokenValue = (String) ann.getFeatures().get("string");
				//System.out.println(tokenValue);
				buffer.append(tokenValue + " ");				
			}
			//			for(int j = 0; j<set.size(); j++){
			//				System.out.println(set.get(j));
			//				String tokenValue = (String) set.get(j).getFeatures().get("string");
			//				System.out.println(tokenValue);
			//				buffer.append(tokenValue + " ");
			//			}
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		} catch (GateException e) {
			e.printStackTrace();
		}	
		return buffer.toString().trim();
	}


	/**
	 * Run from the command-line, with a list of URLs as argument.
	 * <P><B>NOTE:</B><BR>
	 * This code will run with all the documents in memory - if you
	 * want to unload each from memory after use, add code to store
	 * the corpus in a DataStore.
//	 */
//	static{
//		// initialise the GATE library
//		Out.prln("Initialising GATE...");
//		// Gate.init();
//		setUp();
//		// Load ANNIE plugin
//		File gateHome = Gate.getGateHome();
//		File pluginsHome = new File(gateHome, "plugins");
//		try {
//			Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "ANNIE").toURL());
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (GateException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		Out.prln("...GATE initialised");
//		// initialise ANNIE (this may take several minutes)
//
//		try {
//			annie.initAnnie();
//		} catch (GateException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	public static void main(String[] args) {
		String text = "Outstanding service We decided to give Hotel Monaco a try after reading the positive reviews here on TripAdvisor, and having had excellent experiences at Kimpton Hotels in other cities.I ve stayed at several downtown Seattle hotels before (including Sheraton, Grand Hyatt, The W) and while I had positive experiences at all of them, I would certainly return to Hotel Monaco before the others."+
				"It was truly outstanding in all areas from decor, cleanliness, and especially the service.The reservations and check-in process was flawless.";
		//	text = "I am going to the market.I'hv gone to the market.";
		String refinedText = StandAloneAnnie.getRefinedText(text);
		System.out.println(refinedText);
	}

}