package query;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//import util.TextProcess;

public class QueryRecomender {
	private List<String> phraseList = new ArrayList<String>();
	String filepath = "";
	
	public QueryRecomender() {
		loadfile();
	}
	
	private void loadfile() {
		String filepath = "";
		if (this.filepath == null || this.filepath.length() == 0) {
			// filepath = "./";
			filepath = "./file/phrase.txt";
		} else {
			filepath = this.filepath;
		}
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(filepath), "utf-8"));
			String str = "";
			while((str = reader.readLine()) != null) {
				phraseList.add(str.trim());
			}
			reader.close();
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double calculateSim(List<String> queryWords, String phrase) {
		double score = 0.0;
		for(int i = 0; i < queryWords.size(); i++) {
			if(phrase.contains(queryWords.get(i))) {
				int ind = phrase.indexOf(queryWords.get(i));
				score = score + 1.0/(ind + (i + 1)*1.5 + 5.0);
			}
		}
		return score;
		
	}
	
	public List<String> getRecommendations(List<String> queryWords){
		List<String> recoms = new ArrayList<String>();
		Map<Integer, Double> simMaps = new HashMap<Integer, Double>();
		for(int i = 0; i < phraseList.size(); i++) {
			double simScore = calculateSim(queryWords, phraseList.get(i));
			if(simScore > 0) {
				simMaps.put(i, simScore);
			}
		}
		
		
		List<Map.Entry<Integer, Double>> entrys = new ArrayList<Map.Entry<Integer,Double>>(simMaps.entrySet());
		Collections.sort(entrys,new Comparator<Map.Entry<Integer, Double>>(){
			public int compare(Entry<Integer,Double> o1, Entry<Integer,Double> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		
		int k = (entrys.size() > 6) ? 6 : entrys.size();
//		System.out.println("鐩稿叧鎺ㄨ崘锛�");
		for(int i = 0; i < k; i ++) {
			recoms.add(phraseList.get(entrys.get(i).getKey()));
//			System.out.println(entrys.get(i).getValue()+ "\t" +phraseList.get(entrys.get(i).getKey()));
		}
		return recoms;
	}
	
//	public static void main(String []args) {
//		QueryRecomender recommender = new QueryRecomender();
//		TextProcess processer = new TextProcess();
//		String query = "鏁版嵁鎸栨帢涓庝汉宸ユ櫤鑳�";
//		List<String> words = processer.sentenceSplit(query);
//		System.out.println("鏌ヨ璇彞锛歕n" + query + "\n\n");
//		List<String> recoms = recommender.getRecommendations(words);
//		System.out.println("鐩稿叧鎺ㄨ崘锛�");
//		for(String str: recoms)
//			System.out.println(str);
//	}
}
