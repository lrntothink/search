package com.yltest.search;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineResponse;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.ingest.PipelineConfiguration;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
class SearchApplicationTests {
    @Test
    void tt(){
        File f = new File("d:/listall.txt");
        log.info(f.getName());
        log.info(f.getAbsoluteFile()+"");
        log.info(f.getAbsolutePath());
    }
    @Test
    void base64(){
        String str = "我担心的是，奥运相关的行业会变得更加集中，大企业有能力存活下去，而更多小企业倒闭或者被吞并，导致市场竞争被进一步削弱、垄断的可能性更明显。";
        try {
            byte[] bytes = str.getBytes("utf-8");
            String ss = Base64.getEncoder().encodeToString(bytes);
            System.out.println(ss);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    @Test
    void deleteIndex(){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );
        try {
            //删除多个索引以逗号分割
            DeleteIndexRequest request = new DeleteIndexRequest("ik_index");
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(request, RequestOptions.DEFAULT);
            log.info("deleteIndexResponse.isAcknowledged():{}",deleteIndexResponse.isAcknowledged());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Test
    void createIndex(){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );

        CreateIndexRequest request = new CreateIndexRequest("ik_index");
        request.source("{\n" +
                "  \"settings\": {\n" +
                "    \"analysis\": {\n" +
                "      \"analyzer\": {\n" +
                "        \"default\": {\n" +
                "          \"type\": \"ik_max_word\"\n" +
                "        },\n" +
                "        \"default_search\": {\n" +
                "          \"type\": \"ik_smart\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "    \"mappings\" : {\n" +
                "        \"properties\" : {\n" +
                "            \"attachment.content\" : {\n" +
                "                \"type\": \"text\",\n" +
                "                  \"analyzer\": \"ik_max_word\",\n" +
                "                  \"term_vector\" : \"with_positions_offsets\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}",XContentType.JSON);
        try {
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            log.info("createIndexResponse.isAcknowledged():{}",createIndexResponse.isAcknowledged());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    void createPipeline(){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );

        String source =
                "{\"description\":\"my set of processors\"," +
                        "\"processors\":[{\"attachment\":{\"field\":\"data\",\"indexed_chars\":\"-1\"}}]}";
        PutPipelineRequest request2 = new PutPipelineRequest(
                "pipeId",
                new BytesArray(source.getBytes(StandardCharsets.UTF_8)),
                XContentType.JSON
        );
        try {
            AcknowledgedResponse response = client.ingest().putPipeline(request2, RequestOptions.DEFAULT);
            log.info("管道创建接口执行完毕，{}", response.isAcknowledged());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Test
    void indexStrContents(){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );
        String str = "我担心的是，奥运相关的行业会变得更加集中，大企业有能力存活下去，而更多小企业倒闭或者被吞并，导致市场竞争被进一步削弱、垄断的可能性更明显。";
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("data", Base64.getEncoder().encodeToString(str.getBytes("utf-8")));
            jsonMap.put("title","test.pdf");
            jsonMap.put("url","d:/test.pdf");
            IndexRequest indexRequest = new IndexRequest("ik_index")
                    .setPipeline("attachment")
                    .source(jsonMap);
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            log.info("indexResponse.getIndex():{}  result:{}", indexResponse.getIndex(), indexResponse.getResult());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Test
    void testFiles(){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );
        String startPath = "F:\\小说";
        try {
            Files.walkFileTree(Paths.get(startPath), new FileVisitor<Path>(){

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    File f = file.toFile();
                    String fileName = f.getName();
                    if(fileName.endsWith(".txt")){
                        FileInputStream fis = new FileInputStream(f);
                        byte[] bytes = new byte[fis.available()];
                        fis.read(bytes);
                        try {
                            Map<String, String> jsonMap = new HashMap<>();
                            jsonMap.put("data", Base64.getEncoder().encodeToString(bytes));
                            jsonMap.put("title",f.getName());
                            jsonMap.put("url",f.getAbsolutePath());
                            IndexRequest indexRequest = new IndexRequest("ik_index")
                                    .setPipeline("attachment")
                                    .source(jsonMap);
                            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                            log.info("indexResponse.getIndex():{}  result:{}", indexResponse.getIndex(), indexResponse.getResult());
                            fis.close();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    void testSearch(){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );
        SearchRequest searchRequest = new SearchRequest("ik_index");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("attachment.content","奥运");
        searchSourceBuilder.query(matchQueryBuilder);
        try {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field("attachment.content");

            highlightTitle.highlighterType("fvh");
            highlightTitle.fragmentSize(60);
            highlightTitle.noMatchSize(50);
            highlightBuilder.preTags("<b>").postTags("</b>");
            highlightBuilder.field(highlightTitle);

//            highlightBuilder.field(new HighlightBuilder.Field("title"));
//            highlightBuilder.field(new HighlightBuilder.Field("url"));

            searchSourceBuilder.highlighter(highlightBuilder);
            SearchResponse searchResponse = client.search(searchRequest.source(searchSourceBuilder), RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits.getHits()) {
                Map map = hit.getSourceAsMap();
                log.info("map.get(\"title\"){}",map.get("title"));
                log.info("map.get(\"url\"){}",map.get("url"));
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlight = highlightFields.get("attachment.content");
                Text[] fragments = highlight.fragments();
                String fragmentString = fragments[0].string();
                log.info("fragmentString[{}]",fragmentString);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Test
    void testIndex(){
        //连接elasticsearch  http://localhost:9200/
        RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                new HttpHost("localhost", 9200, "http")
            )
        );
//创建索引  用ik智能分词
        CreateIndexRequest request = new CreateIndexRequest("ik_index");
        request.source("{\n" +
                "  \"settings\": {\n" +
                "    \"analysis\": {\n" +
                "      \"analyzer\": {\n" +
                "        \"default\": {\n" +
                "          \"type\": \"ik_smart\"\n" +
                "        },\n" +
                "        \"default_search\": {\n" +
                "          \"type\": \"ik_smart\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}",XContentType.JSON);
        try {
//            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
//            log.info("createIndexResponse.isAcknowledged():{}",createIndexResponse.isAcknowledged());

//文件索引
//            String base64 = "5Yid5pyf55qE55+l5LmO77yM5piv55Sx6auY57qn55+l6K+G5YiG5a2Q77yM5ZCE6KGM5Lia57K+6Iux57uE5oiQ55qE5bCP56S+5Yy644CC6L+Z6YeM5bCx5YOP5LiA5Liq56S+5Lya5rKZ55uY77yM5q+P5LiA5Liq6Zeu6aKY6YO95YWB6K645pyJ5LiN5ZCM55qE56uL5Zy65ZKM6Kej6K+744CC5LuK5aSp5LiA5YiH55qE56aB5b+M77yM5Zyo5b2T5pe26YO95piv5Y+v5Lul6LCI55qE44CC5LqO5piv77yM6YKj5pe25Lqn5Ye65LqG5bqe5aSn55qE5YWs55+l576k5L2TO+S5n+S6p+WHuuS6huKAnOefpeS5juS5i+WLuiLov5nnp43nsr7lppnnu53kvKbnmoTohJHmtJ475Y2z5L2/5piv5pmu5LiW5Lu35YC85Li65Li75L2T55qE6K665Z2b5rCb5Zu05LiL77yM5Lmf6IO95a655b6X5LiL546E5aSE5ZKM6ams5YmN5Y2S6L+Z5qC355qE6LWE5rex5LqU5q+b44CC55Sa6Iez6YeN55S36L275aWz55qE6K+d6aKY5LiL77yM6YKj5Liq57K+56Gu5o+P6L+w5Ye65Yac5p2R55Sf5oCB6YeM55S35oCn5a+55a625bqt5b+F6KaB5oCn55qE562U5qGI77yM5Lmf6IO96I635b6X5pWw5LiH55qE6auY6LWe44CC5q2j5piv5b2T5bm055qE55m+5a625LqJ6bij55m+6Iqx6b2Q5pS+77yM5omN5oiQ5bCx5LqG5ZCO5p2l55qE55+l5LmO44CC";
//            Map<String,Object> jsonMap = new HashMap<>();
//            jsonMap.put("data", base64);
//            IndexRequest indexRequest = new IndexRequest("ik_index")
//                    .setPipeline("attachment")
//                    .source(jsonMap);
//            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
//            log.info("indexResponse.getIndex():{}  result:{}",indexResponse.getIndex() ,indexResponse.getResult());

//进行查询
//            SearchRequest searchRequest = new SearchRequest("ik_index");
//            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//            QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("attachment.content","精英知识分子");
//            searchSourceBuilder.query(matchQueryBuilder);
//            SearchResponse searchResponse = client.search(searchRequest.source(searchSourceBuilder), RequestOptions.DEFAULT);
//
//            SearchHits hits = searchResponse.getHits();
//            for (SearchHit hit : hits.getHits()) {
//                String sourceAsString = hit.getSourceAsString();
//                System.out.println(sourceAsString);
//
//                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
//                Map<String, Object> map =  (Map<String, Object>)sourceAsMap.get("attachment");
//                String content = (String)map.get("content");
//                log.info("content:{}",content);
//            }

            SearchRequest searchRequest = new SearchRequest("ik_index");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("attachment.content","精英知识分子");
            searchSourceBuilder.query(matchQueryBuilder);

            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field("attachment.content");
            highlightBuilder.preTags("<b>").postTags("</b>");
            highlightBuilder.field(highlightTitle);
            searchSourceBuilder.highlighter(highlightBuilder);
            SearchResponse searchResponse = client.search(searchRequest.source(searchSourceBuilder), RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits.getHits()) {
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlight = highlightFields.get("attachment.content");
                Text[] fragments = highlight.fragments();
                String fragmentString = fragments[0].string();
                System.out.println(fragmentString);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    void contextLoads() {
        //连接elasticsearch  http://localhost:9200/
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                ));

        try {
            //进行索引并获取应答
            IndexRequest request = new IndexRequest("posts");
            request.id("1");
            String jsonString = "{" +
                    "\"user\":\"kimchy\"," +
                    "\"postDate\":\"2013-01-30\"," +
                    "\"message\":\"trying out Elasticsearch\"" +
                    "}";
            request.source(jsonString, XContentType.JSON);
            IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);//进行索引
            String index = indexResponse.getIndex();
            String id = indexResponse.getId();
            System.out.println(index+"   id:"+id+"  indexResponse.getResult():"+indexResponse.getResult());
            if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {

            } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {

            }
            ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
            if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

            }
            if (shardInfo.getFailed() > 0) {
                for (ReplicationResponse.ShardInfo.Failure failure :
                        shardInfo.getFailures()) {
                    String reason = failure.reason();
                    System.out.println(reason);
                }
            }



            //管道创建接口执行
            String source =
                    "{\"description\":\"my set of processors\"," +
                            "\"processors\":[{\"attachment\":{\"field\":\"data\",\"indexed_chars\":\"-1\"}}]}";
            PutPipelineRequest request2 = new PutPipelineRequest(
                    "pipeId",
                    new BytesArray(source.getBytes(StandardCharsets.UTF_8)),
                    XContentType.JSON
            );
            AcknowledgedResponse response = client.ingest().putPipeline(request2, RequestOptions.DEFAULT);
            log.info("管道创建接口执行完毕，{}", response.isAcknowledged());


            //对文件进行索引
            File f = new File("D:\\java_deep_learn\\oracle\\Oracle\\ORACLE数据库DBA管理手册\\020.PDF");
            FileInputStream fis = new FileInputStream(f);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            Base64.Encoder encoder = Base64.getEncoder();
            String base64 = encoder.encodeToString(bytes);
            Map<String,Object> jsonMap = new HashMap<>();
            jsonMap.put("data", base64);
            request = new IndexRequest("yltest")
                    .setPipeline("attachment")
                    .source(jsonMap);
            indexResponse = client.index(request, RequestOptions.DEFAULT);
            log.info("indexResponse.getIndex():{}",indexResponse.getIndex());


            //获取管道接口配置
            GetPipelineRequest request3 = new GetPipelineRequest("pipeId");
            GetPipelineResponse response2 = client.ingest().getPipeline(request3, RequestOptions.DEFAULT);
            boolean successful = response2.isFound();
            log.info("successful:"+successful);
            List<PipelineConfiguration> pipelines = response2.pipelines();
            for(PipelineConfiguration pipeline: pipelines) {
                Map<String, Object> config = pipeline.getConfigAsMap();
                log.info("config.toString():{}",config.toString());
            }


            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("================================");
    }

}
