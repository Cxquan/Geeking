//package com.geek.geeksearch.indexer;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicLong;
//
//import org.ansj.domain.Term;
//import org.ansj.splitWord.analysis.IndexAnalysis;
//
//import com.geek.geeksearch.util.DBOperator;
//
///**
// * �ִ���
// *
// */
//public class Tokenizer {
//	
//	private final DBOperator dbOp;
//	
//	public Tokenizer(DBOperator dbOp) {
//		this.dbOp = dbOp;
//	}
//	
//	/* ������ */
//	public List<String> doTokenise(String text, long docID) {
//		// ʹ�õ������ִʹ���ansjʵ�ִַ�
//		List<Term> parsedTerms = IndexAnalysis.parse(text);
//		
//		//���� ����ID-����ID ӳ���TermIdIndex
//		List<Long> termIDs = createTermIdIndex(parsedTerms);
//		
//		
//	}
//	
//
//	
//	private void addTermId2DB() {
////		termID.incrementAndGet();
////		DB: TermIdIndex
//		
//	}
//}
