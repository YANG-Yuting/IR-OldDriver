package query;

import java.io.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils; 
 
import java.io.PrintStream;    
import java.text.DecimalFormat;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class search{
	private static SentimentAnalysis sentimentAnalyzer = new SentimentAnalysis();
    private static TextProcess textProcessor = new TextProcess();
    private static QueryRecomender recommender = new QueryRecomender();/////////////////////////////////////////
    private static SimilarNews simNews = new SimilarNews();
	
	/*
	 * ��ʱ������ʱ�����Sort problems (UninvertedReader and DocValues)
	 */
    private static final FieldType INTEGER_FIELD_TYPE_STORED_SORTED = new FieldType();
    static {
    	INTEGER_FIELD_TYPE_STORED_SORTED.setTokenized(true);
    	INTEGER_FIELD_TYPE_STORED_SORTED.setOmitNorms(true);
    	INTEGER_FIELD_TYPE_STORED_SORTED.setIndexOptions(IndexOptions.DOCS);
    	INTEGER_FIELD_TYPE_STORED_SORTED.setNumericType(FieldType.NumericType.INT);
    	INTEGER_FIELD_TYPE_STORED_SORTED.setStored(true);
    	INTEGER_FIELD_TYPE_STORED_SORTED.setDocValuesType(DocValuesType.NUMERIC);
    	INTEGER_FIELD_TYPE_STORED_SORTED.freeze();
    }
    
    /*
     *��ҳ�оٵ�ǰ��������
     * ʵ��˼·���Ȳ�ѯ"?"�����������ŷ��أ��ٰ����������ȶȣ����򣬻����������
     */
    public static Result shouye() throws IOException, ParseException{
    	String queryStr = "?";
//-------------------------- ��ȡ������-----------------------------------------------------
        //����Ŀ¼
        String indexDir = "./indexDir"; //ע������ĳɾ���·�����Է�����
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        //ѡ���ѯ�ķִ���IKAnalyzer
        Analyzer analyzer = new IKAnalyzer();
                
//-------------------------- ѡ�������ʽ�����ֶκͶ��ֶμ������ؼ��ʼ�����ͨ���������----------        
        String[] searchField = {"title"};//�����ֶ�
        Query query;
        	// ͨ�����������ʱû�����϶��ֶμ�����
        Term term = new Term("title", queryStr);         
        query = new WildcardQuery(term);        
        
//-------------------------- ���������򣬵õ��м���----------------------------------------
        IndexReader indexReader=DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //����ضȡ�ʱ�䡢�ȶȣ���������������������޸ģ�֧�ֲ�ͬ������ʽ��
        //Ĭ�ϰ��ۺ������Ƚ����չʾ
        Sort sort = new Sort(new SortField("cmtnum", SortField.Type.INT, true));        
      
       TopDocs topDocs = indexSearcher.search(query, 10,sort);// ����ָ����ѯ������ѯ��ֻ����ǰn�����
        int count = topDocs.totalHits;// �ܵĲ�ѯ������
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;// ���յ÷ֽ���������ǰn���������Ϣ

        List<JSONObject> newslList = new ArrayList<JSONObject>();
//-------------------------- 5. �����м������õ��б�����---------------------------------------
        for (ScoreDoc scoreDoc : scoreDocs) {
            //float score = scoreDoc.score;// ��ضȵ÷֣�Ĭ������²��Լ��������֣����score�ֶ�ΪNAN        
          
            int docId = scoreDoc.doc; // Document�����ݿ���ڲ����(��Ψһ�ģ���lucene�Զ�����)
 
            // ���ݱ��ȡ��������Document����
            Document doc = indexSearcher.doc(docId);
 
            // ��Documentת��json
            //Article artical = new Article();
            JSONObject oneNew = new JSONObject();
            //articlJson.accumulate("id",doc.get("id") );
            oneNew.accumulate("content",doc.get("content") );
            oneNew.accumulate("ntime",doc.get("ntime") );
            oneNew.accumulate("newsurl",doc.get("newsurl") );
            oneNew.accumulate("newsid",doc.get("newsid") );
            oneNew.accumulate("title",doc.get("title") );
            oneNew.accumulate("author",doc.get("author") );
            oneNew.accumulate("type",doc.get("type") );
            oneNew.accumulate("cmtnum",doc.get("cmtnum") );
            oneNew.accumulate("cmtlist",doc.get("cmtlist") );
            oneNew.accumulate("modified",doc.get("modified") );
            oneNew.accumulate("ori",doc.get("ori") );
            oneNew.accumulate("sentimentLev", 0);
            oneNew.accumulate("similarNews", null);
            newslList.add(oneNew);
        }
        
        
        indexReader.close();
        //------------��ѯ����---------------
        
//-------------------------- 6. �������ս����ǰ̨---------------------------------------------       
        long time = 0; 
        List<JSONObject> first = new ArrayList<JSONObject>();
        List<String> queryRecom = new ArrayList<String>();
        Result result = new Result(count,time, newslList,queryRecom);        

        return result;
    	
    }
    
    
    /*
     * input: String queryStr
     * output: Result(my class, contains totalHitsNum��searchTime��newsList��queryRecom)
     * ����ǰ̨�����Ĳ�ѯ��string��������ѡ�񷵻���Result
     * Result��������������int��������ʱ�䣨double���������б���������Ƽ���
     */
    public static Result oriSearch(String queryStr) throws Throwable{
    	long startTime=System.currentTimeMillis();   //��ȡ��ʼʱ��  
    	
//-------------------------- 1. �Դ����Ĳ�ѯ���м򵥵�Ԥ����----------------------------------
    	//ͨ��������У�?������Ӣ�������µģ�������û������������µ�"��"���������
        if (queryStr.contains("��")){
        	queryStr = queryStr.replace( "��","?");     	
        }
//-------------------------- 2. ��ȡ������-----------------------------------------------------
        //����Ŀ¼
        String indexDir = "./indexDir";
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        //ѡ���ѯ�ķִ���IKAnalyzer
        Analyzer analyzer = new IKAnalyzer();
 
                
//-------------------------- 3. ѡ�������ʽ�����ֶκͶ��ֶμ������ؼ��ʼ�����ͨ���������----------        
        String[] searchField = {"title","content"};//�����ֶΣ���title��content��author�ֶμ�����֧�ֶ��ֶμ�����
        
        Query query;
        if(queryStr.contains("?") || queryStr.contains("*")){
        	// ͨ�����������ʱû�����϶��ֶμ�����
            Term term = new Term("content", queryStr);         
            query = new WildcardQuery(term); 
        }
        else{
        //�ؼ��ʼ���
        //�Ѳ�ѯ�ַ���תΪ��ѯ����(�洢�Ķ��Ƕ������ļ�����ͨ��String�϶��޷���ѯ�������Ҫת��)        
        QueryParser queryParser = new MultiFieldQueryParser(searchField,analyzer);//��title��content�ֶμ���
        query = queryParser.parse(queryStr);      
        }  
        
        
//-------------------------- 4. ���������򣬵õ��м���----------------------------------------
        IndexReader indexReader=DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //����ضȡ�ʱ�䡢�ȶȣ���������������������޸ģ�֧�ֲ�ͬ������ʽ��
        //Ĭ�ϰ��ۺ������Ƚ����չʾ
        Sort sort = new Sort(new SortField("content", SortField.Type.SCORE), new SortField("title", SortField.Type.SCORE),new SortField("ntime", SortField.Type.INT, true),new SortField("cmtnum", SortField.Type.INT, true));        
    
     
		// ���������������ľ��Ե÷�ʱ�����������������������Ƿ��֣�ע�⣬����������ʱ�������鿪����֣����󽵵����ܣ�
        //TopDocs topDocs = indexSearcher.search(query, 20, sort,true,true)
       
        TopDocs topDocs = indexSearcher.search(query, 10,sort);// ����ָ����ѯ������ѯ��ֻ����ǰn�����
        int count = topDocs.totalHits; // �ܵĲ�ѯ������
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;// ���յ÷ֽ���������ǰn���������Ϣ
        List<JSONObject> newsList = new ArrayList<JSONObject>(); //���ؽ���е�newsList
        
//-------------------------- 5. �����м������õ��б�����---------------------------------------
        for (ScoreDoc scoreDoc : scoreDocs) {
        	// �ȴ����������
            int docId = scoreDoc.doc; // Document�����ݿ���ڲ����(��Ψһ�ģ���lucene�Զ�����)        
            Document doc = indexSearcher.doc(docId); // ���ݱ��ȡ��������Document����
            // ��Documentת��json
            JSONObject oneNews = new JSONObject();
            oneNews.accumulate("content",doc.get("content") );
            oneNews.accumulate("ntime",doc.get("ntime") );
            oneNews.accumulate("newsurl",doc.get("newsurl") );
            oneNews.accumulate("newsid",doc.get("newsid") );
            oneNews.accumulate("title",doc.get("title") );
            oneNews.accumulate("author",doc.get("author") );
            oneNews.accumulate("type",doc.get("type") );
            oneNews.accumulate("cmtnum",doc.get("cmtnum") );
            oneNews.accumulate("cmtlist",doc.get("cmtlist") );
            oneNews.accumulate("modified",doc.get("modified") );
            oneNews.accumulate("ori",doc.get("ori") );
            //�Ը����ŵ����ۼ������ֵ�ȼ���1��6
    		String cmt = doc.get("cmtlist"); 
    		int sentimentLev = 0;
    		if (cmt == null){
    			sentimentLev = 0; //������Ϊ�գ�Ĭ��Ϊ0
    		}
    		else{
        		List<String> words = textProcessor.sentenceSplit(cmt); 	//�����۷ִʣ��õ��Ǵ������ģ�	    		
        		double score = sentimentAnalyzer.getEmotionFromSentence(words); //�����ۼ������ֵ�������ģ�
        		//�����ֵ�鵽[-1,1]������Ϊ6���ȼ�����
        		if(score < -1){
        			sentimentLev = 1;
        		}
        		else if (score >= -1 && score <-0.5){
        			sentimentLev = 2;
        		}
        		else if (score >= -0.5 && score <0){
        			sentimentLev = 3;
        		}
        		else if (score >= 0 && score <0.5){
        			sentimentLev = 4;
        		}
        		else if (score >= 0.5 && score <1){
        			sentimentLev = 5;
        		}
        		else if (score >= 1){
        			sentimentLev = 6;
        		}	
    		}
    		oneNews.accumulate("sentimentLev", sentimentLev); //���ֵ�ȼ�
    		
    		//�������Ų���
            SimilarNews simNews = new SimilarNews();
            ScoreDoc[] scoreDocs1 = simNews.Similar(docId); //���������б���һ����ԭ���� 
            ScoreDoc[] simiNews = scoreDocs1; //����ԭ��������������
            System.arraycopy(scoreDocs1, 1, simiNews, 0, 3);
           
            List<JSONObject> similarNews = new ArrayList<JSONObject>();
            //��������ŵ���������
            for (ScoreDoc scoreDoc1 : simiNews) {
                int docId1 = scoreDoc1.doc; // Document�����ݿ���ڲ����(��Ψһ�ģ���lucene�Զ�����)              
                Document doc1 = indexSearcher.doc(docId1);// ���ݱ��ȡ��������Document����
     
                // ��Documentת��json
                JSONObject oneNews1 = new JSONObject();
                oneNews1.accumulate("content",doc1.get("content") );
                oneNews1.accumulate("ntime",doc1.get("ntime") );
                oneNews1.accumulate("newsurl",doc1.get("newsurl") );
                oneNews1.accumulate("newsid",doc1.get("newsid") );
                oneNews1.accumulate("title",doc1.get("title") );
                oneNews1.accumulate("author",doc1.get("author") );
                oneNews1.accumulate("type",doc1.get("type") );
                oneNews1.accumulate("cmtnum",doc1.get("cmtnum") );
                oneNews1.accumulate("cmtlist",doc1.get("cmtlist") );
                oneNews1.accumulate("modified",doc1.get("modified") );
                oneNews1.accumulate("ori",doc1.get("ori") );
                oneNews1.accumulate("similarNews", null);
             
        		//�����ۼ������ֵ�ȼ���1��6
        		String cmt1 = doc1.get("cmtlist"); 
        		int sentimentLev1 = 0;
        		if (cmt1 == null){
        			sentimentLev1 = 0; //������Ϊ�գ�Ĭ��Ϊ0
        		}
        		else{
    	    		List<String> words = textProcessor.sentenceSplit(cmt1); 	//�����۷ִʣ��õ��Ǵ������ģ�	    		
    	    		double score = sentimentAnalyzer.getEmotionFromSentence(words); //�����ۼ������ֵ�������ģ�
    	    		//�����ֵ�鵽[-1,1]������Ϊ6���ȼ�����
    	    		if(score < -1){
    	    			sentimentLev1 = 1;
    	    		}
    	    		else if (score >= -1 && score <-0.5){
    	    			sentimentLev1 = 2;
    	    		}
    	    		else if (score >= -0.5 && score <0){
    	    			sentimentLev1 = 3;
    	    		}
    	    		else if (score >= 0 && score <0.5){
    	    			sentimentLev1 = 4;
    	    		}
    	    		else if (score >= 0.5 && score <1){
    	    			sentimentLev1 = 5;
    	    		}
    	    		else if (score >= 1){
    	    			sentimentLev1 = 6;
    	    		}
        		
        	}
        		oneNews1.accumulate("sentimentLev", sentimentLev1); //���ֵ�ȼ�	
        		similarNews.add(oneNews1);
            }
            
            
            
            oneNews.accumulate("similarNews", similarNews);
            System.out.println(oneNews);
            newsList.add(oneNews);
                 	
        }
   
        indexReader.close();
        //------------��ѯ����---------------
        
//-------------------------- 6. �������ս����ǰ̨---------------------------------------------
        long endTime=System.currentTimeMillis(); //��ȡ����ʱ�� 
        long time = endTime - startTime; //��ȡ����������ʱ��
        
        // ��������Ƽ�
        List<String> queryWords = textProcessor.sentenceSplit(queryStr);
        List<String> recomPhrase = recommender.getRecommendations(queryWords);

        Result result = new Result(count,time, newsList,recomPhrase);  //���ؽ��     
        return result;
    	
    }
    
    /*
     * input: String queryStr, String sortChoice
     * output: Result(my class, contains totalHitsNum��searchTime��firstNews��othersNews)
     * ����ǰ̨�����Ĳ�ѯ��string��������ʽ������ѡ�񷵻���Result
     * Result��������������int��������ʱ�䣨double������һ��������ż����������š����µ�������ţ������б�List<JSONObject>��ÿ��Ԫ����һ��JSONObject��
     */
    public static Result sortSearch(String queryStr, String sortChoice) throws Throwable{
    	long startTime=System.currentTimeMillis();   //��ȡ��ʼʱ��  
    	
    	//-------------------------- 1. �Դ����Ĳ�ѯ���м򵥵�Ԥ����----------------------------------
    	    	//ͨ��������У�?������Ӣ�������µģ�������û������������µ�"��"���������
    	        if (queryStr.contains("��")){
    	        	queryStr = queryStr.replace( "��","?");     	
    	        }
    	//-------------------------- 2. ��ȡ������-----------------------------------------------------
    	        //����Ŀ¼
    	        String indexDir = "./indexDir";
    	        Directory directory = FSDirectory.open(Paths.get(indexDir));
    	        //ѡ���ѯ�ķִ���IKAnalyzer
    	        Analyzer analyzer = new IKAnalyzer();
    	 
    	                
    	//-------------------------- 3. ѡ�������ʽ�����ֶκͶ��ֶμ������ؼ��ʼ�����ͨ���������----------        
    	        String[] searchField = {"title","content"};//�����ֶΣ���title��content��author�ֶμ�����֧�ֶ��ֶμ�����
    	        
    	        Query query;
    	        if(queryStr.contains("?") || queryStr.contains("*")){
    	        	// ͨ�����������ʱû�����϶��ֶμ�����
    	            Term term = new Term("content", queryStr);         
    	            query = new WildcardQuery(term); 
    	        }
    	        else{
    	        //�ؼ��ʼ���
    	        //�Ѳ�ѯ�ַ���תΪ��ѯ����(�洢�Ķ��Ƕ������ļ�����ͨ��String�϶��޷���ѯ�������Ҫת��)        
    	        QueryParser queryParser = new MultiFieldQueryParser(searchField,analyzer);//��title��content�ֶμ���
    	        query = queryParser.parse(queryStr);      
    	        }  
    	        
    	        
    	//-------------------------- 4. ���������򣬵õ��м���----------------------------------------
    	        IndexReader indexReader=DirectoryReader.open(directory);
    	        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    	        
    	      //����ضȡ�ʱ�䡢�ȶȣ���������������������޸ģ�֧�ֲ�ͬ������ʽ��
    	      //Ĭ�ϰ��ۺ������Ƚ����չʾ
    	      Sort sort = new Sort();
    	      if (sortChoice == "��ʱ��"){
    	      	sort = new Sort(new SortField("ntime", SortField.Type.INT, true));         
    	      }
    	      else if(sortChoice == "����ض�"){
    	      	sort = new Sort(new SortField("content", SortField.Type.SCORE), new SortField("title", SortField.Type.SCORE));
    	      }
    	      else if (sortChoice == "���ȶ�"){
    	      	sort = new Sort(new SortField("cmtnum", SortField.Type.INT, true));
    	      }
    	      else if (sortChoice == "�ۺ�"){
    	      	sort = new Sort(new SortField("content", SortField.Type.SCORE), new SortField("title", SortField.Type.SCORE),new SortField("ntime", SortField.Type.INT, true),new SortField("cmtnum", SortField.Type.INT, true));        
    	      }
    	     
    			// ���������������ľ��Ե÷�ʱ�����������������������Ƿ��֣�ע�⣬����������ʱ�������鿪����֣����󽵵����ܣ�
    	        //TopDocs topDocs = indexSearcher.search(query, 20, sort,true,true)
    	       
    	        TopDocs topDocs = indexSearcher.search(query, 10,sort);// ����ָ����ѯ������ѯ��ֻ����ǰn�����
    	        int count = topDocs.totalHits; // �ܵĲ�ѯ������
    	        ScoreDoc[] scoreDocs = topDocs.scoreDocs;// ���յ÷ֽ���������ǰn���������Ϣ
    	        List<JSONObject> newsList = new ArrayList<JSONObject>(); //���ؽ���е�newsList
    	        
    	//-------------------------- 5. �����м������õ��б�����---------------------------------------
    	        for (ScoreDoc scoreDoc : scoreDocs) {
    	        	// �ȴ����������
    	            int docId = scoreDoc.doc; // Document�����ݿ���ڲ����(��Ψһ�ģ���lucene�Զ�����)        
    	            Document doc = indexSearcher.doc(docId); // ���ݱ��ȡ��������Document����
    	            // ��Documentת��json
    	            JSONObject oneNews = new JSONObject();
    	            oneNews.accumulate("content",doc.get("content") );
    	            oneNews.accumulate("ntime",doc.get("ntime") );
    	            oneNews.accumulate("newsurl",doc.get("newsurl") );
    	            oneNews.accumulate("newsid",doc.get("newsid") );
    	            oneNews.accumulate("title",doc.get("title") );
    	            oneNews.accumulate("author",doc.get("author") );
    	            oneNews.accumulate("type",doc.get("type") );
    	            oneNews.accumulate("cmtnum",doc.get("cmtnum") );
    	            oneNews.accumulate("cmtlist",doc.get("cmtlist") );
    	            oneNews.accumulate("modified",doc.get("modified") );
    	            oneNews.accumulate("ori",doc.get("ori") );
    	            //�Ը����ŵ����ۼ������ֵ�ȼ���1��6
    	    		String cmt = doc.get("cmtlist"); 
    	    		int sentimentLev = 0;
    	    		if (cmt == null){
    	    			sentimentLev = 0; //������Ϊ�գ�Ĭ��Ϊ0
    	    		}
    	    		else{
    	        		List<String> words = textProcessor.sentenceSplit(cmt); 	//�����۷ִʣ��õ��Ǵ������ģ�	    		
    	        		double score = sentimentAnalyzer.getEmotionFromSentence(words); //�����ۼ������ֵ�������ģ�
    	        		//�����ֵ�鵽[-1,1]������Ϊ6���ȼ�����
    	        		if(score < -1){
    	        			sentimentLev = 1;
    	        		}
    	        		else if (score >= -1 && score <-0.5){
    	        			sentimentLev = 2;
    	        		}
    	        		else if (score >= -0.5 && score <0){
    	        			sentimentLev = 3;
    	        		}
    	        		else if (score >= 0 && score <0.5){
    	        			sentimentLev = 4;
    	        		}
    	        		else if (score >= 0.5 && score <1){
    	        			sentimentLev = 5;
    	        		}
    	        		else if (score >= 1){
    	        			sentimentLev = 6;
    	        		}	
    	    		}
    	    		oneNews.accumulate("sentimentLev", sentimentLev); //���ֵ�ȼ�
    	    		
    	    		//�������Ų���
    	            ScoreDoc[] scoreDocs1 = simNews.Similar(docId); //���������б���һ����ԭ���� 
    	            ScoreDoc[] simiNews = scoreDocs1; //����ԭ��������������
    	            System.arraycopy(scoreDocs1, 1, simiNews, 0, 3);
    	            List<JSONObject> similarNews = new ArrayList<JSONObject>();
    	            //��������ŵ���������
    	            for (ScoreDoc scoreDoc1 : simiNews) {
    	                int docId1 = scoreDoc1.doc; // Document�����ݿ���ڲ����(��Ψһ�ģ���lucene�Զ�����)              
    	                Document doc1 = indexSearcher.doc(docId1);// ���ݱ��ȡ��������Document����
    	     
    	                // ��Documentת��json
    	                JSONObject oneNews1 = new JSONObject();
    	                oneNews1.accumulate("content",doc1.get("content") );
    	                oneNews1.accumulate("ntime",doc1.get("ntime") );
    	                oneNews1.accumulate("newsurl",doc1.get("newsurl") );
    	                oneNews1.accumulate("newsid",doc1.get("newsid") );
    	                oneNews1.accumulate("title",doc1.get("title") );
    	                oneNews1.accumulate("author",doc1.get("author") );
    	                oneNews1.accumulate("type",doc1.get("type") );
    	                oneNews1.accumulate("cmtnum",doc1.get("cmtnum") );
    	                oneNews1.accumulate("cmtlist",doc1.get("cmtlist") );
    	                oneNews1.accumulate("modified",doc1.get("modified") );
    	                oneNews1.accumulate("ori",doc1.get("ori") );
    	                oneNews1.accumulate("similarNews", null);
    	             
    	        		//�����ۼ������ֵ�ȼ���1��6
    	        		String cmt1 = doc1.get("cmtlist"); 
    	        		int sentimentLev1 = 0;
    	        		if (cmt1 == null){
    	        			sentimentLev1 = 0; //������Ϊ�գ�Ĭ��Ϊ0
    	        		}
    	        		else{
    	    	    		List<String> words = textProcessor.sentenceSplit(cmt1); 	//�����۷ִʣ��õ��Ǵ������ģ�	    		
    	    	    		double score = sentimentAnalyzer.getEmotionFromSentence(words); //�����ۼ������ֵ�������ģ�
    	    	    		//�����ֵ�鵽[-1,1]������Ϊ6���ȼ�����
    	    	    		if(score < -1){
    	    	    			sentimentLev1 = 1;
    	    	    		}
    	    	    		else if (score >= -1 && score <-0.5){
    	    	    			sentimentLev1 = 2;
    	    	    		}
    	    	    		else if (score >= -0.5 && score <0){
    	    	    			sentimentLev1 = 3;
    	    	    		}
    	    	    		else if (score >= 0 && score <0.5){
    	    	    			sentimentLev1 = 4;
    	    	    		}
    	    	    		else if (score >= 0.5 && score <1){
    	    	    			sentimentLev1 = 5;
    	    	    		}
    	    	    		else if (score >= 1){
    	    	    			sentimentLev1 = 6;
    	    	    		}
    	        		
    	        	}
    	        		oneNews1.accumulate("sentimentLev", sentimentLev1); //���ֵ�ȼ�	
    	        		similarNews.add(oneNews1);
    	            }
    	            
    	            
    	            
    	            oneNews.accumulate("similarNews", similarNews);
    	            System.out.println(oneNews);
    	            newsList.add(oneNews);
    	                 	
    	        }
    	   
    	        indexReader.close();
    	        //------------��ѯ����---------------
    	        
    	//-------------------------- 6. �������ս����ǰ̨---------------------------------------------
    	        long endTime=System.currentTimeMillis(); //��ȡ����ʱ�� 
    	        long time = endTime - startTime; //��ȡ����������ʱ��
    	        
    	        // ��������Ƽ�
    	        List<String> queryWords = textProcessor.sentenceSplit(queryStr);
    	        List<String> recomPhrase = recommender.getRecommendations(queryWords);

    	        System.out.println(recomPhrase);
    	        Result result = new Result(count,time, newsList,recomPhrase);  //���ؽ��     
    	        return result;
    	    	
    }

        
	public static void main(String []args)throws Throwable {  	
		// ��������
		//ע�⣺*��?�����ڲ�ѯ�ʵ���ǰ��;
        String str = "��������";
        System.out.println("\nString is : "+str);
        String sortChoice = "��ʱ��";
        
        //�û�������Ĭ�Ϸ��ذ��ۺ�����Ľ�� 
        //Result result = new Result(oriSearch(str));
        Result result = new Result(sortSearch(str,sortChoice));
        
        //��ʾ���
        System.out.println("��ʱ�� "+(result.getSearchTime())*0.001+"s");         
        System.out.println("�ҵ�Լ : "+result.getHitsNum()+"����ؽ�� ");
        System.out.println("��������Ƽ���"+ result.getQueryRecom());
        System.out.println("������ţ�");
        
        for (JSONObject oneNews : result.getNewsList()) {
        	System.out.println(oneNews.get("similarNews"));
        }  
        
        
//        Result result = new Result(shouye());
//        //��ʾ���
//        System.out.println("��ʱ�� "+(result.getSearchTime())*0.001+"s");         
//        System.out.println("\n�ҵ�Լ : "+result.getHitsNum()+"����ؽ�� ");        
//        for (JSONObject oneNews : result.getNewsList()) {
//        	System.out.println(oneNews);
//        }   
//        
	} 
}
	






