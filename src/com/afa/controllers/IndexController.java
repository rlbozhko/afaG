package com.afa.controllers;

import com.afa.entities.Feedback;
import com.afa.service.AfaService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("serial")
@WebServlet("/index.html")
public class IndexController extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String url = request.getParameter("url");
		String starsText = request.getParameter("stars");
		String language = request.getParameter("language");

// �� ������ ������� ����� ��� ���� ����� ���� ���� required, ������� ������� ����� ������ �������� �����...
//		if (url != null) {
//			List<Feedback> feedbacksList = AfaService.getFeedbacksList(url);
//			request.setAttribute("feedbacksList", feedbacksList);
//		}
		
		if (url != null && starsText != null  && language  != null) {
			int stars = Integer.parseInt(starsText);
			List<Feedback> feedbacksList = AfaService.getFeedbacksList(url,
					stars, language);
			request.setAttribute("feedbacksList", feedbacksList);
		}
// ��� ������ ��� ���� ��� �� �� ������� ������ ��������, �������� �� ����� ������
		request.setAttribute("url", url);
		request.getRequestDispatcher("/WEB-INF/jsp/index.jsp").forward(request,
				response);
	}

}
