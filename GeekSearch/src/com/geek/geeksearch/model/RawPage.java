package com.geek.geeksearch.model;

import java.sql.Date;

/**
 * ԭʼ��ҳ - ����head��content
 *
 */
public class RawPage {
	private String url = null;
	private Date date = null;
	private Integer length = null;
	//ͨ�� url ���� md5
	
	public RawPage() {
		
	}
	
	public static String getPageContent(String type, Integer offset) {
		
	}
	
	
	
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

}
