package dev.bandarlog.graph;

import java.io.Console;
import java.io.IOException;

import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesListObject;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.ListOptions;
import okhttp3.Call;

public class GraphInformer {

	public static void main(String[] args) throws IOException, ApiException {
		final String apiGroup = "bandarlog.dev", apiVersion = "v1", resourcePlural = "jgraphs";
		final String namespace = "default";
		
	    final ApiClient apiClient = ClientBuilder.standard().build();
	    
	    // retrieving the latest state of the default namespace
//	    DynamicKubernetesApi dynamicApi = new DynamicKubernetesApi("bandarlog.dev", "v1", "jgraphs", apiClient);
	    final CustomObjectsApi customObjectsApi = new CustomObjectsApi(apiClient);
	    
	    SharedInformerFactory factory = new SharedInformerFactory(apiClient);

	    // Node informer
	    SharedIndexInformer<DynamicKubernetesObject> nodeInformer =
	        factory.sharedIndexInformerFor(
	            // **NOTE**:
	            // The following "CallGeneratorParams" lambda merely generates a stateless
	            // HTTPs requests, the effective apiClient is the one specified when constructing
	            // the informer-factory.
	            (CallGeneratorParams params) -> {
	              return customObjectsApi.listNamespacedCustomObjectCall(
	                      apiGroup,
	                      apiVersion,
	                      namespace,
	                      resourcePlural,
	                      null,
	                      null,
	                      null,
	                      null,
	                      null,
	                      null,
	                      params.resourceVersion,
	                      null,
	                      params.timeoutSeconds,
	                      params.watch,
	                      null);
	            },
	            DynamicKubernetesObject.class,
	            DynamicKubernetesListObject.class);

	    nodeInformer.addEventHandler(new ResourceEventHandler<DynamicKubernetesObject>() {
			
			@Override
			public void onUpdate(DynamicKubernetesObject oldObj, DynamicKubernetesObject newObj) {
				System.out.println("UPDATE: " + newObj.getMetadata().getName());
			}
			
			@Override
			public void onDelete(DynamicKubernetesObject obj, boolean deletedFinalStateUnknown) {
				System.out.println("DELETE: " + obj.getMetadata().getName());
			}
			
			@Override
			public void onAdd(DynamicKubernetesObject obj) {
				System.out.println("ADD: " + obj.getMetadata().getName());
			}
		});
	    
	    factory.startAllRegisteredInformers();
	    
	    System.out.println("Press enter to stop.");
	    System.console().readLine();
	    
//	    apiClient.getHttpClient().connectionPool().evictAll();
	}
}