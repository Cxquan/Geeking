package com.geek.geeksearch.indexer;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import com.geek.geeksearch.model.PageInfo;
import com.geek.geeksearch.util.DBOperator;
import com.geek.geeksearch.util.HtmlParser;

/**
 * 0. ��ȡȫ��html��ҳ��
 * 1. ��ȡ�ؼ���Ϣ�� ����PageInfo����PageIndex���ݿ�
 * 2. ����html��ǩ��ȡ���ģ��������Ĵ�����
 * 3. �����ĵ�����DocIndex���ʹ���ID-����ӳ���TermsMap��������д�����ݿ�
 * 4. �����ĵ��������ɵ�������InvertedIndex��д�����ݿ�
 *
 */
public class IndexGenerator {
	private AtomicLong docID = new AtomicLong(-1); // �ĵ�ID
	private AtomicLong termID = new AtomicLong(-1); // ����ID
	private DBOperator dbOperator = new DBOperator();
	private String rawPagesDir = null; //configure.properties
	
	public IndexGenerator(String rawPagesDir) {
		this.rawPagesDir = rawPagesDir;
	}
	
	public static void main(String[] args) {
		IndexGenerator generator = new IndexGenerator("RawPages");
		generator.createIndexes();
	}
	
	public void createIndexes() {
		String[] typeArr = getTypes();
		for (String type : typeArr) {
			String[] htmlArr = getHTMLs(type);
			for (String html : htmlArr) {
//				System.out.println(type+"/"+html);
				createIndexes(type, html);
			}
		}
	}
	
	/* ���ɸ������� */
	public void createIndexes(String type, String html) {
		String path = rawPagesDir+"\\"+type+"\\"+html;
		//if (checkHtml()) return;
		String htmlStr = HtmlParser.readHtmlFile(path);

		//������ҳ��Ϣ����
		createPageIndex(htmlStr, type, getURL(html));
		
		//���˱�ǩ��ȡ����
		String plainText = HtmlParser.getPlainText(htmlStr, type);
		
		//�����ĵ�����
		createDocIndex(plainText);
	}
	
	/* �����ĵ����� */
	public void createDocIndex(String text) {
		//�ִʣ��õ�һƪ�ĵ��Ĵ����б�д�����ݿ�
	}
	
	/* ������ҳ��Ϣ���� */
	public void createPageIndex(String htmlStr, String type, String url) {
		String title = HtmlParser.getTitle(htmlStr);
		String[] kwAndDesc = HtmlParser.getKeyWordAndDesc(htmlStr);
		if (url.isEmpty() || type.isEmpty() || kwAndDesc.length != 2) {
			String err = "type="+type+";url="+url+";kwAndDesc="+kwAndDesc.toString();
			System.err.printf("bad page info: %s\n", err);
			return;
		}
		PageInfo pageInfo = new PageInfo(docID.incrementAndGet(), url, type, title, kwAndDesc[0], kwAndDesc[1]);	
		pageInfo.add2DB(dbOperator);
		System.out.print("title: "+title+";  kw: "+kwAndDesc[0]
				+"\ndescrip: "+kwAndDesc[1]+"\n");
	}
	
	public String getURL(String fileName) {
		int idx = fileName.indexOf(".");
		if (idx <= 0) {
			System.err.printf("wrong type of file name!", fileName);
			return "";
		}
		return fileName.substring(0, idx); 
	}
	
	/* ����ҳ��Ŀ¼��ȡ����Ŀ¼ �б�*/
	public String[] getTypes() {
		File rootDir = new File(rawPagesDir);
		if (!rootDir.exists() || !rootDir.isDirectory()) {   
			System.err.printf("unexisting path of rawPages: %s\n", rawPagesDir);   
		    return null;
		}
		return rootDir.list(); 
	}
	
	/* �� ����Ŀ¼��ȡhtml�ļ��б�*/
	public String[] getHTMLs(String type) {
		File typeDir = new File(rawPagesDir+"\\"+type);
		if (!typeDir.exists() || !typeDir.isDirectory()) {   
			System.err.printf("unexisting path of type: %s\n", typeDir.toString());   
		    return null;
		}
		return typeDir.list();
	}

}
