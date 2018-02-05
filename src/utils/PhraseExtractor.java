package utils;

import java.util.Iterator;

import org.json.JSONObject;

public class PhraseExtractor {
	/**
	 * Extract phrases by semantic type from json text
	 * @param text - json text
	 * @param type - semantic type
	 */
	public void extractBySeamType(String text, String type) {
		JSONObject json = new JSONObject(text);
		Iterator<String> sections = json.keys();
	}
}