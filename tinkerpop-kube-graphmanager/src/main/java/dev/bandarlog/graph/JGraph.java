package dev.bandarlog.graph;

import dev.bandarlog.graph.JGraph.JGraphSpec;
import dev.bandarlog.graph.JGraph.JGraphStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;

@Kind("JGraph")
@Singular("jgraph")
@Plural("jgraphs")
@Group("bandar-log.dev")
@Version("v1")
public class JGraph extends CustomResource<JGraphSpec, JGraphStatus> implements Namespaced {

	public static class JGraphSpec {
		public String configuration;
	}
	
	public static class JGraphStatus {
		public String status;
	}
}