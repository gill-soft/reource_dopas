package com.gillsoft.client;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gillsoft.util.StringUtil;

public abstract class AbstractModel {
	
	public String asString() {
		try {
			return StringUtil.objectToJsonBase64String(this);
		} catch (JsonProcessingException e) {
			return "";		}
	}
	
	public AbstractModel create(String json) {
		try {
			return StringUtil.jsonBase64StringToObject(getClass(), json);
		} catch (IOException e) {
			return null;
		}
	}

}
