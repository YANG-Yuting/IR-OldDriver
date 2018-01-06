package query;

import java.util.ArrayList;
import java.util.List;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

public class TextProcess {
	
	
	public List<String> sentenceSplit (String line){
		
		line=line.replaceAll("[\\s;#@,.&!?������������'\\\"%\\[\\]\\(\\)\\��\\��\\:\\<\\>\\��\\����������~_����-������-]","");
		line=line.replaceAll("\\��","");
		//ȥ����������ĸ
		line=line.replaceAll("[a-zA-Z]{7,}","");
		//ȥ������������
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
		
		line=line.replaceAll("[\\s;#@,.&!?������������'\\\"%\\[\\]\\(\\)\\��\\��\\:\\<\\>\\��\\����������~_����-������-]","");
		line=line.replaceAll("\\��","");
		//ȥ����������ĸ
		line=line.replaceAll("[a-zA-Z]{7,}","");
		//ȥ������������
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
		String line1 = "���Һ��ӣ���ת�����æ\u0026nbsp;\u0026nbsp;\u0026nbsp;\u0026nbsp;Ѱ������ 13940292999�����������10�� ��æ��ɢ����������һ�������СŮ���ڽ��廨԰С���������˹����ˣ�СŮ����˵�����ְֵ��ֻ����� �Ӽ���Ͽ��Ǳ�һ����ʮ��...\u0026nbsp;\u0026nbsp;@��˼���ý�Ӧ��\u0026nbsp;\u0026nbsp;O��ҳ���� ????";
		String line2 = "�����£�Ů��3�꣺ʧ�����ڣ�2014��12��09������2�����ң�ʧ�ٵص㣺��ɽ����������ſڡ�����Ҫ���㡣���˰��﷭ֽ�Ĺ��򡣺��Ӿ�����󡣻�ͷ����˲��Ͷ��ˡ���ĸ�ͼ����Ѿ�����������֪���߰����ҵ����ӡ�Ը��Ѽ�ֵ50��������������Ը���к�����һ��ƽ����֪����ϵ�绰.15885896808��";
		String line3 = "����ת�ġ� ���Һ��ӣ���ת�����æ ��Ѱ������ ���������10�� ��æ��ɢ����������һ�������СŮ���ڽ��廨԰С���������˹����ˣ�СŮ����˵�����ְֵ��ֻ����� �Ӽ���Ͽ��Ǳ�һ����ʮ�������˱����ˣ� ������Ϣ���ֵܽ�������һ�� ��ϵ�����̴�13940292999�����������ָ��3�� ������תת";
		String line4 = "Ѱ������ 13940292999�����������10�� ��æ��ɢ����������һ�������СŮ���ڽ��廨԰С���������˹����ˣ�СŮ����˵�����ְֵ��ֻ����� �Ӽ���Ͽ��Ǳ�һ����ʮ�������˱������ִ��˶������� ��֪�������֮ ��ָ�л ������Ϣ���ֵܽ�������һ�� ��ϵ�����̴�13940292999 ������תת ????";
		String line5 = "�ҽ���֪ͨ�������к��ӵ� ���˶������ˣ��ɶ�������1000������� ר͵С����С�����Ѿ���20������ѽ���7���������٣�����ѧУҲ���ҳ������أ�˵���ǽ���ת�ƣ��������ſ�¥�¾����ˣ������������ʳ�ĳ����վɼҵ�ģ��˴��ڿ��֣����ڿ��ӣ�����·��ǧ���ͣ�£���Ҫ��ᣬ�յ��Ķ����£��չ˺ú���";
		String line6 = "����һ�ҿϵ»����۳�������ը���ȣ�����˿Ͳ������������ì�ܣ����������񣬷�ŭ�Ľ��õ��һ١���ʾ���е��й��ˣ�Զ���������������������Ʒ�ƶ��Ǻõġ�";
		String line7 = "����һ�ҿϵ»����۳�������ը���ȣ�����˿Ͳ������������ì�ܣ����������񣬷�ŭ�Ľ��õ��һ١�O����һ�ϵ»������С�����ը���������ڷ�ŭ�ҵ�";
		String line8 = "��ת������ɢ��ʵ��Сѧ Ѱ������ 13940292999�����������10�� ��æ��ɢ����������һ�������СŮ���ڽ��廨԰С���������˹����ˣ�СŮ����˵�����ְֵ��ֻ����� �Ӽ���Ͽ��Ǳ�һ����ʮ�������˱������ִ��˶������� ��֪�������֮ ��ָ�л ������Ϣ���ֵܽ�������һ�� ��ϵ�����̴�13940292999 ????";
		String line9 = "��л�µ��������ز��������޹�˾����ַ��λ�ڱ�����·���ҹ��о����죬����Ϊ�����ͻ����ϣ���ӭ��������ѯxxxx-xxxxxxx[xxxxxxx]";
		//		String line1 = "���Һ��ӣ���ת��Ѱ������ 15850665544���������10�� ��æ��ɢ��";
//		String line2 = "Ѱ������ 13940292999�����������10�� ��æ��ɢ";
		TextProcess tp = new TextProcess(); 
		List<String> words1 = tp.sentenceSplit(line1);
		List<Term> words2 = tp.sentenceSplitByTerm(line9);
		System.out.println(words1);
		System.out.println(words2);
//		System.out.println(tp.getJacardIndex(words1, words2));
		
	}
}
