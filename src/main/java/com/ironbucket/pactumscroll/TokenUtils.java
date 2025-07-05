package com.ironbucket.pactumscroll;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TokenUtils {
	private static final String ALGORITHM = "HmacSHA256";
	public static final String SIGNATURE_HEADER_NAME = "X-Signature";
	public static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";
	public static final String TIMESTAMP_HEADER_NAME = "X-Timestamp";
	
	
	public static JsonNode getClaims(ServerWebExchange exchange) {
		String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
		String path = exchange.getRequest().getURI().getPath();
		log.debug("Request path: " + path);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return null;
		}

		
		JsonNode claims = null;
		
		try {
			String token = authHeader.substring(7);
			SignedJWT jwt = SignedJWT.parse(token);
			claims = new ObjectMapper().valueToTree(jwt.getJWTClaimsSet().getClaims());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return claims;
	}
	
	public static String signPayload(String data, String key, ObjectMapper mapper) {
		if(data != null && key != null) {
			try {
				SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);    	         
				Mac mac = Mac.getInstance(ALGORITHM);    	            
				mac.init(secretKey);
				
				byte[] rawHmac = mac.doFinal(data.getBytes());
				return Base64.getEncoder().encodeToString(rawHmac);
			} catch (Exception e) {
			
				throw new IllegalStateException("Error generating HMAC", e);
			}
		}else {
			return null;
		}
	}

	private static StoragePermissions storagePermissionConvert(ObjectMapper mapper, JsonNode claim) throws JsonProcessingException, IllegalArgumentException {
		String val = claim.get("storage_permissions").asText();
		return new ObjectMapper()
		    .readValue(val, StoragePermissions.class);	        	   
	}
	
	public static StoragePermissions getStoragePermissions(ServerWebExchange exchange, ObjectMapper mapper) throws JsonProcessingException, IllegalArgumentException {
		return storagePermissionConvert(mapper, getClaims(exchange));
	}
	public static StorageRequestContract extractFromRequestPath(ServerWebExchange exchange) {
		String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
		String path = exchange.getRequest().getURI().getPath();
		List<String> segments =Arrays.stream(path.split("/"))
			      .filter(s -> !s.isEmpty()).toList();
		ArrayNode segmentsNode = new ObjectMapper().createArrayNode();
		segments.stream()
		      .filter(s -> !s.isEmpty())
		      .forEach(segmentsNode::add);
		
		String system = segments.get(0);
		String backend = segments.get(1);
		String container = segments.get(2);
		String key = segments.stream().skip(3).collect(Collectors.joining("/"));
		HttpMethod method = exchange.getRequest().getMethod();
		String methodName = toStorageMethod(method).name();
		StorageRequestContract contract = new StorageRequestContract(system, tenantId, path, segments, backend, container, key,methodName);
		log.debug(contract.toString());
		return contract;
	}

	private static StorageMethodTypes toStorageMethod(HttpMethod httpMethod) {
		String method = httpMethod.name();
		return switch (method) {
		    case "GET"    -> StorageMethodTypes.DOWNLOAD;
		    case "POST", "PUT" -> StorageMethodTypes.UPLOAD;
		    case "DELETE" -> StorageMethodTypes.DELETE;
		    case "HEAD", "OPTIONS", "PATCH" -> StorageMethodTypes.LIST;
		    default       -> throw new IllegalArgumentException("Unsupported method: " + method);
		};
    }
}