package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RestHandler {

	public void executeSearch(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;
}
