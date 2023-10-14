package org.janusgraph.graphdb.types;

import org.janusgraph.core.schema.PropertyKeyMaker;
import org.janusgraph.graphdb.transaction.StandardJanusGraphTx;

public class AccessorUtils {
	
	public static StandardJanusGraphTx getTx(PropertyKeyMaker maker) {
		final StandardPropertyKeyMaker standardMaker = (StandardPropertyKeyMaker) maker;
		
		return standardMaker.tx;
	}
}