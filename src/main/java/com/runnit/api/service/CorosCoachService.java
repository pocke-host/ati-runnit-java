package com.runnit.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.*;

@Service @RequiredArgsConstructor
public class CorosCoachService {
    private final UserRepository users; private final RestTemplate http; private final ObjectMapper mapper;
    @Value("${app.frontend.url:https://runnit.live}") private String frontendUrl;
    @Value("${coros.mcp.base-url:https://mcpus.coros.com}") private String baseUrl;
    @Value("${coros.mcp.redirect-uri:https://ati-runnit-java.onrender.com/api/coros-coach/callback}") private String callbackUri;
    private String redirect(){return callbackUri;}

    @Transactional
    public String connect(Long userId){
        User u=users.findById(userId).orElseThrow(); String clientId=u.getCorosMcpClientId();
        if(clientId==null||clientId.isBlank()){
            Map<String,Object> registration=new LinkedHashMap<>(); registration.put("client_name","RUNNIT COROS Coach"); registration.put("redirect_uris",List.of(redirect())); registration.put("grant_types",List.of("authorization_code","refresh_token")); registration.put("response_types",List.of("code")); registration.put("token_endpoint_auth_method","none"); registration.put("scope","openid mcp.tools offline_access");
            ResponseEntity<Map> res=http.postForEntity(baseUrl+"/connect/register",registration,Map.class); clientId=String.valueOf(Objects.requireNonNull(res.getBody()).get("client_id")); u.setCorosMcpClientId(clientId); users.save(u);
        }
        String state=UUID.randomUUID().toString(); u.setCorosMcpOauthState(state); users.save(u);
        return baseUrl+"/oauth2/authorize?client_id="+enc(clientId)+"&redirect_uri="+enc(redirect())+"&response_type=code&code_challenge_method=S256&code_challenge="+enc(challenge(state))+"&scope="+enc("openid mcp.tools offline_access")+"&state="+enc(state);
    }

    @Transactional
    public String callback(String code,String state){
        User u=users.findByCorosMcpOauthState(state).orElseThrow(()->new IllegalStateException("COROS Coach authorization expired"));
        org.springframework.util.MultiValueMap<String,String> body=new org.springframework.util.LinkedMultiValueMap<>(); body.add("grant_type","authorization_code"); body.add("code",code); body.add("client_id",u.getCorosMcpClientId()); body.add("redirect_uri",redirect()); body.add("code_verifier",state);
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); ResponseEntity<Map> res=http.postForEntity(baseUrl+"/oauth2/token",new HttpEntity<>(body,headers),Map.class); Map<String,Object> t=Objects.requireNonNull(res.getBody()); saveTokens(u,t); u.setCorosMcpOauthState(null); users.save(u); return frontendUrl+"/coros-coach?connected=1";
    }

    public Map<String,Object> status(Long userId){User u=users.findById(userId).orElseThrow(); return Map.of("connected",u.getCorosMcpAccessToken()!=null,"expiresAt",u.getCorosMcpTokenExpiresAt()==null?0:u.getCorosMcpTokenExpiresAt());}

    public Map<String,Object> brief(Long userId){
        User u=users.findById(userId).orElseThrow(); if(u.getCorosMcpAccessToken()==null) throw new IllegalStateException("Connect COROS Coach first"); ensureFresh(u);
        Map<String,Object> out=new LinkedHashMap<>(); out.put("recovery",callTool(u,"queryRecoveryStatus",Map.of())); out.put("trainingLoad",callTool(u,"queryTrainingLoadAssessment",Map.of())); out.put("fitness",callTool(u,"queryFitnessAssessmentOverview",Map.of())); return out;
    }
    private Object callTool(User u,String name,Map<String,Object> args){
        Map<String,Object> req=new LinkedHashMap<>(); req.put("jsonrpc","2.0"); req.put("id",UUID.randomUUID().toString()); req.put("method","tools/call"); req.put("params",Map.of("name",name,"arguments",args));
        HttpHeaders h=new HttpHeaders(); h.setBearerAuth(u.getCorosMcpAccessToken()); h.setAccept(List.of(MediaType.APPLICATION_JSON,MediaType.TEXT_EVENT_STREAM)); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response=http.exchange(baseUrl+"/mcp",HttpMethod.POST,new HttpEntity<>(req,h),String.class); return response.getBody();
    }
    private void saveTokens(User u,Map<String,Object> t){u.setCorosMcpAccessToken((String)t.get("access_token")); if(t.get("refresh_token")!=null)u.setCorosMcpRefreshToken((String)t.get("refresh_token")); long e=((Number)t.getOrDefault("expires_in",3600)).longValue();u.setCorosMcpTokenExpiresAt(Instant.now().getEpochSecond()+e);}
    private void ensureFresh(User u){
        if(u.getCorosMcpTokenExpiresAt()!=null && Instant.now().getEpochSecond()<u.getCorosMcpTokenExpiresAt()-60) return;
        if(u.getCorosMcpRefreshToken()==null) throw new IllegalStateException("COROS Coach authorization expired");
        org.springframework.util.MultiValueMap<String,String> body=new org.springframework.util.LinkedMultiValueMap<>(); body.add("grant_type","refresh_token"); body.add("refresh_token",u.getCorosMcpRefreshToken()); body.add("client_id",u.getCorosMcpClientId());
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); Map<String,Object> t=Objects.requireNonNull(http.postForEntity(baseUrl+"/oauth2/token",new HttpEntity<>(body,headers),Map.class).getBody()); saveTokens(u,t); users.save(u);
    }
    private String challenge(String verifier){try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
