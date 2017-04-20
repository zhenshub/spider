package com.spider.pageProcessor;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import  java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.spider.dao.GoogleResumeDao;
import com.spider.model.GoogleResult;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

public class GoogleResumePageProcessor implements PageProcessor {
//	https://www.google.com/webhp?sourceid=chrome&ie=UTF-8#q=filetype+doc+java+resume&newwindow=1&tbas=0&start=10
//	https://www.google.com/webhp?sourceid=chrome&ie=UTF-8#q=filetype+doc+java+resume&newwindow=1&tbas=0&tbs=lr:lang_1zh-CN|lang_1zh-TW&lr=lang_zh-CN|lang_zh-TW
	public static String url = "https://www.google.com.hk/search?q=filetype:+doc+java+resume&lr=lang_zh-CN&safe=strict&gbv=1&tbs=lr:lang_1zh-CN&prmd=ivns&ei=YdP2WICcL8eY8wXYl7awCA&sa=N";
	private static int id = 0; 

//	private Site site = Site.me()
//			.setRetryTimes(3)
//			.setSleepTime(1000)
//			.setTimeOut(10000)
//			.addHeader(":authority", "www.google.com.hk")
//			.addHeader(":method", "GET")
//			.addHeader(":path", "/search?q=file+type:+doc+java+resume&safe=strict&gbv=1&sei=Y8_2WJ_fGciz0AT36I_4DQ")
//			.addHeader(":scheme", "https")
//			.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
//			.addHeader("accept-encoding", "gzip, deflate, sdch")
//			.addHeader("accept-language", "zh-CN,zh;q=0.8")
//			.addHeader("avail-dictionary", "LiNh3Gcr")
//			.addHeader("referer", "https://www.google.com.hk/")
//			.addHeader("upgrade-insecure-requests", "1")
//			.addHeader("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36 QIHU 360SE");
	
	private Site site = Site.me()
			.setRetryTimes(3)
			.setSleepTime(1000)
			.setTimeOut(10000);
	
	
	private static Logger logger = Logger.getLogger(GoogleResumePageProcessor.class);
	
	private static SqlSessionFactory sqlSessionFactory;
	private static Reader reader;
	static{
        try{
            reader    = Resources.getResourceAsReader("mybatis-config.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
	
	public void process(Page page) {
		
		String resultStat = page.getHtml().xpath("//*[@id='resultStats']/text()").toString();
		logger.info(resultStat);
		
		try {
			FileUtils.writeStringToFile(new File("ret.html"), page.toString(), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("write html error");
		}
		
		List<Selectable> gList = page.getHtml().xpath("//div[@class='g']").nodes();
		
		if(gList.isEmpty()) {
			//check is last page
			Selectable node = page.getHtml().xpath("//div/ul[@class='_Gnc']");
			if(node==null) {
				loadNextPage(page);
			} else {
				logger.info("抓取已经完成");
			}
		}
		
		
		ArrayList<GoogleResult> results = new ArrayList<GoogleResult>();
		
		for (Selectable g : gList) {
			GoogleResult googleResult = new GoogleResult();
			
			System.out.println(g.xpath("//h3[@class='r']/a").toString());
			
			String original = g.xpath("//h3[@class='r']/a").toString();
			int start = original.indexOf("/url?q=")+7;
			int end = original.indexOf("&")-1;
			String originalUrl=original.substring(start, end);			
			try {
				
				url = java.net.URLDecoder.decode(originalUrl, "utf-8");
			} catch (UnsupportedEncodingException e) {
				logger.info("url decode fail");
				url = originalUrl;
			} 
			
			googleResult.setId(++id);
			googleResult.setTitle(g.xpath("//h3[@class='r']/a/text()").toString());
			googleResult.setUrl(url);
			googleResult.setDes(g.xpath("//div[@class='s']/span[@class='st']/text()").toString());
			logger.info(googleResult);
			results.add(googleResult);
			
			SqlSession session = sqlSessionFactory.openSession();
			 try {
				 GoogleResumeDao dao = session.getMapper(GoogleResumeDao.class);
			     dao.addResult(googleResult);
			     session.commit();
			     logger.info("添加"+googleResult.getTitle()+"成功");
			 } finally {
				 session.close();    
			 }
		}
		
		loadNextPage(page);
	}
	
	private void loadNextPage(Page page) {
		page.addTargetRequest(url+"&start="+id);
	}
	
	public Site getSite() {
//		List<String []> httpPorxyList = new ArrayList<String []>();
//		httpPorxyList.add(new String[] {"" ,"", "125.88.74.122", "83"});
//		httpPorxyList.add(new String[] {"" ,"","219.226.180.72", "8998"});
//		httpPorxyList.add(new String[] {"" ,"","123.84.13.240", "8118"});
//		httpPorxyList.add(new String[] {"" ,"","125.88.74.122", "83"});
//		httpPorxyList.add(new String[] {"" ,"","121.232.144.119", "9000"});
//		httpPorxyList.add(new String[] {"" ,"","118.249.176.83", "8000"});
//		httpPorxyList.add(new String[] {"" ,"","117.90.2.55", "9000"});
//		site.setHttpProxyPool(httpPorxyList, false);
		
		return site;
	}
	
	public static void main( String[] args )
    {	
		logger.info("开始抓取");
		
		id = 92;
        Spider.create(new GoogleResumePageProcessor())
        .addUrl(GoogleResumePageProcessor.url+"&start="+id)
        .thread(1)
        .run();
    }
}
