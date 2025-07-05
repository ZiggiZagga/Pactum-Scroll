package com.ironbucket.pactumscroll;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Validated
public class IronBucketVerificationFilterFacory extends AbstractGatewayFilterFactory<IronBucketVerificationFilterFacory.Config> {

	private ObjectMapper mapper;
	
	
	@Value("${ironbucket.security.secret}")
	private String SIG_K;

	
	
	public IronBucketVerificationFilterFacory() {
		super(Config.class);
		this.mapper = new ObjectMapper();
	}

	@Override
	public GatewayFilter apply(Config config) {		
		return (exchange, chain)->{
			ServerHttpRequest mutatedRequest = exchange.getRequest();
			try {
				
				StorageRequestContract storageRequestInfo = TokenUtils.extractFromRequestPath(exchange);	
				String correlationId = exchange.getRequest().getHeaders().getFirst(TokenUtils.CORRELATION_ID_HEADER_NAME);;
				log.info("request ID: "+correlationId+", path: "+storageRequestInfo.path()+", method: "+storageRequestInfo.method());
				String requestPayload = mapper.writeValueAsString(storageRequestInfo);				
				String payloadSignature = TokenUtils.signPayload(requestPayload, SIG_K, mapper);
				

				if((payloadSignature+"").length() > 35) {
					
					String headerSignature = exchange.getRequest().getHeaders().getFirst(TokenUtils.SIGNATURE_HEADER_NAME);					
					if(payloadSignature.equals(headerSignature)) {
						
						StoragePermissions claims = TokenUtils.getStoragePermissions(exchange, mapper);
						
						List<String> permittedPaths = 
								claims.getRules().stream()
								.filter(rule -> rule.getBucket().equals(storageRequestInfo.container()))
								.flatMap(rule->{						
									log.debug("permitted Bucket: "+rule.getBucket());
									return  rule.getPrefixes().stream()
											.filter(prefix -> storageRequestInfo.key().startsWith(prefix.getName()))											
											.flatMap(prefix->{
												String name = prefix.getName();
												log.debug("permitted Prefix: "+name);
												
												return prefix.getAllowedMethods().stream()
														.map(method -> {
															log.debug("Method: "+method.name());
															return method;
														})
												.filter(method -> method.name().equals(storageRequestInfo.method()))
												.map(method -> rule.getBucket()+"/"+storageRequestInfo.key());																							
											});
								})
								.toList();
						
						if(permittedPaths.size() == 1) {
							
							String timestamp = String.valueOf(Instant.now().toEpochMilli());
							StorageOperation operation = new StorageOperation(permittedPaths.getFirst(),true);
							StorageRequestPolicy policy = new StorageRequestPolicy(storageRequestInfo,operation);
							String policyPayload = mapper.writeValueAsString(policy);
							String policyPayloadPretty = mapper.writerWithDefaultPrettyPrinter()
									.writeValueAsString(policy);
							String policySignature = TokenUtils.signPayload(policyPayload, SIG_K, mapper);
							String info =  String.format("""
									%6$s: %1$s
									%5$s: %2$s
									%7$s: %3$s
									X-Payload: %4$s
									"""
									,correlationId
									,policySignature
									,timestamp
									,policyPayloadPretty
									,TokenUtils.SIGNATURE_HEADER_NAME
									,TokenUtils.CORRELATION_ID_HEADER_NAME
									,TokenUtils.TIMESTAMP_HEADER_NAME);

						
							log.debug(info);
							mutatedRequest = mutatedRequest
									.mutate()
									.header(TokenUtils.CORRELATION_ID_HEADER_NAME, correlationId)
									.header(TokenUtils.SIGNATURE_HEADER_NAME, policySignature)
									.header(TokenUtils.TIMESTAMP_HEADER_NAME, timestamp)
									.build();	
						}else {
							log.error("operation not permitted");
							return Mono.error(new AccessDeniedException("AccessDenied"));
						}
					}else {
						log.error("Illegal request");
						return Mono.error(new AccessDeniedException("AccessDenied, Illegal request"));
					}									
				}else {
					log.error("Illegal signature");
					return Mono.error(new AccessDeniedException("AccessDenied, Illegal signature"));
				}	
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return Mono.error(new AccessDeniedException("AccessDenied, Illegal wrong permission schema"));
			}
			return chain.filter(exchange.mutate().request(mutatedRequest).build());
		};
	}


	@Override
	public String name() {
		return "IronBucketVerification";
	}

	
	record Config() {};
	
	
}