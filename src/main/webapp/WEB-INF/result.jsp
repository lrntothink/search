<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ" %>
<%@ page import="org.yaml.snakeyaml.util.UriEncoder" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    String queryBack = (String) request.getAttribute("queryBack");
    ArrayList<Map<String, String>> newslist = (ArrayList<Map<String, String>>) request.getAttribute("newslist");
    String totalHits = (String) request.getAttribute("totalHits");
    String totalTime = (String) request.getAttribute("totalTime");
    int pages = Integer.parseInt(totalHits) / 10 + 1;
    pages = pages > 10 ? 10 : pages;
%>
<html>
<head>
    <title>搜索结果</title>
    <style type="text/css">
        body {
            margin: 0;
            padding: 0;
        }

        .result_search {
            width: 100%;
            height: 60px;
            background-color: #cccccc;
        }

        .logo {
            float: left;
        }

        .logo h2 {
            color: #008040;
            padding-left: 20px;
        }

        .logo h2 a:link,.logo h2 a:visited{
            text-decoration: none;
            color: #008040;
        }

        .searchbox {
            float: left;
            border: 1px solid #008040;
            height: 30px;
            width: 500px;
            margin-top: 15px;
            margin-left: 30px;
        }

        .searchbox input[type="text"] {
            width: 85%;
            height: 30px;
            border: 0;
            outline: none;
            font-size: 18px;
        }

        .searchbox input[type="submit"] {
            width: 15%;
            border: 0;
            outline: none;
            height: 30px;
            background-color: #008040;
            color: #ffffff;
            float: right;
        }

        .result_info {
            margin-left: 30px;
        }

        .result_info span {
            color: #ff0000;
        }

        .newslist {
            width: 700px;
        }

        .newslist h4 {
            padding: 0;
            margin: 0;
            margin-left: 20px;
        }

        .newslist h4 a:link, .newslist h4 a:visited {
            text-decoration: none;
        }

        .newslist h4 a:hover {
            text-decoration: underline;
        }

        .newslist p {
            margin-left: 30px;
            line-height: 1.5;
            font-size: 13px;
        }

        .page {
            margin-left: 20px;
            height: 30px;
        }

        .page ul li {
            list-style: none;
            float: left;
            width: 50px;
        }

        .page ul li a:link, .page ul li a:visited {
            text-decoration: none;
        }

        .info {
            width: 800px;

        }

        .info p {
            text-align: center;
            font-size: 12px;
            color: #808080;
        }
    </style>
</head>
<body>
<div class="result_search">
    <div class="logo">
        <h2><a href="/index">本地搜索</a></h2>
    </div>
    <div class="searchbox">
        <form action="/search" method="get">
            <input type="text" name="query" value="<%=queryBack%>">
            <input type="submit" value="搜索一下">
        </form>
    </div>
</div>
<h5 class="result_info">搜索到<span><%=totalHits%></span>条结果,耗时<span> <%=Double.parseDouble(totalTime) / 1000.0 %></span>秒
</h5>
<div class="newslist">
    <%
        if (newslist.size() > 0) {
            Iterator<Map<String, String>> iter = newslist.iterator();
            while (iter.hasNext()) {
                Map<String, String> news = iter.next();
                String content = news.get("content").toString();
                //content = content.length() > 200 ? content.substring(0, 200) : content;
                String url = news.get("url");
    %>
    <div class="news">
        <h4><a href="/showContent?filePath=<%=UriEncoder.encode(url)%>" target="_blank"><%=news.get("title")%>
        </a></h4>
        <p><%=content%> ...
        </p>
    </div>
    <%
            }
        }
    %>
</div>
<div class="page">
    <ul>
        <%
            for (int i = 1; i <= pages; i++) {
        %>
        <li><a href="/search?query=<%=queryBack%>&pageNum=<%=i%>"><%=i%>
        </a></li>
        <%
            }
        %>
    </ul>
</div>
<div class="info">
    <p>本地搜索项目实战 Powered By <b> yltest</b></p>
    <p>@2020 All right reserved</p>
</div>
</body>
</html>
