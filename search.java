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
	 * 按时间排序时，解决Sort problems (UninvertedReader and DocValues)
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
     *首页列举当前最热新闻
     * 实现思路：先查询"?"，将所有新闻返回，再按评论数（热度）排序，获得最热新闻
     */
    public static Result shouye() throws IOException, ParseException{
    	String queryStr = "?";
//-------------------------- 读取索引库-----------------------------------------------------
        //索引目录
        String indexDir = "./indexDir"; //注意这里改成绝对路径，以防出错。
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        //选择查询的分词器IKAnalyzer
        Analyzer analyzer = new IKAnalyzer();
                
//-------------------------- 选择检索方式（单字段和多字段检索；关键词检索和通配符检索）----------        
        String[] searchField = {"title"};//检索字段
        Query query;
        	// 通配符检索（暂时没有用上多字段检索）
        Term term = new Term("title", queryStr);         
        query = new WildcardQuery(term);        
        
//-------------------------- 检索及排序，得到中间结果----------------------------------------
        IndexReader indexReader=DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //按相关度、时间、热度（评论数）排序（这里可以修改，支持不同的排序方式）
        //默认按综合排序，先将结果展示
        Sort sort = new Sort(new SortField("cmtnum", SortField.Type.INT, true));        
      
       TopDocs topDocs = indexSearcher.search(query, 10,sort);// 根据指定查询条件查询，只返回前n条结果
        int count = topDocs.totalHits;// 总的查询命中数
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;// 按照得分进行排序后的前n条结果的信息

        List<JSONObject> newslList = new ArrayList<JSONObject>();
//-------------------------- 5. 处理中间结果，得到列表数据---------------------------------------
        for (ScoreDoc scoreDoc : scoreDocs) {
            //float score = scoreDoc.score;// 相关度得分，默认情况下不对检索结果打分，因此score字段为NAN        
          
            int docId = scoreDoc.doc; // Document在数据库的内部编号(是唯一的，由lucene自动生成)
 
            // 根据编号取出真正的Document数据
            Document doc = indexSearcher.doc(docId);
 
            // 把Document转成json
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
        //------------查询结束---------------
        
//-------------------------- 6. 返回最终结果到前台---------------------------------------------       
        long time = 0; 
        List<JSONObject> first = new ArrayList<JSONObject>();
        List<String> queryRecom = new ArrayList<String>();
        Result result = new Result(count,time, newslList,queryRecom);        

        return result;
    	
    }
    
    
    /*
     * input: String queryStr
     * output: Result(my class, contains totalHitsNum、searchTime、newsList、queryRecom)
     * 接收前台传来的查询（string），排序选择返回类Result
     * Result包括总命中数（int），搜索时间（double）、新闻列表、相关搜索推荐）
     */
    public static Result oriSearch(String queryStr) throws Throwable{
    	long startTime=System.currentTimeMillis();   //获取开始时间  
    	
//-------------------------- 1. 对传来的查询进行简单的预处理----------------------------------
    	//通配符检索中，?必须是英文输入下的，因此若用户误输了中文下的"？"，将其改正
        if (queryStr.contains("？")){
        	queryStr = queryStr.replace( "？","?");     	
        }
//-------------------------- 2. 读取索引库-----------------------------------------------------
        //索引目录
        String indexDir = "./indexDir";
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        //选择查询的分词器IKAnalyzer
        Analyzer analyzer = new IKAnalyzer();
 
                
//-------------------------- 3. 选择检索方式（单字段和多字段检索；关键词检索和通配符检索）----------        
        String[] searchField = {"title","content"};//检索字段（对title、content、author字段检索，支持多字段检索）
        
        Query query;
        if(queryStr.contains("?") || queryStr.contains("*")){
        	// 通配符检索（暂时没有用上多字段检索）
            Term term = new Term("content", queryStr);         
            query = new WildcardQuery(term); 
        }
        else{
        //关键词检索
        //把查询字符串转为查询对象(存储的都是二进制文件，普通的String肯定无法查询，因此需要转换)        
        QueryParser queryParser = new MultiFieldQueryParser(searchField,analyzer);//在title和content字段检索
        query = queryParser.parse(queryStr);      
        }  
        
        
//-------------------------- 4. 检索及排序，得到中间结果----------------------------------------
        IndexReader indexReader=DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //按相关度、时间、热度（评论数）排序（这里可以修改，支持不同的排序方式）
        //默认按综合排序，先将结果展示
        Sort sort = new Sort(new SortField("content", SortField.Type.SCORE), new SortField("title", SortField.Type.SCORE),new SortField("ntime", SortField.Type.INT, true),new SortField("cmtnum", SortField.Type.INT, true));        
    
     
		// 当你想获得排序结果的绝对得分时（后面两个布尔变量控制是否打分，注意，当数据量大时，不建议开启打分，会大大降低性能）
        //TopDocs topDocs = indexSearcher.search(query, 20, sort,true,true)
       
        TopDocs topDocs = indexSearcher.search(query, 10,sort);// 根据指定查询条件查询，只返回前n条结果
        int count = topDocs.totalHits; // 总的查询命中数
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;// 按照得分进行排序后的前n条结果的信息
        List<JSONObject> newsList = new ArrayList<JSONObject>(); //返回结果中的newsList
        
//-------------------------- 5. 处理中间结果，得到列表数据---------------------------------------
        for (ScoreDoc scoreDoc : scoreDocs) {
        	// 先处理该条新闻
            int docId = scoreDoc.doc; // Document在数据库的内部编号(是唯一的，由lucene自动生成)        
            Document doc = indexSearcher.doc(docId); // 根据编号取出真正的Document数据
            // 把Document转成json
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
            //对该新闻的评论计算情感值等级，1至6
    		String cmt = doc.get("cmtlist"); 
    		int sentimentLev = 0;
    		if (cmt == null){
    			sentimentLev = 0; //若评论为空，默认为0
    		}
    		else{
        		List<String> words = textProcessor.sentenceSplit(cmt); 	//对评论分词（用的是川川给的）	    		
        		double score = sentimentAnalyzer.getEmotionFromSentence(words); //对评论计算情感值（川川的）
        		//将情感值归到[-1,1]，并分为6个等级区间
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
    		oneNews.accumulate("sentimentLev", sentimentLev); //情感值等级
    		
    		//相似新闻查找
            SimilarNews simNews = new SimilarNews();
            ScoreDoc[] scoreDocs1 = simNews.Similar(docId); //相似新闻列表，第一个是原新闻 
            ScoreDoc[] simiNews = scoreDocs1; //除了原新闻以外其他的
            System.arraycopy(scoreDocs1, 1, simiNews, 0, 3);
           
            List<JSONObject> similarNews = new ArrayList<JSONObject>();
            //处理该新闻的相似新闻
            for (ScoreDoc scoreDoc1 : simiNews) {
                int docId1 = scoreDoc1.doc; // Document在数据库的内部编号(是唯一的，由lucene自动生成)              
                Document doc1 = indexSearcher.doc(docId1);// 根据编号取出真正的Document数据
     
                // 把Document转成json
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
             
        		//对评论计算情感值等级，1至6
        		String cmt1 = doc1.get("cmtlist"); 
        		int sentimentLev1 = 0;
        		if (cmt1 == null){
        			sentimentLev1 = 0; //若评论为空，默认为0
        		}
        		else{
    	    		List<String> words = textProcessor.sentenceSplit(cmt1); 	//对评论分词（用的是川川给的）	    		
    	    		double score = sentimentAnalyzer.getEmotionFromSentence(words); //对评论计算情感值（川川的）
    	    		//将情感值归到[-1,1]，并分为6个等级区间
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
        		oneNews1.accumulate("sentimentLev", sentimentLev1); //情感值等级	
        		similarNews.add(oneNews1);
            }
            
            
            
            oneNews.accumulate("similarNews", similarNews);
            System.out.println(oneNews);
            newsList.add(oneNews);
                 	
        }
   
        indexReader.close();
        //------------查询结束---------------
        
//-------------------------- 6. 返回最终结果到前台---------------------------------------------
        long endTime=System.currentTimeMillis(); //获取结束时间 
        long time = endTime - startTime; //获取搜索及排序时间
        
        // 相关搜索推荐
        List<String> queryWords = textProcessor.sentenceSplit(queryStr);
        List<String> recomPhrase = recommender.getRecommendations(queryWords);

        Result result = new Result(count,time, newsList,recomPhrase);  //返回结果     
        return result;
    	
    }
    
    /*
     * input: String queryStr, String sortChoice
     * output: Result(my class, contains totalHitsNum、searchTime、firstNews、othersNews)
     * 接收前台传来的查询（string）及排序方式，排序选择返回类Result
     * Result包括总命中数（int），搜索时间（double）、第一条相关新闻及其相似新闻、余下的相关新闻（新闻列表List<JSONObject>，每个元素是一个JSONObject）
     */
    public static Result sortSearch(String queryStr, String sortChoice) throws Throwable{
    	long startTime=System.currentTimeMillis();   //获取开始时间  
    	
    	//-------------------------- 1. 对传来的查询进行简单的预处理----------------------------------
    	    	//通配符检索中，?必须是英文输入下的，因此若用户误输了中文下的"？"，将其改正
    	        if (queryStr.contains("？")){
    	        	queryStr = queryStr.replace( "？","?");     	
    	        }
    	//-------------------------- 2. 读取索引库-----------------------------------------------------
    	        //索引目录
    	        String indexDir = "./indexDir";
    	        Directory directory = FSDirectory.open(Paths.get(indexDir));
    	        //选择查询的分词器IKAnalyzer
    	        Analyzer analyzer = new IKAnalyzer();
    	 
    	                
    	//-------------------------- 3. 选择检索方式（单字段和多字段检索；关键词检索和通配符检索）----------        
    	        String[] searchField = {"title","content"};//检索字段（对title、content、author字段检索，支持多字段检索）
    	        
    	        Query query;
    	        if(queryStr.contains("?") || queryStr.contains("*")){
    	        	// 通配符检索（暂时没有用上多字段检索）
    	            Term term = new Term("content", queryStr);         
    	            query = new WildcardQuery(term); 
    	        }
    	        else{
    	        //关键词检索
    	        //把查询字符串转为查询对象(存储的都是二进制文件，普通的String肯定无法查询，因此需要转换)        
    	        QueryParser queryParser = new MultiFieldQueryParser(searchField,analyzer);//在title和content字段检索
    	        query = queryParser.parse(queryStr);      
    	        }  
    	        
    	        
    	//-------------------------- 4. 检索及排序，得到中间结果----------------------------------------
    	        IndexReader indexReader=DirectoryReader.open(directory);
    	        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    	        
    	      //按相关度、时间、热度（评论数）排序（这里可以修改，支持不同的排序方式）
    	      //默认按综合排序，先将结果展示
    	      Sort sort = new Sort();
    	      if (sortChoice == "按时间"){
    	      	sort = new Sort(new SortField("ntime", SortField.Type.INT, true));         
    	      }
    	      else if(sortChoice == "按相关度"){
    	      	sort = new Sort(new SortField("content", SortField.Type.SCORE), new SortField("title", SortField.Type.SCORE));
    	      }
    	      else if (sortChoice == "按热度"){
    	      	sort = new Sort(new SortField("cmtnum", SortField.Type.INT, true));
    	      }
    	      else if (sortChoice == "综合"){
    	      	sort = new Sort(new SortField("content", SortField.Type.SCORE), new SortField("title", SortField.Type.SCORE),new SortField("ntime", SortField.Type.INT, true),new SortField("cmtnum", SortField.Type.INT, true));        
    	      }
    	     
    			// 当你想获得排序结果的绝对得分时（后面两个布尔变量控制是否打分，注意，当数据量大时，不建议开启打分，会大大降低性能）
    	        //TopDocs topDocs = indexSearcher.search(query, 20, sort,true,true)
    	       
    	        TopDocs topDocs = indexSearcher.search(query, 10,sort);// 根据指定查询条件查询，只返回前n条结果
    	        int count = topDocs.totalHits; // 总的查询命中数
    	        ScoreDoc[] scoreDocs = topDocs.scoreDocs;// 按照得分进行排序后的前n条结果的信息
    	        List<JSONObject> newsList = new ArrayList<JSONObject>(); //返回结果中的newsList
    	        
    	//-------------------------- 5. 处理中间结果，得到列表数据---------------------------------------
    	        for (ScoreDoc scoreDoc : scoreDocs) {
    	        	// 先处理该条新闻
    	            int docId = scoreDoc.doc; // Document在数据库的内部编号(是唯一的，由lucene自动生成)        
    	            Document doc = indexSearcher.doc(docId); // 根据编号取出真正的Document数据
    	            // 把Document转成json
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
    	            //对该新闻的评论计算情感值等级，1至6
    	    		String cmt = doc.get("cmtlist"); 
    	    		int sentimentLev = 0;
    	    		if (cmt == null){
    	    			sentimentLev = 0; //若评论为空，默认为0
    	    		}
    	    		else{
    	        		List<String> words = textProcessor.sentenceSplit(cmt); 	//对评论分词（用的是川川给的）	    		
    	        		double score = sentimentAnalyzer.getEmotionFromSentence(words); //对评论计算情感值（川川的）
    	        		//将情感值归到[-1,1]，并分为6个等级区间
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
    	    		oneNews.accumulate("sentimentLev", sentimentLev); //情感值等级
    	    		
    	    		//相似新闻查找
    	            ScoreDoc[] scoreDocs1 = simNews.Similar(docId); //相似新闻列表，第一个是原新闻 
    	            ScoreDoc[] simiNews = scoreDocs1; //除了原新闻以外其他的
    	            System.arraycopy(scoreDocs1, 1, simiNews, 0, 3);
    	            List<JSONObject> similarNews = new ArrayList<JSONObject>();
    	            //处理该新闻的相似新闻
    	            for (ScoreDoc scoreDoc1 : simiNews) {
    	                int docId1 = scoreDoc1.doc; // Document在数据库的内部编号(是唯一的，由lucene自动生成)              
    	                Document doc1 = indexSearcher.doc(docId1);// 根据编号取出真正的Document数据
    	     
    	                // 把Document转成json
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
    	             
    	        		//对评论计算情感值等级，1至6
    	        		String cmt1 = doc1.get("cmtlist"); 
    	        		int sentimentLev1 = 0;
    	        		if (cmt1 == null){
    	        			sentimentLev1 = 0; //若评论为空，默认为0
    	        		}
    	        		else{
    	    	    		List<String> words = textProcessor.sentenceSplit(cmt1); 	//对评论分词（用的是川川给的）	    		
    	    	    		double score = sentimentAnalyzer.getEmotionFromSentence(words); //对评论计算情感值（川川的）
    	    	    		//将情感值归到[-1,1]，并分为6个等级区间
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
    	        		oneNews1.accumulate("sentimentLev", sentimentLev1); //情感值等级	
    	        		similarNews.add(oneNews1);
    	            }
    	            
    	            
    	            
    	            oneNews.accumulate("similarNews", similarNews);
    	            System.out.println(oneNews);
    	            newsList.add(oneNews);
    	                 	
    	        }
    	   
    	        indexReader.close();
    	        //------------查询结束---------------
    	        
    	//-------------------------- 6. 返回最终结果到前台---------------------------------------------
    	        long endTime=System.currentTimeMillis(); //获取结束时间 
    	        long time = endTime - startTime; //获取搜索及排序时间
    	        
    	        // 相关搜索推荐
    	        List<String> queryWords = textProcessor.sentenceSplit(queryStr);
    	        List<String> recomPhrase = recommender.getRecommendations(queryWords);

    	        System.out.println(recomPhrase);
    	        Result result = new Result(count,time, newsList,recomPhrase);  //返回结果     
    	        return result;
    	    	
    }

        
	public static void main(String []args)throws Throwable {  	
		// 搜索条件
		//注意：*和?不能在查询词的最前面;
        String str = "古力娜扎";
        System.out.println("\nString is : "+str);
        String sortChoice = "按时间";
        
        //用户检索，默认返回按综合排序的结果 
        //Result result = new Result(oriSearch(str));
        Result result = new Result(sortSearch(str,sortChoice));
        
        //显示结果
        System.out.println("用时： "+(result.getSearchTime())*0.001+"s");         
        System.out.println("找到约 : "+result.getHitsNum()+"条相关结果 ");
        System.out.println("相关搜索推荐："+ result.getQueryRecom());
        System.out.println("相关新闻：");
        
        for (JSONObject oneNews : result.getNewsList()) {
        	System.out.println(oneNews.get("similarNews"));
        }  
        
        
//        Result result = new Result(shouye());
//        //显示结果
//        System.out.println("用时： "+(result.getSearchTime())*0.001+"s");         
//        System.out.println("\n找到约 : "+result.getHitsNum()+"条相关结果 ");        
//        for (JSONObject oneNews : result.getNewsList()) {
//        	System.out.println(oneNews);
//        }   
//        
	} 
}
	






