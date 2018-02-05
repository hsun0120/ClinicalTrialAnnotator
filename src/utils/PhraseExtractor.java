package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import au.com.bytecode.opencsv.CSVWriter;

public class PhraseExtractor {
	/**
	 * Extract phrases by semantic type from json text
	 * @param text - json text
	 * @param type - semantic type
	 * @param writer - a CSVWriter
	 */
	public void extractBySemType(String text, String type, CSVWriter writer) {
		JSONObject json = new JSONObject(text);
		Iterator<String> sections = json.keys();
		while(sections.hasNext()) {
			String section = sections.next();
			if(section.equals("nct_id")) continue;

			Iterator<String> it = json.getJSONObject(section).keys();
			while(it.hasNext()) {
				String keyName = it.next();
				JSONArray sentences = json.getJSONObject(section).getJSONArray(keyName)
						.getJSONArray(3); //Get an array of sentences
				for(int index = 0; index < sentences.length(); index++) {
					JSONObject sentenceAnnot = sentences.getJSONObject(index);
					JSONArray phrases = sentenceAnnot.getJSONArray("Phrases");
					HashSet<String> set = new HashSet<>();
					for(int idx = 0; idx < phrases.length(); idx++) {
						JSONArray mappings = phrases.getJSONObject(idx).getJSONArray("Mappings");
						for(int i = 0; i < mappings.length(); i++) {
							JSONArray candidates = mappings.getJSONObject(i).getJSONArray
									("MappingCandidates");
							for(int j = 0; j < candidates.length(); j++) {
								JSONObject candidate = candidates.getJSONObject(j);
								if(this.findSemType(candidate.getJSONArray("SemTypes"), type) 
										&& set.add(candidate.getString("CandidatePreferred"))) {
									String[] tmp = new String[5];
									tmp[0] = json.getString("nct_id");
									if(json.getJSONObject(section).length() > 1)
										tmp[1] = keyName;
									else
										tmp[1] = section;
									tmp[2] = sentenceAnnot.getString("UttText");
									tmp[3] = candidate.getString("CandidateMatched");
									tmp[4] = candidate.getString("CandidatePreferred");
									writer.writeNext(tmp);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private boolean findSemType(JSONArray arr, String type) {
		for(int i = 0; i < arr.length(); i++)
			if(arr.getString(i).equals(type)) return true;
		return false;
	}
	
	public static void main(String[] args) throws IOException {
		PhraseExtractor pe = new PhraseExtractor();
		File dir = new File(args[0]);
		String[] header = {"nct_id", "section", "UttText", "CandidateMatched",
				"CandidatePreferred"};
		CSVWriter writer = new CSVWriter(new FileWriter("gngm.csv"));
		writer.writeNext(header);
		for(final File file : dir.listFiles())
			try(Scanner sc = new Scanner(new FileReader(file.getPath()))) {
				pe.extractBySemType(sc.nextLine(), "gngm", writer);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		writer.close();
	}
}