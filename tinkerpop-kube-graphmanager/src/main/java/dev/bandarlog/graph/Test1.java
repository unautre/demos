package dev.bandarlog.graph;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
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
		
		client.genericKubernetesResources(context).watch(new Watcher<GenericKubernetesResource>() {

			@Override
			public void eventReceived(Action action, GenericKubernetesResource resource) {
				System.out.println("received: " + action + " - " + resource);
			}

			@Override
			public void onClose(WatcherException cause) {
				System.out.println("on close: " + cause);
			}
		});
		
		System.out.println("After the .watch()");
	}
}