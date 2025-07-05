package com.ironbucket.pactumscroll;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoragePermissionPrefixRule {
	private String name;
	private List<StorageMethodTypes> allowedMethods;
	private Map<String, List<String>> tags;
}
