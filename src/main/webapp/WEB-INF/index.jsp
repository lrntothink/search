<%--
  Created by IntelliJ IDEA.
  User: Administrator
  Date: 2020/3/26
  Time: 17:16
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>本地搜索</title>
    <style type="text/css">
        body{
            margin: 0 auto;
        }

        .box{
            border: 1px solid #999999;
            width: 600px;
            height: 400px;
            margin: 100px auto;
        }

        .box h1{
            text-align: center;
            color: #008040;
            margin: 50px auto;
        }

        .searchbox{
            height: 30px;
            border: 1px solid #008040;
            width: 80%;
            margin:50px auto ;

        }

        .searchbox input[type="text"]{
            height: 30px;
            width: 85%;
            outline: none;
            border: 0;
            font-size: 18px;
        }

        .searchbox input[type="submit"]{
            height: 30px;
            width:14% ;
            float: right;
            border: 0;
            outline: none;
            background-color: #008040;
            color: #ffffff;
        }
    </style>
</head>
<body>
<div class="box">
    <h1>本地搜索</h1>
    <div class="searchbox">
        <form action="/search" method="get">
            <input type="text" name="query">
            <input type="submit" value="搜索一下">
        </form>
    </div>
</div>
</body>
</html>
