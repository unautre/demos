package dev.bandarlog.demos.janusgraph;

import static org.janusgraph.graphdb.types.AccessorUtils.*;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.DefaultSchemaMaker;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder;
import org.janusgraph.core.schema.PropertyKeyMaker;
import org.janusgraph.graphdb.transaction.StandardJanusGraphTx;

/**
 * For every newly registered property (on the fly),
 * add it to the mixed index
 * and reindex asynchronously.
 */
public class AutoIndexer implements DefaultSchemaMaker {

	private final String VERTEX_INDEX_NAME = "VERTEX_INDEX_EVERYTHING";

	private final String backingIndex = "elasticsearch";

	@Override
	public PropertyKey makePropertyKey(PropertyKeyMaker factory) {
		final PropertyKey propertyKey = DefaultSchemaMaker.super.makePropertyKey(factory);

		final StandardJanusGraphTx tx = getTx(factory);

		final JanusGraphManagement mgmt = tx.getGraph().openManagement();

		try {
			JanusGraphIndex graphIndex = mgmt.getGraphIndex(VERTEX_INDEX_NAME);

			if (graphIndex == null) {
				final IndexBuilder ib = mgmt.buildIndex(VERTEX_INDEX_NAME, Vertex.class);

				ib.addKey(propertyKey);

				graphIndex = ib.buildMixedIndex(backingIndex);
			} else {
				mgmt.addIndexKey(graphIndex, propertyKey);
			}

			mgmt.addIndexKey(null, propertyKey, null);

			mgmt.commit();
		} catch (Exception exc) {
			mgmt.rollback();

			throw exc;
		}

		return propertyKey;
	}

	@Override
	public PropertyKey makePropertyKey(PropertyKeyMaker factory, Object value) {
		return DefaultSchemaMaker.super.makePropertyKey(factory, value);
	}

	@Override
	public Cardinality defaultPropertyCardinality(String key) {
		return Cardinality.SINGLE;
	}

	@Override
	public boolean ignoreUndefinedQueryTypes() {
		return true;
	}
}