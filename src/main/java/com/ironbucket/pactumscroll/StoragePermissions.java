package com.ironbucket.pactumscroll;

import java.util.List;


public class StoragePermissions {
    private List<StoragePermissionBucketRule> rules;
    public StoragePermissions() {}
    //cluster -> namespaces -> tenants -> systems(storage,compute,..) -> backends(s3,fs,..) -> containers(buckets,file,..) -> prefix
    public List<StoragePermissionBucketRule> getRules() {
        return rules;
    }

    public void setRules(List<StoragePermissionBucketRule> rules) {
        this.rules = rules;
    }

    
}

