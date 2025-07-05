package com.ironbucket.pactumscroll;

public record StorageRequestPolicy (
		StorageRequestContract contract, 
		StorageOperation operation)  {}
