package com.ironbucket.pactumscroll;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoragePermissionBucketRule {
	@NotNull @Pattern(regexp = "^[a-z0-9.-]{3,63}$") 
	private String bucket;
    private List<StoragePermissionPrefixRule> prefixes;
    
}
