package com.geek.geeksearch.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Html������
 *
 */
public class HtmlParser {
	private static Parser parser = new Parser();
	
	public static String getPlainText(String htmlStr, String type) {
		StringBuffer textBuf = new StringBuffer();
		
		htmlStr = deleNoise(htmlStr);
		NodeFilter filter = createFilter(type);
		try {
			parser = Parser.createParser(htmlStr, "GB2312");
			NodeList list = parser.parse(filter);
			if (list == null || list.size() <= 0) {
				return null;
			}
			for (Node node : list.toNodeArray()) {// ֻ��ִ��һ��
				textBuf = textBuf.append(node.toPlainTextString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deleSpace(textBuf.toString());
	}
	
	private static String deleSpace(String text) {
		text = text.replaceAll("\\s{2,}", " ");
		text = text.replaceAll("\n|\r|\t", "");
		System.out.printf("content: %s\n\n", text.trim());
		return text.trim();
	}
	
	private static NodeFilter createFilter(String type) {
		NodeFilter filter = new HasAttributeFilter();
		if (type.equals("163")) { //����
			filter = new AndFilter(new TagNameFilter("div"), 
					new HasAttributeFilter("id", "endText"));
		} else if (type.equals("sina")) { //����
			filter = new AndFilter(new TagNameFilter("div"), 
					new HasAttributeFilter("id", "artibody"));				
		} else if (type.equals("sohu")) { //�Ѻ�
			return new AndFilter(new TagNameFilter("div"), 
					new HasAttributeFilter("itemprop", "articleBody"));
		} else if (type.equals("qq")) { //��Ѷ
			filter = new AndFilter(new TagNameFilter("div"), 
					new HasAttributeFilter("id", "Cnt-Main-Article-QQ"));
		} else if (type.equals("msn")) { //MSN
			filter = new AndFilter(new TagNameFilter("div"), 
					new HasAttributeFilter("class", "endText"));
		} else {
			// other 
		}
		return new AndFilter(new NodeClassFilter(ParagraphTag.class), 
				new HasParentFilter(filter));
	}
	
	private static String deleNoise(String htmlStr) {
		Pattern pattern;
		Matcher matcher;		
		//����script����ʽ
		String script = "<script[^>]*?>[\\s\\S]*?</script>";    
		//����style����ʽ    
		String style = "<style[^>]*?>[\\s\\S]*?</style>";
		
		pattern = Pattern.compile(script,Pattern.CASE_INSENSITIVE);    
        matcher = pattern.matcher(htmlStr);    
        htmlStr = matcher.replaceAll(""); //����script��ǩ  
        pattern = Pattern.compile(style,Pattern.CASE_INSENSITIVE);    
        matcher = pattern.matcher(htmlStr);    
        return matcher.replaceAll(""); //����style��ǩ 
	}
	
	
	/**
	 * ��ȡkeywords��description
	 * ���keywords֮���ԡ�,���ŷָ�
	 */
	public static String[] getKeyWordAndDesc(String htmlStr) {
		Matcher matcher = null;
		MetaTag meta = null;
		String keyWords = "";
		String descrip = "";
		try {
			parser = Parser.createParser(htmlStr, "GB2312");
			NodeFilter filter = new NodeClassFilter(MetaTag.class);
			NodeList list = parser.extractAllNodesThatMatch(filter);
			Pattern pKeywords = Pattern.compile("\\b(keywords)\\b",Pattern.CASE_INSENSITIVE);
			Pattern pDesc = Pattern.compile("\\b(description)\\b",Pattern.CASE_INSENSITIVE);
			if (list == null || list.size() <= 0) {
				System.out.println("no keywords & description in this html!");
				return null;
			}
			for (Node node : list.toNodeArray()) {
				meta = (MetaTag)node;
                String name = meta.getMetaTagName();
                if(name == null || name.isEmpty()){
                    continue;
                }
                matcher = pKeywords.matcher(name);                
                // ֻȡ�״γ��ֵ�keywords �� description
                if (!keyWords.isEmpty() && !descrip.isEmpty()) {
					break;
				}
                if(keyWords.isEmpty() && matcher.find()) {
                	keyWords = meta.getMetaContent();
                } else if (descrip.isEmpty()) {
					matcher = pDesc.matcher(name);
					descrip = matcher.find() ? meta.getMetaContent() : "";
				}
            }
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return new String[]{keyWords, descrip};
	}
	
	public static String getTitle(String htmlStr) {
		try {
			parser = Parser.createParser(htmlStr, "GB2312");
			NodeFilter filter = new NodeClassFilter(TitleTag.class);
			NodeList list = parser.extractAllNodesThatMatch(filter);
			if (list == null || list.size() <= 0) {
				System.out.println("no title in this html!");
				return null;
			}
			for (Node node : list.toNodeArray()) {
				TitleTag title = (TitleTag)node;
				return title.getTitle(); // ֻ��һ��title
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}
		System.err.println("title is null!");
		return null;
	}
	
	public static String readHtmlFile(String path) {
		File file = new File(path);
		if (!file.exists() || !file.isFile()) {
			System.err.printf("unexisting path of html: %s\n", path);
			return null;
		}
		BufferedReader bufReader = null;
		StringBuffer strBuf = new StringBuffer();
		try {
			FileReader reader  = new FileReader(file);
			bufReader = new BufferedReader(reader);
			String line = null;
			while((line = bufReader.readLine()) != null)
			{
				strBuf.append(line + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				bufReader.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return strBuf.toString();
	}
}
