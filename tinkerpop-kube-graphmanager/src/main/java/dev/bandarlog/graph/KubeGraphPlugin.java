package dev.bandarlog.graph;

import java.util.Optional;

import org.apache.tinkerpop.gremlin.jsr223.Customizer;
import org.apache.tinkerpop.gremlin.jsr223.GremlinPlugin;

public class KubeGraphPlugin implements GremlinPlugin {

	@Override
	public String getName() {
		return "KubeGraphPlugin";
	}

	@Override
	public Optional<Customizer[]> getCustomizers(String scriptEngineName) {
		return Optional.empty();
	}
}