<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import = "net.sf.wubiq.common.WebKeys" %>
<%@ page import = "java.util.UUID" %>
<% response.setContentType("application/x-java-jnlp-file"); %>
<%
	String protocol = request.getProtocol().substring(0, request.getProtocol().indexOf("/")).toLowerCase();
	String address = request.getLocalName();
	String port = Integer.toString(request.getLocalPort()).trim();
	String context = request.getContextPath().substring(1);
	String host = protocol + "://" + address;
	
	String url = host + ":" + port + "/" + context;
	Cookie[] cookies = request.getCookies();
	Cookie uuidCookie = null;
	for (Cookie cookie : cookies) {
		if (cookie.getName().equals(WebKeys.UUID_COOKIE_NAME)) {
			uuidCookie = cookie;
		}
	}
	if (uuidCookie == null) {
		uuidCookie = new Cookie(WebKeys.UUID_COOKIE_NAME, UUID.randomUUID().toString());
	}
	uuidCookie.setMaxAge(Integer.MAX_VALUE);
	response.addCookie(uuidCookie);
	String uuid = uuidCookie.getValue();
%>
<jnlp spec="1.0+" codebase="<%=url%>">
    <information>
        <title>Wubiq - Local Print Manager</title>
        <vendor>Open Source</vendor>
		<homepage href="http://sourceforge.net/projects/wubiq" />
    </information>
    <information locale="es">
    	<title>Wubiq - Manejador de Impresión Local</title>
        <vendor>Código Libre</vendor>
    </information>
    <resources>
        <!-- Application Resources -->
        <j2se version="1.6+"
              href="http://java.sun.com/products/autodl/j2se"/>
        <jar href="wubiq-client.jar" main="true" />
    </resources>
    <application-desc
         name="Wubiq Client"
         main-class="net.sf.wubiq.clients.LocalPrintManager">
         <argument>-h <%=host%></argument>
         <argument>-p <%=port%></argument>
         <argument>-u <%=uuid%></argument>
	</application-desc>
	<update check="always" policy="always"/>
</jnlp>					