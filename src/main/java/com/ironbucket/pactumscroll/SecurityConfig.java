package com.ironbucket.pactumscroll;

import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "ironbucket")
class SecurityProperties {
    private SecurityConfig security;
  
	public SecurityConfig getSecurity() {
		return security;
	}
	public void setSecurity(SecurityConfig security) {
		this.security = security;
	}
	record SecurityConfig(String secret) {}
}
