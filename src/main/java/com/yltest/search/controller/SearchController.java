package com.yltest.search.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class SearchController {
    @RequestMapping(value="index")
    public ModelAndView index(){
        log.info("==========================index");
        ModelAndView mv = new ModelAndView();
        mv.setViewName("index");
        return mv;
    }
    @RequestMapping(value="search")
    public ModelAndView search(HttpServletRequest request){
        log.info("==========================search");
        try {
            request.setCharacterEncoding("UTF-8");
            String query = request.getParameter("query");
            String pageNumStr=request.getParameter("pageNum");

            int pageNum=1;

            if (pageNumStr!=null&&Integer.parseInt(pageNumStr)>1){
                pageNum=Integer.parseInt(pageNumStr);
            }
            searchContents(query, pageNum,request);

            request.setAttribute("queryBack", query);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ModelAndView mv = new ModelAndView();
        mv.setViewName("result");
        return mv;
    }

    private void searchContents(String query, int pageNum,HttpServletRequest req) {
        ArrayList<Map<String, Object>> newslist = new ArrayList<Map<String, Object>>();
        long start = System.currentTimeMillis();

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );
        SearchRequest searchRequest = new SearchRequest("ik_index");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("attachment.content",query);
        searchSourceBuilder.query(matchQueryBuilder);
        long totalHits = 0;
        try {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field("attachment.content");
            highlightTitle.highlighterType("fvh");
            highlightTitle.fragmentSize(60);
            highlightTitle.noMatchSize(50);
            highlightBuilder.preTags("<span style=\"color:red\">").postTags("</span>");
            highlightBuilder.field(highlightTitle);
            searchSourceBuilder.highlighter(highlightBuilder);
            searchSourceBuilder.from( (pageNum - 1)*10 );
            searchSourceBuilder.size(10);
            SearchResponse searchResponse = client.search(searchRequest.source(searchSourceBuilder), RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits.getHits()) {
                Map<String, Object> map = hit.getSourceAsMap();
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlight = highlightFields.get("attachment.content");
                Text[] fragments = highlight.fragments();
                String fragmentString = fragments[0].string();
                map.put("content",fragmentString);
                log.info("fragmentString[{}]",fragmentString);
                newslist.add(map);
            }
            totalHits = hits.getTotalHits().value;
        }catch (Exception e){
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        req.setAttribute("newslist", newslist);
        req.setAttribute("totalHits",  totalHits+ "");
//        req.setAttribute("pageNums",pageNum+"");
        req.setAttribute("totalTime", (end - start) + "");
    }
}
