package com.lin.stride.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.util.Version;

import com.lin.stride.utils.EfficientWritable;

public interface QueryAnalyzer {

	public static final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
	
	public BooleanQuery parser(EfficientWritable qp);

	public PhraseQuery getPhraseQuery(String fieldName, String value, int slop);

}
