package com.veggiepal.blog.feed;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.mapper.BlogMapper;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.dto.response.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class FollowingFeedService {
    private final FollowingAuthorClient authors;
    private final BlogRepository blogs;
    private final BlogMapper mapper;
    @Transactional(readOnly=true) public PageResponse<BlogSummaryResponse> get(String token,int page,int size) {
        int safeSize=Math.clamp(size,1,100);int safePage=Math.min(Math.max(0,page),Integer.MAX_VALUE/safeSize);
        List<Long> ids=authors.activeFollowingIds(token);
        if(ids.isEmpty()) return PageResponse.<BlogSummaryResponse>builder().items(List.of()).page(safePage).size(safeSize)
                .totalElements(0).totalPages(0).build();
        var result=blogs.findByAuthorIdInAndStatus(ids,ContentStatus.PUBLISHED,
                PageRequest.of(safePage,safeSize,Sort.by(Sort.Order.desc("publishedAt"),Sort.Order.desc("id"))));
        return PageResponse.<BlogSummaryResponse>builder().items(result.getContent().stream().map(mapper::toBlogSummaryResponse).toList())
                .page(result.getNumber()).size(result.getSize()).totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build();
    }
}
