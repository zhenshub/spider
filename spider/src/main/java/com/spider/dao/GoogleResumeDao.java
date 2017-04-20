package com.spider.dao;

import java.util.List;

import com.spider.model.GoogleResult;

public interface GoogleResumeDao {

	public List<GoogleResult> finaAllResults();
	public void addResult(GoogleResult googleResult);
}
