package query;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class Result {
	/*
	 * Result类，用来返回给前台。
	 * 包括四个属性，总命中数totalHitsNum，搜索总时间searchTime、排序结果newslList(JSONArray，每个元素是一个json对象)、相关搜索推荐。
	 * 
	 */
	private int totalHitsNum;
	private long searchTime;
	private List<JSONObject> newsList;
	private List<String> queryRecom;

	
    //-----------constructors----------
	public Result() {
        super();
        this.totalHitsNum = 0;
        this.searchTime = 0;
        this.newsList = new ArrayList<JSONObject>();
        this.queryRecom = new ArrayList<String>();
    }
	public Result(Result nresult) {
        super();
        this.totalHitsNum = nresult.totalHitsNum;
        this.searchTime = nresult.searchTime;
        this.newsList = nresult.newsList;
        this.queryRecom = nresult.queryRecom;
    }
	
     public Result(int num, long time, List<JSONObject> news, List<String> querys ) {
        super();
        this.totalHitsNum = num;
        this.searchTime = time;
        this.newsList = news;
        this.queryRecom = querys;
    }
     
     public int getHitsNum(){
    	 return this.totalHitsNum;
     }
     public long getSearchTime(){
    	 return this.searchTime;
     }    
     public List<JSONObject> getNewsList(){
    	 return this.newsList;
     }
     public List<String> getQueryRecom(){
    	 return this.queryRecom;
     }
     
}
