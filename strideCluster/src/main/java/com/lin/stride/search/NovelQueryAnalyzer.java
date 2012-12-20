package com.lin.stride.search;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import com.lin.stride.search.request.NovelSearchRequst;
import com.lin.stride.utils.EfficientWritable;

public class NovelQueryAnalyzer implements QueryAnalyzer {

	@Override
	public BooleanQuery parser(EfficientWritable qp) {
		
		NovelSearchRequst nsq = (NovelSearchRequst)qp;
		
		BooleanQuery result = new BooleanQuery();

		if (nsq.getNovelName() != null && !nsq.getNovelName().equals("")) {
			result.add(getPhraseQuery("name", nsq.getNovelName(), 0), Occur.MUST);
		}
		if (nsq.getNovelAuthor() != null && !nsq.getNovelAuthor().equals("")) {
			result.add(getPhraseQuery("author", nsq.getNovelAuthor(), 0), Occur.MUST);
		}
		if (nsq.getTag() != null && !nsq.getTag().equals("")) {
			result.add(getPhraseQuery("tag", nsq.getTag(), 0), Occur.MUST);
		}
		return result;
	}

	@Override
	public PhraseQuery getPhraseQuery(String fieldName, String value, int slop) {
		PhraseQuery pq = new PhraseQuery();
		pq.setSlop(slop);
		try {
			TokenStream ts = analyzer.tokenStream(fieldName, new StringReader(value));
			CharTermAttribute ca = ts.getAttribute(CharTermAttribute.class);
			while (ts.incrementToken()) {
				pq.add(new Term(fieldName, ca.toString()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pq;
	}

	public static void main(String[] args) {
		NovelQueryAnalyzer noa = new NovelQueryAnalyzer();
		noa.getPhraseQuery("", "你好啊四季度哈斯kd", 0);
	}

}
