package dev.bandarlog.graph;

import java.util.UUID;

import dev.bandarlog.graph.JGraph.JGraphStatus;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

public class Test1 {

	public static void main(String[] args) {
		KubernetesClient client = new KubernetesClientBuilder().build();

		final ResourceDefinitionContext context = new ResourceDefinitionContext.Builder() //
				.withGroup("bandarlog.dev")
				.withKind("JGraph")
				.withNamespaced(true)
				.withPlural("jgraphs")
				.withVersion("v1")
				.build();
		
		final MixedOperation<JGraph, KubernetesResourceList<JGraph>, Resource<JGraph>> graphClient = client.resources(JGraph.class);
		
		graphClient.watch(new Watcher<JGraph>() {

			@Override
			public void eventReceived(Action action, JGraph resource) {
				System.out.println("received: " + action + " - " + resource.getMetadata().getName());
				
//				String lastAppliedConfiguration = resource.getMetadata().getAnnotations().get("kubectl.kubernetes.io/last-applied-configuration");
//				System.out.println("\tBefore: " + lastAppliedConfiguration);
				
				System.out.println("\tNow: " + resource.getSpec().configuration);
				
				resource.setStatus(new JGraphStatus());
				resource.getStatus().status = "Written: " + UUID.randomUUID();
				
				graphClient.resource(resource).update();
			}

			@Override
			public void onClose(WatcherException cause) {
				System.out.println("on close: " + cause);
			}
		});
		
		System.out.println("After the .watch()");
	}
}