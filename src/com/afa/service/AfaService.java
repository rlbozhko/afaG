package com.afa.service;

//Выборка по количеству звезд + парсинг отзывов	
import java.util.Iterator;
import java.util.List;

import com.afa.dao.AfaDao;
import com.afa.entities.Feedback;

public class AfaService {

	// В зависимости от количества звезд выбранных на странице,
	// мы удаляем из списка все записи с не таким к-вом звезд
	public static List<Feedback> getFeedbacksList(String url, int stars) {

		List<Feedback> feedbacksList = getFeedbacksList(url);
		Iterator<Feedback> iterator = feedbacksList.iterator();

		while (iterator.hasNext()) {
			Feedback feedback = iterator.next();
			if (feedback.getStars() == null || feedback.getStars() != stars) {
				iterator.remove();
			}

		}

		// в этом варианте без использования итераторов,мы формируем
		// новый список из списка со всеми отзывами
		// List<Feedback> feedbacksList2 = new ArrayList<>();

		// for (int i = 0; i < feedbacksList.size(); i++) {
		// Feedback feedback = feedbacksList.get(i);
		// if (feedback.getStars() != null) {
		// if (feedback.getStars() == stars) {
		// feedbacksList2.add(feedback);
		// }
		// }
		// }

		return feedbacksList;
	}

	public static List<Feedback> getFeedbacksList(String url) {
		List<Feedback> feedbacksList = AfaDao.getFeedbacksList(url);
		return feedbacksList;
	}

}
