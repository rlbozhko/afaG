package com.afa.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.afa.entities.Feedback;
import com.afa.service.AfaService;

@Controller
public class IndexController {
	@Autowired
	private AfaService afaService;

	@RequestMapping(value = "/index.action", method = RequestMethod.GET)
	public ModelAndView getIndex(
			@RequestParam(value = "url", required = false) String url,
			@RequestParam(value = "stars", required = false) Integer stars,
			@RequestParam(value = "language", required = false) String language) {

		ModelAndView mav = new ModelAndView("index");

		if (url != null && stars != null && language != null) {
			List<Feedback> feedbacksList = afaService.getFeedbacksList(url,
					stars, language);
			mav.addObject("feedbacksList", feedbacksList);
		}
		// эта строка для того что бы не вводить ссылку повторно, передаем по
		// кругу ссылку
		mav.addObject("url", url);

		return mav;
	}

}