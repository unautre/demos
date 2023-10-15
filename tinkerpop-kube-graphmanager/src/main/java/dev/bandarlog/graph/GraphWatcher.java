package dev.bandarlog.graph;

import java.io.IOException;

import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.ListOptions;
import okhttp3.Call;

public class GraphWatcher {

	public static void main(String[] args) throws IOException, ApiException {
		final String apiGroup = "bandarlog.dev", apiVersion = "v1", resourcePlural = "jgraphs";
		final String namespace = "default";
		
	    final ApiClient apiClient = ClientBuilder.standard().build();
	    
	    // retrieving the latest state of the default namespace
//	    DynamicKubernetesApi dynamicApi = new DynamicKubernetesApi("bandarlog.dev", "v1", "jgraphs", apiClient);
	    
	    final CustomObjectsApi customObjectsApi = new CustomObjectsApi(apiClient);
	    
	    final ListOptions listOptions = new ListOptions();
        final Call call = customObjectsApi.listNamespacedCustomObjectCall(
                apiGroup,
                apiVersion,
                namespace,
                resourcePlural,
                null,
                null,
                listOptions.getContinue(),
                listOptions.getFieldSelector(),
                listOptions.getLabelSelector(),
                listOptions.getLimit(),
                listOptions.getResourceVersion(),
                null,
                listOptions.getTimeoutSeconds(),
                true,
                null);
	    
	    final Watch<DynamicKubernetesObject> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<DynamicKubernetesObject>>() {}.getType());
	    
	    for (Response<DynamicKubernetesObject> obj : watch) {
	    	System.out.println(obj);
	    	System.out.println(obj.type);
	    	System.out.println(obj.object);
	    	System.out.println(obj.status);
	    }
	    
//	    dynamicApi.list("default")
	    
//	    DynamicKubernetesObject defaultNamespace =
//	        dynamicApi.get("default").throwsApiException().getObject();
//
//	     attaching a "foo=bar" label to the default namespace
//	    defaultNamespace.setMetadata(defaultNamespace.getMetadata().putLabelsItem("foo", "bar"));
//	    DynamicKubernetesObject updatedDefaultNamespace =
//	        dynamicApi.update(defaultNamespace).throwsApiException().getObject();
//
//	    System.out.println(updatedDefaultNamespace);
//
//	    apiClient.getHttpClient().connectionPool().evictAll();
	}
}