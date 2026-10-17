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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RecommendationService {
    private static final Logger log=LoggerFactory.getLogger(RecommendationService.class);
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
        List<Candidate> candidates=fetchCandidates(query(interest));
        // A keyword that matches no title would leave the AI nothing to rank; let it pick from the latest items.
        if (candidates.isEmpty() && !query(interest).isEmpty()) candidates=fetchCandidates("");
        if (candidates.isEmpty()) return List.of();
        usage.checkBudget("user:"+userId);
        String compact;
        try { compact=mapper.writeValueAsString(candidates.stream().map(c -> Map.of("type",c.type,"id",c.id,"title",c.node.path("title").asText())).toList()); }
        catch (Exception e) { throw new AppException(ErrorCode.INVALID_REQUEST,e); }
        String prompt="Chọn tối đa "+safeLimit+" nội dung phù hợp sở thích '"+(interest==null?"ăn chay":interest)+"'. Chỉ dùng candidate đã cho. Trả JSON array [{\"type\":\"BLOG|RECIPE\",\"id\":1,\"reason\":\"...\"}]. Candidates: "+compact;
        long start=System.nanoTime();
        try {
            // gpt-oss spends part of max_completion_tokens on reasoning; 500 cut the JSON short.
            var result=groq.complete(models.recommendation(),List.of(new GroqRequest.Message("user",prompt)),0.2,1500);
            usage.success(userId,"user:"+userId,"RECOMMENDATION",models.recommendation(),result,elapsed(start));
            JsonNode ranked=mapper.readTree(cleanJson(result.content()));
            if(!ranked.isArray()) throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
            Map<String,Candidate> byKey=new HashMap<>(); candidates.forEach(c -> byKey.put(c.type+":"+c.id,c));
            List<Recommendation> response=new ArrayList<>();
            java.util.Set<String> seen=new java.util.HashSet<>();
            for(JsonNode item:ranked) {
                String type=item.path("type").asText(); long id=item.path("id").asLong(-1); Candidate c=byKey.get(type+":"+id);
                String key=type+":"+id;
                if(c!=null && seen.add(key) && response.size()<safeLimit) response.add(new Recommendation(type,id,item.path("reason").asText(),mapper.convertValue(c.node,Object.class)));
            }
            return response;
        } catch (AppException e) { usage.failure(userId,"user:"+userId,"RECOMMENDATION",models.recommendation(),elapsed(start),e); throw e; }
        catch (Exception e) { usage.failure(userId,"user:"+userId,"RECOMMENDATION",models.recommendation(),elapsed(start),e); throw new AppException(ErrorCode.INVALID_AI_RESPONSE,e); }
    }
    // One source down still leaves the other to recommend from; only both down is an outage.
    private List<Candidate> fetchCandidates(String keywordQuery) {
        List<Candidate> output=new ArrayList<>();
        boolean blogs=readItems(blogUrl+"/blogs?size=20"+keywordQuery,"items","BLOG",output);
        boolean recipes=readItems(recipeUrl+"/recipes?size=20"+keywordQuery,"content","RECIPE",output);
        if (!blogs && !recipes) throw new AppException(ErrorCode.DOWNSTREAM_UNAVAILABLE);
        return output;
    }
    private boolean readItems(String url,String field,String type,List<Candidate> output) {
        try {
            JsonNode root=http.get().uri(url).retrieve().body(JsonNode.class);
            JsonNode items=root==null?null:root.path("result").path(field);
            if(items!=null&&items.isArray()) items.forEach(n -> { if(n.path("id").canConvertToLong()) output.add(new Candidate(type,n.path("id").asLong(),n)); });
            return true;
        } catch (Exception e) { log.warn("Recommendation candidates unavailable from {}: {}",url,e.getMessage()); return false; }
    }
    private String query(String value) { return value==null||value.isBlank()?"":"&keyword="+java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8); }
    // Models often wrap the array in a sentence or a ```json fence; keep only the outermost [...].
    private String cleanJson(String raw) {
        int start=raw.indexOf('['), end=raw.lastIndexOf(']');
        return start>=0 && end>start ? raw.substring(start,end+1) : raw.trim();
    }
    private long elapsed(long start) { return (System.nanoTime()-start)/1_000_000; }
    private record Candidate(String type,long id,JsonNode node) {}
}
