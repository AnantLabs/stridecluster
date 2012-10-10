package com.lin.stride.index;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.Norm;
import org.apache.lucene.search.similarities.DefaultSimilarity;

public class NovelSimilarity extends DefaultSimilarity{

	@Override
	public float tf(float freq) {
		return 1f;
	}
	
	@Override
	  public void computeNorm(FieldInvertState state, Norm norm) {
	    final int numTerms;
	    if (discountOverlaps)
	      numTerms = state.getLength() - state.getNumOverlap();
	    else
	      numTerms = state.getLength();
	    norm.setByte(encodeNormValue(state.getBoost() * ((float) (1.0 / Math.sqrt(numTerms)))));
	  }
	@Override
	public float idf(long docFreq, long numDocs) {
		return 1f;
	}
	
}
