package org.geek.geeksearch.queryer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.geek.geeksearch.configure.Configuration;
import org.geek.geeksearch.indexer.Tokenizer;
import org.geek.geeksearch.model.InvertedIndex;
import org.geek.geeksearch.model.PageInfo;
import org.geek.geeksearch.model.TermStat;
import org.geek.geeksearch.queryer.Response.VarInteger;
import org.geek.geeksearch.util.DBOperator;


public class QueryProcessor {
//	static {
//		//配置文件初始化，临时在此初始化，便于调试，工程完工后会在BootLoader里初始化
//		Configuration config = new Configuration("configure.properties");//初始化
//		new DBOperator(config);
//		//初始化分词，加载词典
//		new Tokenizer(new Configuration());
//	}
	private Map<String, Long> termIDsMap = new HashMap<>(); //词项-词项ID 映射表
	private Map<Long,InvertedIndex> invIdxMap = new HashMap<>(); //倒排索引表
	private int topK = 50; //设置胜者表的topK，默认50
	private int idxNumInMem = 100; //内存中倒排索引的初始数目
	private int totalDocs = 61725; //正向索引数
	
	private final Configuration config = new Configuration();
	private final DBOperator dbOperator = new DBOperator();
	
	// 不支持多线程,用于匹配标题
	private Map<Long, String> queryTerms = new HashMap<>(); //查询词  
	
	public ArrayList<String> get_query(){
		ArrayList<String> querys = new ArrayList<String>();
		Iterator iter = queryTerms.entrySet().iterator(); 
		while (iter.hasNext()) { 
		    Map.Entry entry = (Map.Entry) iter.next(); 
		    Object key = entry.getKey(); 
		    Object val = entry.getValue(); 
		    querys.add((String) val);
		}
		return querys;
	}
	
	public QueryProcessor() {
		setTopK(config);
		setTotalDocs(config);
		setIdxNum(config);
		long start = System.currentTimeMillis();
		
		System.out.println("----开始加载倒排索引...");
		loadInvertedIndex(-1);
		long end = System.currentTimeMillis();
		System.out.println("==== 加载倒排索引结束，用时："+(end-start)+"毫秒 ====");
		
		System.out.println("----开始加载词项ID映射表...");
		loadTermsIndex();		
		start = System.currentTimeMillis();
		System.out.println("==== 加载词项DI映射表结束，用时："+(start-end)+"毫秒 ====");
	}
	
	/**
	 * 检索入口
	 * 返回值为已排序并聚类后的相关page
	 * 第二层链表表示同一类page
	 * 
	 */
	public List<List<PageInfo>> doQuery(String query, VarInteger resultCnt) {
		//初始化查询
		queryTerms.clear();
		
		long start = System.currentTimeMillis();
		// 分词 		
		List<Long> queryIDs = parseQuery(query);
		if (queryIDs == null || queryIDs.isEmpty()) {
			System.out.println("nothing to search!");
			return null;
		}
		long end = System.currentTimeMillis();
		
		System.out.println("===== 分词完成，用时:"+(end-start)+"毫秒 =====");
		
		// 获取已排序的相关网页及信息
		List<PageInfo> resultPages = getResultPages(queryIDs);
		if (resultPages == null || resultPages.isEmpty()) {
			System.out.println("nothing retrived for query: "+ query);
			return null;
		}
		//获得相关网页数目
		resultCnt.setVar(resultPages.size());
		
		// 聚类
		return PageCluster.doCluster(resultPages);
	}
	
