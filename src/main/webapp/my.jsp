<%@ page import="top.integer.framework.core.ioc.Context" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="top.integer.framework.core.ioc.AnnotationContext" %><%--
  Created by IntelliJ IDEA.
  User: singx
  Date: 2022/11/23
  Time: 20:30
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<%
    AnnotationContext context = (AnnotationContext) config.getServletContext().getAttribute("ioc");
    DataSource datasource = context.getBean(DataSource.class);
%>
<%=datasource.getConnection()%>
${key}
</body>
</html>
