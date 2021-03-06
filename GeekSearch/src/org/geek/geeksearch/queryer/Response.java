package org.geek.geeksearch.queryer;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;

import org.geek.geeksearch.configure.Configuration;
import org.geek.geeksearch.indexer.Tokenizer;
import org.geek.geeksearch.model.PageInfo;
import org.geek.geeksearch.recommender.CheckSpell;
import org.geek.geeksearch.util.DBOperator;


public class Response {
	static {
		long start = System.currentTimeMillis();
		//配置文件初始化，临时在此初始化，便于调试，工程完工后会在BootLoader里初始化
		Configuration config = new Configuration("configure.properties");//初始化
		new DBOperator(config);
		//初始化分词，加载词典
		new Tokenizer(new Configuration());
		System.out.println("===== 初始化全部完成，总共用时:"+(System.currentTimeMillis()-start)+"毫秒 =====");
	}
	private VarInteger resultCnt = new VarInteger();//相关新闻数目
	private static QueryProcessor processor = new QueryProcessor();//所有response对象共有

	public Response(){
		//do nothing
	}

	/* 获取推荐词 */
	public String get_recommend_query(String query){
		long start = System.currentTimeMillis();
		ArrayList<String> sug = CheckSpell.suggestSimilar(query,3);
		System.out.println("===== 搜索词推荐完成，用时:"+(System.currentTimeMillis()-start)+"毫秒 =====");
		return JSONArray.fromObject(sug).toString();
//		List<String> sug = new ArrayList<String>();
//		sug.add("科比");
//		sug.add("科技");
//		sug.add("科学");
//		return JSONArray.fromObject(sug).toString();
	}
	
	public String getQuerys(){
		JSONArray querys = JSONArray.fromObject(processor.get_query());
		System.out.println("querys:\n"+querys.toString());
		return querys.toString();
	}
	
	/*服务器端入口*/
	public String getResponse(String query)
	{
		long start = System.currentTimeMillis();
		List<List<PageInfo>> resultList = processor.doQuery(query, resultCnt);
		System.out.println("========== 检索全部完成，总共用时:"+(System.currentTimeMillis()-start)+"毫秒 ==========");
		
		if (resultList == null || resultList.isEmpty()) {
			return null;
		}
		
		//有结果才将query存入热词库
		CheckSpell.store_query(query);
		
		/*construct json and output it*/
		JSONArray json_result = JSONArray.fromObject(resultList);
		
//		JSONObject json = new JSONObject();
		
/*		JSONObject json = new JSONObject();
		JSONArray jsonMembers = new JSONArray();
		JSONObject member1 = new JSONObject();
		member1.put("loginname", "zhangfan");
		member1.put("password", "userpass");
		member1.put("email", "10371443@qq.com");
		member1.put("sign_date", "2007-06-12");
		jsonMembers.add(member1);

		JSONObject member2 = new JSONObject();
		member2.put("loginname", "zf");
		member2.put("password", "userpass");
		member2.put("email", "8223939@qq.com");
		member2.put("sign_date", "2008-07-16");
		jsonMembers.add(member2);
		json.put("users", jsonMembers);*/
		System.out.println("results:\n"+json_result.toString());
		return json_result.toString();
	}
	
	public int getResultCnt() {
		return resultCnt.getVar();
	}
	
	static class VarInteger {
		private int varInteger = 0;
		public void setVar(int var) {
			this.varInteger = var;
		}
		public int getVar() {
			return varInteger;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Response response = new Response();
		
		System.out.println(response.getResponse("林丹"));
		System.out.println(response.get_recommend_query("大运"));//单字推荐报错
//		System.out.println("相关数："+resultCnt);
	}

}
