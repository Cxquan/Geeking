package com.geek.geeksearch.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.geek.geeksearch.util.DBOperator;
import com.sun.org.apache.regexp.internal.recompile;

/**
 * �ĵ�������ϣ��
 * �洢�������ĵ�������¼
 *
 */
public class DocIndex {
//	// ���ڴ����ݿ��ȡʱ���ڴ�ά������������������д���ݿ�ʱ�ۻ�һ����Ŀ������д��
//	private HashMap<Long, ArrayList<Long>> docsIndex= new HashMap<>();

	public void addIndex(long docID, List<Long> docTermIDs, DBOperator dbOp) {
		if (docID < 0) {
			System.err.printf("bad docID: %s\n", docID);
			return;
		}
//		docsIndex.put(docID, docTermIDs);
		String docTerms = toString(docTermIDs);
		add2DB(dbOp, docID, docTerms);
	}
	
	public List<Long> toList() {
		List<Long> docTermIDs = new ArrayList<>();
		//
		return docTermIDs;
	}
	
	private String toString(List<Long> docTermIDs) {
		StringBuffer docTerms = new StringBuffer();
		for (Long termID : docTermIDs) {
			docTerms = docTerms.append("#").append(termID.toString());
		}
//		System.out.println(terms.toString());
		return docTerms.toString();	
	}

//	public ArrayList<String> getTermList(long docID) {
//		if (!docsIndex.containsKey(docID)) {
//			System.err.printf("bad docID: %s\n", docID);
//		}
//		return docsIndex.get(docID);
//	}
	
	/* ÿ��һ���ĵ���������дһ�����ݿ� */
	private void add2DB(DBOperator dbOp, long docID, String terms) {
		//
	}
	
//	/* ������Ŀ�ﵽһ����Ŀ��һ��д�����ݿ⣨����ʵ�֣� */
//	public void addAll2DB() {
//		//
//	}
	
}
