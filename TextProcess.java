package query;

import java.util.ArrayList;
import java.util.List;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

public class TextProcess {
	
	
	public List<String> sentenceSplit (String line){
		
		line=line.replaceAll("[\\s;#@,.&!?：；，。！？'\\\"%\\[\\]\\(\\)\\（\\）\\:\\<\\>\\《\\》“”‘’~_——-、【】-]","");
		line=line.replaceAll("\\～","");
		//去除连续的字母
		line=line.replaceAll("[a-zA-Z]{7,}","");
		//去除连续的数字
		line=line.replaceAll("[0-9]{5,}","");
		List<Term> terms = ToAnalysis.parse(line);
		
		List<String> words = new ArrayList<String>();
//		System.out.println(terms);
		for(Term t:terms){
//			System.out.println(t.getName());
//			System.out.println(t.getNatureStr());
			if(t.getNatureStr().trim().equals("en")) continue;
			if(t.getNatureStr().trim().equals("m")) continue;
			if(t.getNatureStr().trim().equals("w")) continue;
			if(t.getNatureStr().trim().equals("ul")) continue;
			if(t.getNatureStr().trim().equals("uj")) continue;
			if(t.getNatureStr().trim().equals("p")) continue;
			if(t.getNatureStr().trim().equals("r")) continue;
			if(t.getNatureStr().trim().equals("f")) continue;
			if(t.getNatureStr().trim().equals("b")) continue;
			if(t.getNatureStr().trim().equals("y")) continue;
			if(t.getNatureStr().trim().equals("d")) continue;
			if(words.contains(t.getName().trim())) continue;
			words.add(t.getName().trim());
		}
		
		return words;
	}
	
public List<Term> sentenceSplitByTerm (String line){
		
		line=line.replaceAll("[\\s;#@,.&!?：；，。！？'\\\"%\\[\\]\\(\\)\\（\\）\\:\\<\\>\\《\\》“”‘’~_——-、【】-]","");
		line=line.replaceAll("\\～","");
		//去除连续的字母
		line=line.replaceAll("[a-zA-Z]{7,}","");
		//去除连续的数字
		line=line.replaceAll("[0-9]{5,}","");
		List<Term> terms = ToAnalysis.parse(line);
		
		List<String> words = new ArrayList<String>();
		List<Term> wterms = new ArrayList<Term>();
//		System.out.println(terms);
		for(Term t:terms){
//			System.out.println(t.getName());
//			System.out.println(t.getNatureStr());
			if(t.getNatureStr().trim().equals("en")) continue;
			if(t.getNatureStr().trim().equals("m")) continue;
			if(t.getNatureStr().trim().equals("w")) continue;
			if(t.getNatureStr().trim().equals("ul")) continue;
			if(t.getNatureStr().trim().equals("uj")) continue;
			if(t.getNatureStr().trim().equals("p")) continue;
//			if(t.getNatureStr().trim().equals("r")) continue;
			if(t.getNatureStr().trim().equals("f")) continue;
			if(t.getNatureStr().trim().equals("b")) continue;
			if(t.getNatureStr().trim().equals("y")) continue;
			if(t.getNatureStr().trim().equals("d")) continue;
			if(words.contains(t.getName().trim())) continue;
			words.add(t.getName().trim());
			wterms.add(t);
		}
		
		return wterms;
	}
	
	public double getJacardIndex(List<String> s1,List<String> s2){
		List<String> combine = new ArrayList<String>() ;
		combine.addAll(s1);
		combine.addAll(s2);
		List<String> connex = new ArrayList<String>();
		connex.addAll(s2);
		connex.retainAll(s1);
		
//		System.out.println(connex);
//		System.out.println(combine);
		return connex.size()*1.0/combine.size()*1.0;
//		return connex.size()*1.0/((s1.size()>s2.size())?s2.size():s1.size());
	}
	