	/* 获取相关网页，并从数据库PagesIndex获取网页信息 */
	private List<PageInfo> getResultPages(List<Long> queryIDs) {
		List<PageInfo> resultPages = new ArrayList<>();
		Map<Long, PageInfo> tmpPages = new HashMap<>();
		
		long start = System.currentTimeMillis();
		List<Map.Entry<Long, TermStat>> relevantDocs = getRelevantDocs(queryIDs);
		if (relevantDocs == null || relevantDocs.isEmpty()) {
			System.out.println("no pages retrived");
			return null;
		}
		long end = System.currentTimeMillis();
		System.out.println("===== 相关文档已找到并合并，用时:"+(end-start)+"毫秒 =====");
		
		//计算相似度权重nnn.ntn, 顺便从PagesIndex获取PageInfo
		PageInfo page;
		for (Map.Entry<Long, TermStat> doc : relevantDocs) {
			//获取pageinfo
			long t1 = System.currentTimeMillis();
			page = new PageInfo(doc.getKey());
			if (!page.loadInfo(dbOperator)) {
				System.out.println("no page info of "+doc.getKey());
				continue;
			}
			long t2 = System.currentTimeMillis();
			System.out.println("-- 从数据库获取网页信息，用时:"+(t2-t1)+"毫秒  --");
			//计算关键词高亮位置
//			page.highlight(queryTerms);//弃用
			tmpPages.put(doc.getKey(), page);
			
			//计算权重
			for (long term : queryIDs) {
				TermStat stat = invIdxMap.get(term).getStatsMap().get(doc.getKey());
				if (stat == null) {
					System.out.println("can not find doc stat in term: "+term);
				}
				//计算“标题+描述”中搜索词出现次数，1次weight+10,以及时间权重
				long titWeight = page.countInTitleDesc(queryTerms.get(term));
				
				//累计相似度结果:
				//weight = Σ{检索词项权重(1)*该文档权重(tf-idf)+标题中搜索词出现次数*10}
				doc.getValue().addWeight(stat.getTfIdf()+titWeight);
			}
			//计算并加入日期权重
			doc.getValue().addWeight(page.countPubTimeWeight());
			
			System.out.println("doc:"+doc.getKey()+
					"; weight["+doc.getKey()+"]="+doc.getValue().getWeight());//
			t1 = System.currentTimeMillis();
			System.out.println("--权重计算完成，用时:"+(t1-t2)+"毫秒 --");
		}
		start = System.currentTimeMillis();
		System.out.println("===== 相似度计算完成，用时:"+(start-end)+"毫秒 =====");
		
		//relevantDocs根据相似度权重降序排列
		Collections.sort(relevantDocs, new Comparator<Map.Entry<Long, TermStat>>() {
			public int compare(Map.Entry<Long, TermStat> o1, Map.Entry<Long, TermStat> o2) {
				if (o2.getValue().getWeight() > o1.getValue().getWeight()) {
					return 1;
				} else if (o2.getValue().getWeight() >= o1.getValue().getWeight()){
					return 0;
				} else {
					return -1;
				}
			}
		});
		
		//返回排好序的结果信息
		for (Map.Entry<Long, TermStat> doc : relevantDocs) {
			resultPages.add(tmpPages.get(doc.getKey()));
			System.out.println("\nretrived page: "+ doc.getKey());
		}
		System.out.println("===== 根据相似度排序完成，用时:"+(System.currentTimeMillis()-start)+"毫秒 =====");
		
		return resultPages;
	}
	
