package dev.bandarlog.graph;

import java.io.IOException;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;

public class TestWatch {

	public static void main(String[] args) throws IOException, ApiException {
	    ApiClient apiClient = ClientBuilder.standard().build();

	    // retrieving the latest state of the default namespace
	    DynamicKubernetesApi dynamicApi = new DynamicKubernetesApi("", "v1", "namespaces", apiClient);
	    DynamicKubernetesObject defaultNamespace =
	        dynamicApi.get("default").throwsApiException().getObject();

	    // attaching a "foo=bar" label to the default namespace
	    defaultNamespace.setMetadata(defaultNamespace.getMetadata().putLabelsItem("foo", "bar"));
	    DynamicKubernetesObject updatedDefaultNamespace =
	        dynamicApi.update(defaultNamespace).throwsApiException().getObject();

	    System.out.println(updatedDefaultNamespace);

	    apiClient.getHttpClient().connectionPool().evictAll();
	}
}