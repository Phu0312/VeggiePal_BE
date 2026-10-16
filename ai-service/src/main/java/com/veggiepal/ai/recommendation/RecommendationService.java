package com.veggiepal.ai.recommendation;

import static com.veggiepal.ai.recommendation.RecommendationDtos.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.monitoring.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RecommendationService {
    private final RestClient http;
    private final String blogUrl;
    private final String recipeUrl;
    private final GroqClient groq;
    private final ModelProperties models;
    private final ObjectMapper mapper;
    private final UsageService usage;

    public RecommendationService(@Qualifier("serviceRestClient") RestClient http,
            @Value("${services.blog.base-url}") String blogUrl, @Value("${services.recipe.base-url}") String recipeUrl,
            GroqClient groq, ModelProperties models, ObjectMapper mapper, UsageService usage) {
        this.http=http; this.blogUrl=blogUrl; this.recipeUrl=recipeUrl; this.groq=groq; this.models=models; this.mapper=mapper; this.usage=usage;
    }

    public List<Recommendation> recommend(Long userId, String interest, int limit) {
        int safeLimit=Math.max(1,Math.min(limit,10));
        List<Candidate> candidates=new ArrayList<>();
        readItems(blogUrl+"/blogs?size=20"+query(interest),"items","BLOG",candidates);
        readItems(recipeUrl+"/recipes?size=20"+query(interest),"content","RECIPE",candidates);
        if (candidates.isEmpty()) return List.of();
        usage.checkBudget("user:"+userId);
        String compact;
        try { compact=mapper.writeValueAsString(candidates.stream().map(c -> Map.of("type",c.type,"id",c.id,"title",c.node.path("title").asText())).toList()); }
        catch (Exception e) { throw new AppException(ErrorCode.INVALID_REQUEST,e); }
        String prompt="Chọn tối đa "+safeLimit+" nội dung phù hợp sở thích '"+(interest==null?"ăn chay":interest)+"'. Chỉ dùng candidate đã cho. Trả JSON array [{\"type\":\"BLOG|RECIPE\",\"id\":1,\"reason\":\"...\"}]. Candidates: "+compact;
        long start=System.nanoTime();
        try {
            var result=groq.complete(models.recommendation(),List.of(new GroqRequest.Message("user",prompt)),0.2,500);
            usage.success(userId,"user:"+userId,"RECOMMENDATION",models.recommendation(),result,elapsed(start));
            JsonNode ranked=mapper.readTree(cleanJson(result.content()));
            if(!ranked.isArray()) throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
            Map<String,Candidate> byKey=new HashMap<>(); candidates.forEach(c -> byKey.put(c.type+":"+c.id,c));
            List<Recommendation> response=new ArrayList<>();
            java.util.Set<String> seen=new java.util.HashSet<>();
            for(JsonNode item:ranked) {
                String type=item.path("type").asText(); long id=item.path("id").asLong(-1); Candidate c=byKey.get(type+":"+id);
                String key=type+":"+id;
                if(c!=null && seen.add(key) && response.size()<safeLimit) response.add(new Recommendation(type,id,item.path("reason").asText(),c.node));
            }
            return response;
        } catch (AppException e) { usage.failure(userId,"user:"+userId,"RECOMMENDATION",models.recommendation(),elapsed(start),e); throw e; }
        catch (Exception e) { usage.failure(userId,"user:"+userId,"RECOMMENDATION",models.recommendation(),elapsed(start),e); throw new AppException(ErrorCode.INVALID_AI_RESPONSE,e); }
    }
    private void readItems(String url,String field,String type,List<Candidate> output) {
        try {
            JsonNode root=http.get().uri(url).retrieve().body(JsonNode.class);
            JsonNode items=root==null?null:root.path("result").path(field);
            if(items!=null&&items.isArray()) items.forEach(n -> { if(n.path("id").canConvertToLong()) output.add(new Candidate(type,n.path("id").asLong(),n)); });
        } catch (Exception e) { throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE,e); }
    }
    private String query(String value) { return value==null||value.isBlank()?"":"&keyword="+java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8); }
    private String cleanJson(String raw) { return raw.replaceFirst("(?s)^```(?:json)?\\s*","").replaceFirst("\\s*```$","").trim(); }
    private long elapsed(long start) { return (System.nanoTime()-start)/1_000_000; }
    private record Candidate(String type,long id,JsonNode node) {}
}