	/* 获取各个词项的TopK篇文档，求并集,此处尚未考虑只包含部分检索词的文档,返回的文档都包含所有检索词*/
	private List<Map.Entry<Long, TermStat>> getRelevantDocs(List<Long> queryIDs) {
		Map<Long, TermStat> mergedResult = new TreeMap<>();
		Map<Long, TermStat> tmpDocs = new TreeMap<>();
		
		List<Long> sortedQIDs = sortQueryIDs(queryIDs);
		
		//对每个词项id获取topK文档
		InvertedIndex invIdx;
		if (!invIdxMap.containsKey(sortedQIDs.get(0))) {
			loadInvertedIndex(sortedQIDs.get(0));//数据库也没有？和词项映射表对应，不大可能没有
		}
		invIdx = invIdxMap.get(sortedQIDs.get(0));
		mergedResult = invIdx.getTopKDocs();
		invIdx=null;//release mem
		
		for (int k = 1; k < sortedQIDs.size(); k++) {
			if (!invIdxMap.containsKey(sortedQIDs.get(k))) {
				loadInvertedIndex(sortedQIDs.get(0));
			}
			invIdx = invIdxMap.get(sortedQIDs.get(k));
			tmpDocs = invIdx.getTopKDocs();
			invIdx = null;//release mem
			
			//将该词项id的topK文档与上一次merge结果进行merge
			Iterator<Map.Entry<Long, TermStat>> iter = mergedResult.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Long, TermStat> entry = iter.next();
				long docId = entry.getKey();
				if (!tmpDocs.containsKey(docId)) {
					iter.remove();
					System.out.println("remove "+docId);
					continue;
				}
				System.out.println("remain "+docId);
			}
		}
		return new ArrayList<Map.Entry<Long,TermStat>>(mergedResult.entrySet());
	}
	
	/*按照检索词项的相关文档规模排序，便于merge*/
	private List<Long> sortQueryIDs(List<Long> queryIDs) {
		List<Long> sortedQIDs = new ArrayList<>();
		Map<Long, Integer> indexSizes = new HashMap<Long, Integer>();
		for (long id : queryIDs) {
			
			if (!invIdxMap.containsKey(id)) {
				loadInvertedIndex(id);
			}
			indexSizes.put(id, invIdxMap.get(id).getTopKDocs().size());
//			System.out.println("key:"+id+"  value:"+indexSizes.get(id));
		}
		List<Map.Entry<Long, Integer>> indexSizesList = new ArrayList<>(indexSizes.entrySet());
		Collections.sort(indexSizesList, new Comparator<Map.Entry<Long, Integer>>() {
			public int compare(Entry<Long, Integer> o1, Entry<Long, Integer> o2) {
				return o2.getValue() <= o1.getValue() ? 1 : -1;
			}
		});
//		System.out.println("after sort:");
		for (Map.Entry<Long, Integer> entry : indexSizesList) {
			if (entry == null) {
				continue;
			}
			sortedQIDs.add(entry.getKey());
//			System.out.print(entry.getKey()+" ");
		}
		return sortedQIDs;
	}
	
	/* query解析 */
	private List<Long> parseQuery(String query) {
		// 分词
		List<String> qTerms = Tokenizer.doTokenise(query);
//		List<String> qTerms = new ArrayList<>();// just for test
//		qTerms.add("中");
//		qTerms.add("林书豪");
//		qTerms.add("詹姆斯");
		if (qTerms == null || qTerms.isEmpty()) {
			return null;
		}
		// 映射成ID
		List<Long> queryIDs = new ArrayList<>();
		System.out.print("-----分词结果：");
		for (String term : qTerms) {
			if (term == null || term.isEmpty()) {
				continue;
			}
			System.out.print(term+" ");
			
			long id;
			if (!termIDsMap.containsKey(term)) {
				id = fetchTermID(term);				
			} else {
				id = termIDsMap.get(term);
			}
			
			if (id < 0) {
				//跳过索引库中没有的词项
				continue;
			}
			queryTerms.put(id, term);
			queryIDs.add(id);
		}
		System.out.println();
		return queryIDs;
	}
	
	/* 从TermsIndex获取termID */
	private long fetchTermID(String term) {
		String sql = " SELECT * FROM termsindex WHERE term='"+term+"' ";
		ResultSet rSet = dbOperator.executeQuery(sql);
		if (rSet == null) {
			System.out.println("can not find term: "+term);
			return -1;
		}
		long termID = -1;
		try {
			while (rSet.next()) {
				termID = rSet.getLong("TermID");
				break;				
			}			
		} catch (SQLException e) {
			System.out.println("can not find term: "+term);
			return -1;
		}
		return termID;
	}

	/* 加载 InvertedIndex  */
	private void loadInvertedIndex(long id) {
		String sql;
		if (id > -1) {
			/*内存中找不到，从数据库查找倒排索引*/
			sql = " SELECT * FROM invertedindex where TermID = "+id;
			//System.out.println("----从数据库读取索引："+id);
		} else {
			/*加载IdxNumInMem个索引到内存*/
			sql = " SELECT * FROM invertedindex ";			
		}
		
		ResultSet rSet = dbOperator.executeQuery(sql);
		if (rSet == null) {
			System.err.println("load nothing from table InvertedIndex!");
			return;
		}
		InvertedIndex invIdx;
		long termID = -1;
		String docIDs = "";
		try {
			while (rSet.next()) {
				termID = rSet.getLong("TermID");
				docIDs = rSet.getString("DocumentIDs");
				if (docIDs == null || docIDs.isEmpty() || termID < 0) {
					continue;
				}
				invIdx = new InvertedIndex(termID);
				invIdx.parseIndex(docIDs, topK, totalDocs);
				invIdxMap.put(termID, invIdx);
				invIdx=null;//release mem
				if (termID % 100 == 0) {
					//System.out.println("----已载入倒排索引："+termID);
				}
				if (termID > idxNumInMem) {//low mem
					break;
				}
			}
			rSet=null;//release mem
		} catch (SQLException e) {
			System.err.println("error occurs while loading termID: "+termID);
			e.printStackTrace();
		}
//		InvertedIndex.addAll2DB(invIdxMap, dbOperator, 3); //just for test
	}

	/* 加载 TermsIndex 表 */
	private void loadTermsIndex() {
		String sql = " SELECT * FROM termsindex ";
		ResultSet rSet = dbOperator.executeQuery(sql);
		if (rSet == null) {
			System.err.println("load nothing from table TermsIndex!");
			return;
		}
		String term = "";
		long id = -1;
		try {
			while (rSet.next()) {
				term = rSet.getString("Term");
				id = rSet.getLong("TermID");
				if (term == null || term.isEmpty() || id < 0) {
					continue;
				}
				termIDsMap.put(term, id);
//				System.out.println(id+" = "+term);//
				if (id > idxNumInMem) {//low mem
					break;
				}
			}
			rSet = null;//release mem
		} catch (SQLException e) {
			System.err.println("error occurs while loading term: "+term);
			e.printStackTrace();
		}
	}
	
	/* 设置胜者表的topK */
	private void setTopK(Configuration config) {
		int tmp;
		try {
			tmp = Integer.parseInt(config.getValue("ChampionTopK"));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		topK = tmp;
	}
	
	/* 设置内存中的初始索引数目 */
	private void setIdxNum(Configuration config) {
		int tmp;
		try {
			tmp = Integer.parseInt(config.getValue("IdxNumInMem"));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		idxNumInMem = tmp;
	}
	/* 设置内存中的初始索引数目 */
	private void setTotalDocs(Configuration config) {
		int tmp;
		try {
			tmp = Integer.parseInt(config.getValue("TotalDocs"));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		totalDocs = tmp;
	}

	/* just for test */
	public static void main(String[] args) {
		QueryProcessor queryProc = new QueryProcessor();
		
		long start = System.currentTimeMillis();
		VarInteger cnt = new VarInteger(); 
		List<List<PageInfo>> result = queryProc.doQuery("黄征", cnt);//中 詹姆斯
		System.err.println("===Time cost for doing query: "
				+(System.currentTimeMillis()-start)/1000+" ===");
		
		if (result == null) {
			System.out.println("sorry, 找不到相关页面");
			return;
		}
		for (List<PageInfo> set : result) {
			System.out.println("\n以下新闻为一类：");
			for (PageInfo page : set) {
				System.out.println("docID："+page.getDocID()+"\n标题："+page.getTitle());
			}
		}
	}
}
