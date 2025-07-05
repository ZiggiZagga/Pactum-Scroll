package com.ironbucket.pactumscroll;

import java.util.List;

public record StorageRequestContract (
		String system,
		String tenantId, 
		String path, 
		List<String> segments, 
		String backend,
		String container,
		String key,
		String method)  {}
