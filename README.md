环境
Elasticsearch 7.6.0；
ingest-attachment-7.6.0；

在线安装
进入elasticsearch目录，执行下面的命令进行安装：
elasticsearch-plugin install ingest-attachment
安装完需重启各节点。
离线安装
先下载离线安装文件：https://artifacts.elastic.co/downloads/elasticsearch-plugins/ingest-attachment/ingest-attachment-7.6.0.zip. ，然后执行安装：
elasticsearch-plugin.bat install file:///F:/ruanjian/ingest-attachment-7.6.0.zip


文件转为base64
	File f = new File("d:\\solr-word.pdf");
	try {
		FileInputStream fis = new FileInputStream(f);
		byte[] bytes = new byte[fis.available()];
		fis.read(bytes);
		Encoder encoder = Base64.getEncoder();
        String base64 = encoder.encodeToString(bytes);
		System.out.println(base64);
	} catch (Exception e) {
	}



posting高亮方式的特点：
1）速度快，不需要对高亮的文档再分析。文档越大，获得越高 性能 。
2）比fvh高亮方式需要的磁盘空间少。
3）将text文件分割成语句并对其高亮处理。对于自然语言发挥作用明显，但对于html则不然。
4）将文档视为整个语料库，并 使用BM25算法 为该语料库中的文档打分。

fvh高亮方式的特点如下：
1）当文件>1MB(大文件）时候，尤其适合fvh高亮方式。
2）自定义为 boundary_scanner的扫描方式。
3) 设定了 term_vector to with_positions_offsets会增加索引的大小。
4）能联合多字段匹配返回一个结果，详见matched_fields。
5）对于不同的匹配类型分配不同的权重，如：pharse匹配比term匹配高。

创建索引，默认分词为ik_max_word  默认搜索分词为ik_smart ,高亮模式为fast-vector-highlighter 简称fvh高亮方式

//fast-vector-highlighter    "term_vector" : "with_positions_offsets"      
//postings-highlighter  "index_options" : "offsets"  
PUT ik_index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "default": {
          "type": "ik_max_word"
        },
        "default_search": {
          "type": "ik_smart"
        }
      }
    }
  },
    "mappings" : {
        "properties" : {
            "attachment.content" : {
                "type": "text",
                  "analyzer": "ik_max_word",
                  "term_vector" : "with_positions_offsets"
            }
        }
    }
}

//plain, postings ， fvh 分别对应高亮显示的三种类型
//fragment_size: 设置要显示出来的fragment文本判断的长度，默认是100
//number_of_fragments：高亮的fragment文本片段显示指定的片段
//将高亮显示旁边的文字也显示出来的字数

GET /_search
{
    "query": {
        "match": {
            "attachment.content": {
                "query": "源泉"
                
            }
        }
    },
    "highlight": {
        "pre_tags": [
            "<b>"
        ],
        "post_tags": [
            "</b>"
        ],
        "fields": {
            "attachment.content": {
                "fragment_size": 60,
                "type": "fvh",
                "no_match_size": 50
            }
        }
    }
}


对应Java代码

RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                )
        );
        SearchRequest searchRequest = new SearchRequest("ik_index");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("attachment.content","源泉");
        searchSourceBuilder.query(matchQueryBuilder);
        try {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field("attachment.content");
            highlightTitle.highlighterType("fvh");
            highlightTitle.fragmentSize(60);
            highlightTitle.noMatchSize(50);
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
                log.info("fragmentString[{}]",fragmentString);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

索引pdf，doc等文档，先用Java代码将文件转换为base64文字流
PUT localhost:9200/_ingest/pipeline/attachment
{
  "description" : "Extract attachment information",
  "processors" : [
    {
      "attachment" : {
        "field" : "data"
      }
    }
  ]
}
PUT books/_doc/my_id?pipeline=attachment
{
  "data": "e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0="
}
GET books/_doc/my_id

搜索
GET localhost:9200/_search
{"query":{"bool":{"must":[],"must_not":[],"should":[{"match":{"email":"books"}}]}},"from":0,"size":10,"sort":[],"aggs":{}}





创建管道
PUT _ingest/pipeline/attachment
{
  "description" : "Extract attachment information",
  "processors" : [
    {
      "attachment" : {
        "field" : "data"
      }
    }
  ]
}

对应Java代码
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
            AcknowledgedResponse response = client.ingest().putPipeline(request2, RequestOptions.DEFAULT);
            log.info("管道创建接口执行完毕，{}", response.isAcknowledged());

删除索引
//删除多个索引以逗号分割
            DeleteIndexRequest request = new DeleteIndexRequest("posts,pdf,goods");
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(request, RequestOptions.DEFAULT);
            log.info("deleteIndexResponse.isAcknowledged():{}",deleteIndexResponse.isAcknowledged());



创建索引，以ik_smart进行分词
PUT my_index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "default": {
          "type": "ik_max_word"
        },
        "default_search": {
          "type": "ik_smart"
        }
      }
    }
  }
}

对应Java代码

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
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            log.info("createIndexResponse.isAcknowledged():{}",createIndexResponse.isAcknowledged());
        } catch (IOException e) {
            e.printStackTrace();
        }



转变成base64

String str = "初期的知乎，是由高级知识分子，各行业精英组成的小社区。这里就像一个社会沙盘，每一个问题都允许有不同的立场和解读。今天一切的禁忌，在当时都是可以谈的。于是，那时产出了庞大的公知群体;也产出了“知乎之勺\"这种精妙绝伦的脑洞;即使是普世价值为主体的论坛氛围下，也能容得下玄处和马前卒这样的资深五毛。甚至重男轻女的话题下，那个精确描述出农村生态里男性对家庭必要性的答案，也能获得数万的高赞。正是当年的百家争鸣百花齐放，才成就了后来的知乎。";
		byte[] bytes = str.getBytes();
		Base64.Encoder encoder = Base64.getEncoder();
        String base64 = encoder.encodeToString(bytes);
        System.out.println(base64);


