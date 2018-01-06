package query;

import java.io.File;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.apache.lucene.analysis.*;

public class SimilarNews {
	public static ScoreDoc[] Similar(int docID) throws Throwable {
		String filepath = "H:/TiTi的java程序/ir/indexDir";
		Directory directory = FSDirectory.open(new File(filepath).toPath()); 
		DirectoryReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);

		MoreLikeThis mlt = new MoreLikeThis(reader);
		mlt.setFieldNames(new String [] {"title"});
		mlt.setMinTermFreq(1);
		mlt.setMinDocFreq(1); 
		mlt.setMinWordLen(1);
		Analyzer analyzer = new IKAnalyzer();
		mlt.setAnalyzer(analyzer);

		Query query = mlt.like(docID);
		//System.out.println(query.toString());

		TopDocs topDocs = searcher.search(query,4);
		ScoreDoc[] returnDocs = new ScoreDoc[4];
		if(topDocs.totalHits == 0){
			System.out.println("None like this");
			reader.close();
			directory.close();
			return null;
		} else {
			returnDocs = topDocs.scoreDocs;
			reader.close();
			directory.close();
			return returnDocs;
		}
	}
	public static void main(String [] args) throws Throwable {
		int docID = 100;
		ScoreDoc[] ScoreDocs1 = Similar(docID);
}
}