	public static void main(String []args){
		String line1 = "急找孩子，求转，求帮忙\u0026nbsp;\u0026nbsp;\u0026nbsp;\u0026nbsp;寻人启事 13940292999。有线索酬金10万 帮忙扩散，今天上午一个三岁多小女孩在锦绣花园小区附近被人拐走了，小女孩能说出她爸爸的手机号码 从监控上看是被一个四十多...\u0026nbsp;\u0026nbsp;@百思不得姐应用\u0026nbsp;\u0026nbsp;O网页链接 ????";
		String line2 = "吴梦月，女，3岁：失踪日期：2014年12月09日下午2点左右，失踪地点：房山区花冠天地门口。孩子要方便。老人包里翻纸的功夫。孩子就在身后。回头孩子瞬间就丢了。父母和家人已经崩溃。如有知情者帮助找到孩子。愿意把价值50万房子卖掉给予酬金。愿所有好心人一生平安。知情联系电话.15885896808。";
		String line3 = "帮人转的。 急找孩子，求转，求帮忙 ，寻人启事 有线索酬金10万 帮忙扩散，今天上午一个三岁多小女孩在锦绣花园小区附近被人拐走了，小女孩能说出她爸爸的手机号码 从监控上看是被一个四十多岁男人抱走了， 看到信息的兄弟姐妹留意一下 联系人宁继春13940292999。请你伸出手指按3秒 看到就转转";
		String line4 = "寻人启事 13940292999。有线索酬金10万 帮忙扩散，今天上午一个三岁多小女孩在锦绣花园小区附近被人拐走了，小女孩能说出她爸爸的手机号码 从监控上看是被一个四十多岁男人抱走了现大人都急疯了 有知情者请告之 万分感谢 看到信息的兄弟姐妹留意一下 联系人宁继春13940292999 看到就转转 ????";
		String line5 = "我紧急通知：家里有孩子的 大人都看好了，成都市来了1000多外地人 专偷小孩抢小孩的已经丢20多个，已解剖7个拿走器官！今天学校也给家长开会呢，说凡是街上转悠，跟到家门口楼下就走了，面包车，收粮食的车，收旧家电的，人带黑口罩，穿黑裤子，有问路的千万别停下，不要理会，收到的都传下，照顾好孩子";
		String line6 = "河南一家肯德基出售长了蛆的炸鸡腿，引起顾客不满，随后引发矛盾，最后河南市民，愤怒的将该店砸毁。警示所有的中国人，远离洋垃圾，不是所有外国品牌都是好的。";
		String line7 = "河南一家肯德基出售长了蛆的炸鸡腿，引起顾客不满，随后引发矛盾，最后河南市民，愤怒的将该店砸毁。O河南一肯德基出售有“蛆”炸鸡，被民众愤怒砸店";
		String line8 = "求转，求扩散，实验小学 寻人启事 13940292999。有线索酬金10万 帮忙扩散，今天上午一个三岁多小女孩在锦绣花园小区附近被人拐走了，小女孩能说出她爸爸的手机号码 从监控上看是被一个四十多岁男人抱走了现大人都急疯了 有知情者请告之 万分感谢 看到信息的兄弟姐妹留意一下 联系人宁继春13940292999 ????";
		String line9 = "感谢致电腾阳房地产开发有限公司，地址是位于北京南路刘家沟尚景春天，诚信为本，客户至上，欢迎您来电咨询xxxx-xxxxxxx[xxxxxxx]";
		//		String line1 = "急找孩子，求转，寻人启事 15850665544有线索酬金10万 帮忙扩散，";
//		String line2 = "寻人启事 13940292999。有线索酬金10万 帮忙扩散";
		TextProcess tp = new TextProcess(); 
		List<String> words1 = tp.sentenceSplit(line1);
		List<Term> words2 = tp.sentenceSplitByTerm(line9);
		System.out.println(words1);
		System.out.println(words2);
//		System.out.println(tp.getJacardIndex(words1, words2));
		
	}
}
