package org.geek.geeksearch.spider;

import java.net.URL;

import org.geek.geeksearch.util.HtmlParser;

import cn.edu.hfut.dmic.webcollector.crawler.BreadthCrawler;
import cn.edu.hfut.dmic.webcollector.model.Page;

//腾讯体育
public class SpiderQQ extends BreadthCrawler{
	
	@Override
	public void visit(Page page){
        page.getHtml();//获取网页信息
        //得到path的具体信息
        try {
            URL _URL = new URL(page.getUrl());
            String query = "";
            if (_URL.getQuery() != null) {
                query = "_" + _URL.getQuery();
            }
            String path ="";// _URL.getPath();
            if (path.length() == 0) {
                path = "index.htm";
            } else {
                if (path.charAt(path.length() - 1) == '/') {
                    path = path + "index.htm";
                } else {

                    for (int i = path.length() - 1; i >= 0; i--) {
                        if (path.charAt(i) == '/') {
                            if (!path.substring(i + 1).contains(".")) {
                                path = path + ".htm";
                            }
                        }
                    }
                }
            }
            path += query;
            
//           System.out.println("-------page.getUrl()-------"+page.getUrl());
           
           String replace_file_name = page.getUrl().replace('/', '#').replace(':', '$');
           int NamLen=replace_file_name.length();
           String LastStr=replace_file_name.substring(NamLen-4, NamLen);
           if(LastStr.equals(".htm"))
           {
//        	   System.out.println("----保存文件**********"+replace_file_name+"***********到本地！！！----");
        	   String PathQQ=StartSpider.config.getValue("PathStoreQQ");
        	   String file=PathQQ+replace_file_name;
        	   String htmlInfor=HtmlParser.deleNoise(page.getHtml().toString());
               new CreateHtml().OutputFile(file,htmlInfor);
           }
           
        } catch (Exception e) {
            e.printStackTrace();
        }  
	}

	public boolean startSpiderQQ(){
		 SpiderQQ crawler = new SpiderQQ();  
	        crawler.addSeed("http://sports.qq.com/");
	        
	        crawler.addRegex("+http://sports.qq.com/.*");
	        crawler.addRegex("+http://golf\\.qq\\.com/.*");
	        crawler.addRegex("+http://sports.qq.com/cba/.*");
	        crawler.addRegex("+http://sports.qq.com/nba/.*");
	        crawler.addRegex("+http://sports.qq.com/csocce/csl/.*");
	        crawler.addRegex("+http://sports.qq.com/tennis/.*");
	        crawler.addRegex("+http://sports.qq.com/others/.*");
	        crawler.addRegex("+http://sports.qq.com/ucl/.*");
	        
	        
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
	        /*爬取深度为5*/  
	        try {
				crawler.start(6);
			} catch (Exception e) {
				e.printStackTrace();
			}     
			return true;
	}
	
}
