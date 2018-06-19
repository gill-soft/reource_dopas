package com.gillsoft;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractResourceService;
import com.gillsoft.model.Method;
import com.gillsoft.model.MethodType;
import com.gillsoft.model.Ping;
import com.gillsoft.model.Resource;

@RestController
public class ResourceServiceController extends AbstractResourceService {

	@Override
	public Resource getInfoResponse() {
		Resource resource = new Resource();
		resource.setCode("DOPAS");
		resource.setName("Днепропетровский ОПАС");
		return resource;
	}

	@Override
	public List<Method> getAvailableMethodsResponse() {
		List<Method> methods = new ArrayList<>();
		addMethod(methods, "Resource activity check", Method.PING, MethodType.GET);
		addMethod(methods, "Information about resource", Method.INFO, MethodType.GET);
		addMethod(methods, "Available methods", Method.METHOD, MethodType.GET);
		addMethod(methods, "All available resource localities", Method.LOCALITY_ALL, MethodType.POST);
		addMethod(methods, "All used resource localities", Method.LOCALITY_USED, MethodType.POST);
		addMethod(methods, "Binding from resource localities", Method.LOCALITY_BINDING, MethodType.POST);
		addMethod(methods, "Init search", Method.SEARCH, MethodType.POST);
		addMethod(methods, "Return search result", Method.SEARCH, MethodType.GET);
		return methods;
	}
	
	private void addMethod(List<Method> methods, String name, String url, MethodType type) {
		Method method = new Method();
		method.setName(name);
		method.setUrl(url);
		method.setType(type);
		methods.add(method);
	}

	@Override
	public Ping pingResponse(String id) {
		Ping ping = new Ping();
		ping.setId(id);
		return ping;
	}

}
