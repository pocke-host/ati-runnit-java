package com.runnit.api.controller;
import com.runnit.api.service.CorosCoachService; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.net.URI; import java.util.Map;
@RestController @RequestMapping("/api/coros-coach") @RequiredArgsConstructor
public class CorosCoachController {
 private final CorosCoachService coach;
 @GetMapping("/connect") public ResponseEntity<?> connect(Authentication a){return ResponseEntity.ok(Map.of("url",coach.connect((Long)a.getPrincipal())));}
 @GetMapping("/callback") public ResponseEntity<Void> callback(@RequestParam String code,@RequestParam String state){return ResponseEntity.status(302).location(URI.create(coach.callback(code,state))).build();}
 @GetMapping("/status") public Map<String,Object> status(Authentication a){return coach.status((Long)a.getPrincipal());}
 @GetMapping("/brief") public Map<String,Object> brief(Authentication a){return coach.brief((Long)a.getPrincipal());}
}
