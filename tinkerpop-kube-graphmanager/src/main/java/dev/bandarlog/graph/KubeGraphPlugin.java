package dev.bandarlog.graph;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.jsr223.Customizer;
import org.apache.tinkerpop.gremlin.jsr223.GremlinPlugin;
import org.janusgraph.core.ConfiguredGraphFactory;
import org.janusgraph.graphdb.management.ConfigurationManagementGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bandarlog.graph.JGraph.JGraphStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

public class KubeGraphPlugin implements GremlinPlugin {

	@Override
	public String getName() {
		return "KubeGraphPlugin";
	}

	@Override
	public Optional<Customizer[]> getCustomizers(String scriptEngineName) {
		start();

		return Optional.empty();
	}

	private void start() {
		final KubernetesClient client = new KubernetesClientBuilder().build();

		final GraphWatcher watcher = new GraphWatcher(client);

		client.resources(JGraph.class).watch(watcher);
	}

	public static class GraphWatcher implements Watcher<JGraph> {

		private static final Logger LOGGER = LoggerFactory.getLogger(GraphWatcher.class);

		private final KubernetesClient client;

		public GraphWatcher(KubernetesClient client) {
			this.client = client;
		}

		@Override
		public void eventReceived(Action action, JGraph resource) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Graph {}: {}/{}", action, resource.getMetadata().getNamespace(),
						resource.getMetadata().getName());
			}

			final String name = resource.getMetadata().getName();
			boolean doUpdateStatus = true;
			String status = null;

			try {
				if (action == Action.ADDED) {

					final String configuration = resource.getSpec().configuration;

					if (configuration == null || configuration.isEmpty()) {
						ConfiguredGraphFactory.create(name);
					} else {
						final PropertiesConfiguration props = new PropertiesConfiguration();
						props.load(new StringReader(configuration));

						if (!props.containsKey(ConfigurationManagementGraph.PROPERTY_GRAPH_NAME)) {
							props.setProperty(ConfigurationManagementGraph.PROPERTY_GRAPH_NAME, name);
						}

						ConfiguredGraphFactory.createConfiguration(props);
					}

					status = "Graph created.";
				} else if (action == Action.MODIFIED) {
					throw new UnsupportedOperationException();
				} else if (action == Action.DELETED) {
					doUpdateStatus = false;

					ConfiguredGraphFactory.drop(name);
				}

				if (doUpdateStatus) {
					resource.setStatus(new JGraphStatus());
					resource.getStatus().status = status;

					client.resource(resource).update();
				}
			} catch (Exception exc) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error("Could not load graph {}", name, exc);
				}

				if (doUpdateStatus) {
					final StringWriter writer = new StringWriter();
					exc.printStackTrace(new PrintWriter(writer));

					resource.setStatus(new JGraphStatus());
					resource.getStatus().status = writer.toString();

					client.resource(resource).update();
				}
			}
		}

		@Override
		public void onClose(WatcherException cause) {
			System.out.println("on close: " + cause);
		}
	}
}