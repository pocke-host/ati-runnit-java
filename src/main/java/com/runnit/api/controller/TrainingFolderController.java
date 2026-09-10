package com.runnit.api.controller;

import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/training-folders") @RequiredArgsConstructor
public class TrainingFolderController {
    private final TrainingFolderRepository folders;
    private final TrainingFolderItemRepository items;
    private final UserRepository users;
    private Long uid(Authentication a){return (Long)a.getPrincipal();}
    private TrainingFolder owned(Long id, Long userId){
        TrainingFolder f=folders.findById(id).orElseThrow(()->new RuntimeException("Folder not found"));
        if(!f.getUser().getId().equals(userId)) throw new RuntimeException("Not authorized"); return f;
    }
    private Map<String,Object> map(TrainingFolder f){
        Map<String,Object> m=new LinkedHashMap<>(); m.put("id",f.getId()); m.put("name",f.getName()); m.put("description",f.getDescription());
        m.put("color",f.getColor()); m.put("targetDate",f.getTargetDate()); m.put("createdAt",f.getCreatedAt());
        m.put("items",items.findByFolderIdOrderByCreatedAtDesc(f.getId()).stream().map(i->Map.of("id",i.getId(),"itemType",i.getItemType(),"itemId",i.getItemId())).collect(Collectors.toList())); return m;
    }
    @GetMapping public ResponseEntity<?> list(Authentication a){return ResponseEntity.ok(folders.findByUserIdOrderByCreatedAtDesc(uid(a)).stream().map(this::map).toList());}
    @PostMapping @Transactional public ResponseEntity<?> create(@RequestBody Map<String,Object> b, Authentication a){
        TrainingFolder f=new TrainingFolder(); f.setUser(users.findById(uid(a)).orElseThrow()); f.setName(String.valueOf(b.getOrDefault("name","Training block")));
        f.setDescription((String)b.get("description")); f.setColor((String)b.get("color")); if(b.get("targetDate") instanceof String s&&!s.isBlank()) f.setTargetDate(LocalDate.parse(s)); return ResponseEntity.ok(map(folders.save(f)));
    }
    @PatchMapping("/{id}") @Transactional public ResponseEntity<?> update(@PathVariable Long id,@RequestBody Map<String,Object>b,Authentication a){
        TrainingFolder f=owned(id,uid(a)); if(b.containsKey("name"))f.setName(String.valueOf(b.get("name"))); if(b.containsKey("description"))f.setDescription((String)b.get("description")); if(b.containsKey("color"))f.setColor((String)b.get("color")); if(b.containsKey("targetDate"))f.setTargetDate(b.get("targetDate")==null?null:LocalDate.parse(String.valueOf(b.get("targetDate")))); return ResponseEntity.ok(map(folders.save(f)));
    }
    @DeleteMapping("/{id}") @Transactional public ResponseEntity<?> delete(@PathVariable Long id,Authentication a){TrainingFolder f=owned(id,uid(a)); folders.delete(f); return ResponseEntity.ok(Map.of("deleted",true));}
    @PostMapping("/{id}/items") @Transactional public ResponseEntity<?> add(@PathVariable Long id,@RequestBody Map<String,Object>b,Authentication a){
        TrainingFolder f=owned(id,uid(a)); String type=String.valueOf(b.getOrDefault("itemType","ACTIVITY")).toUpperCase(); Long itemId=((Number)b.get("itemId")).longValue();
        if(!Set.of("ACTIVITY","WORKOUT","PLAN").contains(type)) return ResponseEntity.badRequest().body(Map.of("error","itemType must be ACTIVITY, WORKOUT, or PLAN"));
        if(!items.existsByFolderIdAndItemTypeAndItemId(id,type,itemId)){TrainingFolderItem i=new TrainingFolderItem();i.setFolder(f);i.setItemType(type);i.setItemId(itemId);items.save(i);} return ResponseEntity.ok(map(f));
    }
    @DeleteMapping("/{id}/items/{type}/{itemId}") @Transactional public ResponseEntity<?> remove(@PathVariable Long id,@PathVariable String type,@PathVariable Long itemId,Authentication a){owned(id,uid(a));items.deleteByFolderIdAndItemTypeAndItemId(id,type.toUpperCase(),itemId);return ResponseEntity.ok(Map.of("removed",true));}
}
