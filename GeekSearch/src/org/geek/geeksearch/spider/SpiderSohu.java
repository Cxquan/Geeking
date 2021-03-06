package org.geek.geeksearch.spider;

import java.net.URL;

import org.geek.geeksearch.util.HtmlParser;

import cn.edu.hfut.dmic.webcollector.crawler.BreadthCrawler;
import cn.edu.hfut.dmic.webcollector.model.Page;

//搜狐体育
public class SpiderSohu extends BreadthCrawler{

//	 private String root = "data";
	
	@Override
	public void visit(Page page){
        
//        FileSystemOutput fsoutput = new FileSystemOutput(root);
        page.getHtml();//获取网页信息
        
        //-------------------------------------------------------------
        //得到path的具体信息
        try {
            URL _URL = new URL(page.getUrl());
            String query = "";
            if (_URL.getQuery() != null) {
                query = "_" + _URL.getQuery();
            }
            String path ="";// _URL.getPath();
            if (path.length() == 0) {
                path = "index.shtml";
            } else {
                if (path.charAt(path.length() - 1) == '/') {
                    path = path + "index.shtml";
                } else {

                    for (int i = path.length() - 1; i >= 0; i--) {
                        if (path.charAt(i) == '/') {
                            if (!path.substring(i + 1).contains(".")) {
                                path = path + ".shtml";
                            }
                        }
                    }
                }
            }
            path += query;
           String replace_file_name = page.getUrl().replace('/', '#').replace(':', '$');
           int NamLen=replace_file_name.length();
           String LastStr=replace_file_name.substring(NamLen-6, NamLen);
           if(LastStr.equals(".shtml"))
           {
//        	   System.out.println("----保存文件**********"+replace_file_name+"***********到本地！！！----");
        	   String PathSohu=StartSpider.config.getValue("PathStoreSohu");
        	   String file=PathSohu+replace_file_name;
        	   String htmlInfor=HtmlParser.deleNoise(page.getHtml().toString());
               new CreateHtml().OutputFile(file,htmlInfor);
           }  
        } catch (Exception e) {
            e.printStackTrace();
        }  
	}
	
	
	public boolean startSpiderSohu(){
		
		SpiderSohu crawler = new SpiderSohu();  
        crawler.addSeed("http://sports.sohu.com/");
        crawler.addRegex("http://sports\\.sohu\\.com/.*");
        crawler.addRegex("+http://cbachina.sports.sohu.com/.*");
        crawler.addRegex("+http://golf.sports.sohu.com/.*");
        crawler.addRegex("+http://ski.sports.sohu.com/.*");
        crawler.addRegex("+http://running.sports.sohu.com/.*");
        crawler.addRegex("-.*#.*");
        crawler.addRegex("-.*png.*");
        crawler.addRegex("-.*jpg.*");
        crawler.addRegex("-.*gif.*");
        crawler.addRegex("-.*js.*");
        crawler.addRegex("-.*css.*");
        /*设置线程数*/
        crawler.setThreads(30);
        
        /*设置爬虫是否为断点爬取*/
        crawler.setResumable(false);
        /*深度为5*/  
        try {
			crawler.start(6);
		} catch (Exception e) {
			e.printStackTrace();
		}    
		return true;
	}
}
