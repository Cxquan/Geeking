<%@ page language="java" import="java.util.*"  pageEncoding="utf-8"%>
<jsp:directive.page import="org.geek.geeksearch.queryer.Response" />
<jsp:directive.page import="org.geek.geeksearch.queryer.Result" />

<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<base href="<%=basePath%>">

<title>Search Result</title>

<style type="text/css">
#search {
	text-align: center;
	position: relative;
	width: 78px;
	height: 28px;
	font: 14px "宋体"
}

.autocomplete {
	border: 1px solid #9ACCFB;
	background-color: white;
	text-align: left;
}

.autocomplete li {
	list-style-type: none;
}

.clickable {
	cursor: default;
}

.highlight {
	background-color: #9ACCFB;
}

#textArea {
	width: 300px;
	height: 30px;
	font: 14px "宋体"
}
</style>
<script type="text/javascript" src="jquery.js"></script>
<script type="text/javascript" src="auto_complete.js"></script>
<script type="text/javascript" src="jquery.min.js"></script>
<script type="text/javascript" src="jquery.pagination.js"></script>
<script type="text/javascript">
$(function(){
	var pageSize = 3;
	var pageIndex = 0;
//	InitTable(0)
	//此demo通过Ajax加载分页元素		
		var num_entries = 10;
		// 创建分页
		$("#Pagination").pagination(num_entries, {
			num_edge_entries: 1, //边缘页数
			num_display_entries: 4, //主体页数
			callback: pageselectCallback,
			items_per_page: 3, //每页显示1项
			prev_text: "前一页",
			next_text: "后一页"
		});

	 
	function pageselectCallback(page_index, jq){
		$("#Searchresult").empty();
		
		InitTable(page_index);
		return false;
	}
    function InitTable(pageIndex) {
    	var keyword=$("#search-text").val();
//    	alert(keyword);
    	if(keyword=="")
    		alert("请输入query");
    	else{
 //   		contentType: “application/x-www-form-urlencoded; charset=UTF-8″    		
    		$.ajax({
    			'url' : '/GeekSearch/search_result_server.jsp', // 服务器的地址
    			'data' : {'search-text':encodeURI(keyword),'pageIndex':pageIndex, 'pageSize':pageSize}, // 参数
    			'dataType' : 'json', // 返回数据类型
    			'type' : 'POST', // 请求类型
    			'success' : function(data) {
//    				alert(data.results);
//    				if (data.length) {
    					// 遍历data，添加到自动完成区					
    					$.each(data.results, function(index, term) {
    						// 创建li标签,添加到下拉列表中
 //   						alert(term.title);
    						$("#Searchresult").append(
    								"<h2><a href="+term.url+">"+term.title+"</a></h2>"
    								);
    						$("#Searchresult").append("<p>"+term.content+"</p>");
    						
    						$("#Searchresult").append("网页来源: "+term.url+"   时间: "+term.date+"    ");
    						$("#Searchresult").append("<a href='RawPages4Test\163\test.html'>快照</a>")
    						
    					});// 事件注册完毕
    					if(data.recommend_words.length){
    						$.each(data.recommend_words,function(index,term){
    							var html="<a href='search.jsp?search-text="+term+"'>"+term+"</a>";
    							$("#recommend_words").append(html);
    						});
    					
    					}
//    				}
    			}
    		});    		
    	}    	
    }
	//ajax加载
});
</script>
</head>

<body>

	<%
		String keyword = new String(request.getParameter("search-text")
				.getBytes("ISO-8859-1"), "utf-8");
	%>
	<form id="search" action="search.jsp" method="get">
		<input name="search-text" type="text" maxlength="100" id="search-text" value=<%=keyword%>> 
		<input type="submit" value="搜索一下"id="submit">
	</form>

	<div id="recommend_words"><p>推荐词：</p></div>
	
	<div id="Searchresult"></div>
	<div id="Pagination" class="pagination"><!-- 这里显示分页 --></div>
</body>
</html>