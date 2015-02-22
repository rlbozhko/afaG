package com.afa.controllers;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.afa.entities.Feedback;
import com.afa.service.AfaService;

@SuppressWarnings("serial")
@WebServlet("/index.html")
public class IndexController extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String url = request.getParameter("url");
		String starsText = request.getParameter("stars");
		if (url != null) {
			List<Feedback> feedbacksList = AfaService.getFeedbacksList(url);
			request.setAttribute("feedbacksList", feedbacksList);
		}////
		if (url != null && starsText != null) {
			int stars = Integer.parseInt(starsText);
			List<Feedback> feedbacksList = AfaService.getFeedbacksList(url,
					stars);
			request.setAttribute("feedbacksList", feedbacksList);
		}

		request.getRequestDispatcher("/WEB-INF/jsp/index.jsp").forward(request,
				response);
	}

}
