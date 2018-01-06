package query;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Sentiment analysis based on lexicon consider word intensity and context
 * relation
 * 
 * @author senochow
 * 
 */
public class SentimentAnalysis {
	private static HashMap<String, Integer> positiveWords = new HashMap<String, Integer>();
	private static HashMap<String, Integer> negativeWords = new HashMap<String, Integer>();
	private static List<String> negationWords = new ArrayList<String>();
	private static HashMap<String, Double> intensifierWords = new HashMap<String, Double>();
	private int wordWindow = 2;
	private List<String> posWordcloud = new ArrayList<String>();
	private List<String> negWordcloud = new ArrayList<String>();
	private String filePath = "";

	/**
	 * Initialize, load file into memory
	 */
	public SentimentAnalysis() {
		loadFile();
	}

	public SentimentAnalysis(String rootPath) {
		this.filePath = rootPath;
		loadFile();
	}

	/**
	 * load file
	 */
	private void loadFile() {
		String filepath = "";
		if (this.filePath == null || this.filePath.length() == 0) {
			// filepath = "./";
			filepath = "./file/";
		} else {
			filepath = this.filePath;
		}
		if (positiveWords.size() <= 0) {
			try {
				BufferedReader readerPos = new BufferedReader(
						new InputStreamReader(new FileInputStream(filepath
								+ "positiveWords.txt"), "utf-8"));
				String line = "";
				while ((line = readerPos.readLine()) != null) {
					String[] wordVal = line.split("\t");
					positiveWords.put(wordVal[0], Integer.parseInt(wordVal[1]));
				}
				readerPos.close();
			} catch (IOException e) {
				System.out.println("load positiveWords.txt file failed!");
				e.printStackTrace();
			}
			try {
				BufferedReader readerNeg = new BufferedReader(
						new InputStreamReader(new FileInputStream(filepath
								+ "negativeWords.txt"), "utf-8"));
				String line = "";
				while ((line = readerNeg.readLine()) != null) {
					String[] wordVal = line.split("\t");
					negativeWords.put(wordVal[0], Integer.parseInt(wordVal[1]));
				}
				readerNeg.close();
			} catch (IOException e) {
				System.out.println("load negativeWords.txt file failed!");
				e.printStackTrace();
			}
			try {
				BufferedReader readerNega = new BufferedReader(
						new InputStreamReader(new FileInputStream(filepath
								+ "negationWords.txt"), "utf-8"));
				String line = "";
				while ((line = readerNega.readLine()) != null) {
					negationWords.add(line);
				}
				readerNega.close();
			} catch (IOException e) {
				System.out.println("load negationWords.txt file failed!");
				e.printStackTrace();
			}
			try {
				BufferedReader readerInten = new BufferedReader(
						new InputStreamReader(new FileInputStream(filepath
								+ "intensifierWords.txt"), "utf-8"));
				String line = "";
				while ((line = readerInten.readLine()) != null) {
					String[] wordVal = line.split("\t");
					intensifierWords.put(wordVal[0], Double
							.parseDouble(wordVal[1]));
				}
				readerInten.close();
			} catch (IOException e) {
				System.out.println("load intensifierWords.txt file failed!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * set word's emotion weight, Max emotion polarity is 9 , according to
	 * PKU_Wan's empirical analysis, negative words usually contribute more to
	 * the overall semantic orientation
	 * 
	 * @param word
	 * @return
	 */
	private double getWordEmotionVal(String word) {
		if (positiveWords.containsKey(word)) {
			this.posWordcloud.add(word);
			return (double) positiveWords.get(word) / 9;
		} else if (negativeWords.containsKey(word)) {
			this.negWordcloud.add(word);
			return -1.2 * (double) negativeWords.get(word) / 9;
		} else {
			return 0.0;
		}
	}

	/**
	 * get emotion from one sentence
	 * 
	 * @param sentence
	 * @return
	 */
	public double getEmotionFromSentence(List<String> sentence) {
		double emotionVal = 0.0;
		int emotionWordCnt = 0;
		if (sentence == null || sentence.size() == 0) {
			return 0.0;
		}
		int wordCnt = sentence.size();
		if (wordCnt < this.wordWindow) {
			for (int i = 0; i < wordCnt; i++) {
				if (getWordEmotionVal(sentence.get(i)) != 0) {
					emotionVal += getWordEmotionVal(sentence.get(i));
					emotionWordCnt++;
				}
			}
		} else {
			for (int i = 0; i < wordCnt; i++) {
				String word = sentence.get(i);
				double val = getWordEmotionVal(word);
				if (val != 0) {
					int beginPos = 0;
					// set begin position of slider window
					if (i >= wordWindow) {
						beginPos = i - wordWindow;
					}
					if (isContainNegationWord(beginPos, i, sentence)) {
						val = -val;
					}
					val = getIntensifierValue(beginPos, i, sentence) * val;
					emotionVal += val;
					emotionWordCnt++;
				}
			}
		}
		if (emotionWordCnt == 0) {
			return 0;
		} else {
			return emotionVal / emotionWordCnt;
		}

	}

	/**
	 * get sentiment value form a list of sentence list
	 * 
	 * @param sentences
	 * @return
	 */
	public double getEmotionFromSentenceList(List<List<String>> sentences) {
		if (sentences == null || sentences.size() == 0) {
			return 0;
		}
		double emotionVal = 0.0;
		for (List<String> list : sentences) {
			emotionVal += getEmotionFromSentence(list);
		}
		return emotionVal / sentences.size();
	}

	/**
	 * [beginPos, endPos) if contain negation words in this interval, consider
	 * multi-negation
	 * 
	 * @param bPos
	 * @param ePos
	 * @param words
	 * @return
	 */
	private boolean isContainNegationWord(int bPos, int ePos, List<String> words) {
		boolean m = false;
		for (int i = bPos; i < ePos; i++) {
			if (negationWords.contains(words.get(i))) {
				// System.out.println(words.get(i)+words.get(ePos));
				m = !m;
			}
		}
		return m;
	}

	/**
	 * [beginPos, endPos) if contain intensifier words in this interval, if
	 * contains return the intensifier value ,else return 1
	 * 
	 * @param bPos
	 * @param ePos
	 * @param words
	 * @return
	 */
	private double getIntensifierValue(int bPos, int ePos, List<String> words) {
		double m = 1.0;
		for (int i = bPos; i < ePos; i++) {
			if (intensifierWords.containsKey(words.get(i))) {
				// System.out.println(words.get(i)+words.get(ePos));
				m *= intensifierWords.get(words.get(i));
			}
		}
		return m;
	}

	public List<String> getNegWordcloud() {
		return negWordcloud;
	}

	public List<String> getPosWordcloud() {
		return posWordcloud;
	}
	
	public void clear() {
		posWordcloud.clear();
		negWordcloud.clear();
	}
	public static void main(String args[]){
		SentimentAnalysis sentimentAnalyzer = new SentimentAnalysis();
		List<String> words = new ArrayList<String>();
		double score = sentimentAnalyzer.getEmotionFromSentence(words);
	}
}