进行索引

PUT my_index/_doc/my_id?pipeline=attachment
{
  "data": "5Yid5pyf55qE55+l5LmO77yM5piv55Sx6auY57qn55+l6K+G5YiG5a2Q77yM5ZCE6KGM5Lia57K+6Iux57uE5oiQ55qE5bCP56S+5Yy644CC6L+Z6YeM5bCx5YOP5LiA5Liq56S+5Lya5rKZ55uY77yM5q+P5LiA5Liq6Zeu6aKY6YO95YWB6K645pyJ5LiN5ZCM55qE56uL5Zy65ZKM6Kej6K+744CC5LuK5aSp5LiA5YiH55qE56aB5b+M77yM5Zyo5b2T5pe26YO95piv5Y+v5Lul6LCI55qE44CC5LqO5piv77yM6YKj5pe25Lqn5Ye65LqG5bqe5aSn55qE5YWs55+l576k5L2TO+S5n+S6p+WHuuS6huKAnOefpeS5juS5i+WLuiLov5nnp43nsr7lppnnu53kvKbnmoTohJHmtJ475Y2z5L2/5piv5pmu5LiW5Lu35YC85Li65Li75L2T55qE6K665Z2b5rCb5Zu05LiL77yM5Lmf6IO95a655b6X5LiL546E5aSE5ZKM6ams5YmN5Y2S6L+Z5qC355qE6LWE5rex5LqU5q+b44CC55Sa6Iez6YeN55S36L275aWz55qE6K+d6aKY5LiL77yM6YKj5Liq57K+56Gu5o+P6L+w5Ye65Yac5p2R55Sf5oCB6YeM55S35oCn5a+55a625bqt5b+F6KaB5oCn55qE562U5qGI77yM5Lmf6IO96I635b6X5pWw5LiH55qE6auY6LWe44CC5q2j5piv5b2T5bm055qE55m+5a625LqJ6bij55m+6Iqx6b2Q5pS+77yM5omN5oiQ5bCx5LqG5ZCO5p2l55qE55+l5LmO44CC"
}

GET my_index/_doc/my_id

对应Java代码

String base64 = "5Yid5pyf55qE55+l5LmO77yM5piv55Sx6auY57qn55+l6K+G5YiG5a2Q77yM5ZCE6KGM5Lia57K+6Iux57uE5oiQ55qE5bCP56S+5Yy644CC6L+Z6YeM5bCx5YOP5LiA5Liq56S+5Lya5rKZ55uY77yM5q+P5LiA5Liq6Zeu6aKY6YO95YWB6K645pyJ5LiN5ZCM55qE56uL5Zy65ZKM6Kej6K+744CC5LuK5aSp5LiA5YiH55qE56aB5b+M77yM5Zyo5b2T5pe26YO95piv5Y+v5Lul6LCI55qE44CC5LqO5piv77yM6YKj5pe25Lqn5Ye65LqG5bqe5aSn55qE5YWs55+l576k5L2TO+S5n+S6p+WHuuS6huKAnOefpeS5juS5i+WLuiLov5nnp43nsr7lppnnu53kvKbnmoTohJHmtJ475Y2z5L2/5piv5pmu5LiW5Lu35YC85Li65Li75L2T55qE6K665Z2b5rCb5Zu05LiL77yM5Lmf6IO95a655b6X5LiL546E5aSE5ZKM6ams5YmN5Y2S6L+Z5qC355qE6LWE5rex5LqU5q+b44CC55Sa6Iez6YeN55S36L275aWz55qE6K+d6aKY5LiL77yM6YKj5Liq57K+56Gu5o+P6L+w5Ye65Yac5p2R55Sf5oCB6YeM55S35oCn5a+55a625bqt5b+F6KaB5oCn55qE562U5qGI77yM5Lmf6IO96I635b6X5pWw5LiH55qE6auY6LWe44CC5q2j5piv5b2T5bm055qE55m+5a625LqJ6bij55m+6Iqx6b2Q5pS+77yM5omN5oiQ5bCx5LqG5ZCO5p2l55qE55+l5LmO44CC";
            Map<String,Object> jsonMap = new HashMap<>();
            jsonMap.put("data", base64);
            IndexRequest indexRequest = new IndexRequest("ik_index")
                    .setPipeline("attachment")
                    .source(jsonMap);
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            log.info("indexResponse.getIndex():{}  result:{}",indexResponse.getIndex() ,indexResponse.getResult());

查询

GET my_index/_search
{"query":{"bool":{"must":[{"match":{"attachment.content":"精英知识分子"}}],"must_not":[],"should":[]}},"from":0,"size":10,"sort":[],"aggs":{}}

对应Java代码

	SearchRequest searchRequest = new SearchRequest("ik_index");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("attachment.content","精英知识分子");
            searchSourceBuilder.query(matchQueryBuilder);
            SearchResponse searchResponse = client.search(searchRequest.source(searchSourceBuilder), RequestOptions.DEFAULT);

            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                System.out.println(sourceAsString);

                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Map<String, Object> map =  (Map<String, Object>)sourceAsMap.get("attachment");
                String content = (String)map.get("content");
                log.info("content:{}",content);
            }

高亮显示

GET my_index/_search
{
    "query": {
        "match": {
            "attachment.content": {
                "query": "精英知识分子"
            }
        }
    },
    "highlight": {
        "pre_tags": [
            "<b>"
        ],
        "post_tags": [
            "</b>"
        ],
        "fields": {
            "attachment.content": {}
        }
    }
}

对应Java代码

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
