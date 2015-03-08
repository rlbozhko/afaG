package com.afa.service;

//Выборка по количеству звезд + парсинг отзывов	
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.afa.dao.AfaDao;
import com.afa.entities.Feedback;
import com.afa.utils.Utils;
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

public class AfaService {

	private static int countDetectorFactory = 0;

	private static void loadDetectorFactory() {
		if (countDetectorFactory == 0) {
			try {
				DetectorFactory.clear();
				DetectorFactory
						.loadProfile("C:\\Java\\workspace1\\.metadata\\.plugins\\org.eclipse.wst.server.core\\tmp0\\wtpwebapps\\AFA\\WEB-INF\\lib\\profiles");
				countDetectorFactory = countDetectorFactory + 1;

			} catch (LangDetectException e) {
				e.printStackTrace();
			}
		}
	}

	// В зависимости от количества звезд и языка выбранных на странице,
	// мы удаляем из списка все записи с не таким к-вом звезд и языком
	public static List<Feedback> getFeedbacksList(String url, int stars,
			String language) {

		List<Feedback> feedbacksList = getFeedbacksList(url);
		Iterator<Feedback> iterator = feedbacksList.iterator();

		while (iterator.hasNext()) {
			Feedback feedback = iterator.next();

			if ((stars != 999)
					&& (feedback.getStars() == null || feedback.getStars() != stars)) {
				iterator.remove();
			} else {
				if ((!language.equals("ALL"))
						&& (feedback.getLanguage() == null || !feedback
								.getLanguage().equals(language))) {
					iterator.remove();
				}

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

	public static long getItemId(String url) {
		// пример строки которая может быть передана в переменной url
		// http://www.aliexpress.com/item/Original-HUAWEI-Honor-3C-5-0-Quad-Core-Mobile-Phone-MTK6582-IPS-1280-720-1GB-RAM/1594611489.html
		// выделяем уникальный идентификатор страницы-продукта(на али иногда для
		// рейтинга на той-же странице начинают продавать новый продукт)
		// для примера это itemId =1594611489 из самого конца строки-примера
		String itemFirstText = "/";
		String itemLastText = ".html";
		int itemIdFirstIndex = url.lastIndexOf(itemFirstText);
		int itemIdLastIndex = url.lastIndexOf(itemLastText);

		String itemIdText = url.substring(
				itemIdFirstIndex + itemFirstText.length(), itemIdLastIndex);

		// если нам дали ссылку через магазин то надо убрать номер магазина
		if (itemIdText.lastIndexOf("_") > -1) {
			itemIdText = itemIdText.substring(itemIdText.lastIndexOf("_") + 1,
					itemIdText.length());
		}
		long itemId = Long.parseLong(itemIdText);
		return itemId;
	}

	// парсинг отзывов
	public static List<Feedback> getFeedbacksList(String url) {

		// если нам передали неверный url
		if (url.lastIndexOf("aliexpress.com/") == -1) {
			return Collections.emptyList();
		}

		// пример строки которая может быть передана в переменной url
		// http://www.aliexpress.com/item/Original-HUAWEI-Honor-3C-5-0-Quad-Core-Mobile-Phone-MTK6582-IPS-1280-720-1GB-RAM/1594611489.html
		long itemId = AfaService.getItemId(url);

		// Для вопросов кэширования запомнили текущую дату
		long scanDate = System.currentTimeMillis();

		// если данные из кэша получены то их и возвращаем
		List<Feedback> feedbacksList = AfaDao.getCachedFeedbacksList(itemId,
				scanDate);
		if (!feedbacksList.isEmpty()) {
			return feedbacksList;
		}

		feedbacksList = getAllFeedbacksList(itemId, scanDate);

		// записываем в БД отобранные записи если они есть
		if (!feedbacksList.isEmpty()) {
			// Так можно узнать какая директория текущая
			// System.out.println(new File(".").getAbsolutePath());
			loadDetectorFactory();
			for (Feedback each : feedbacksList) {
				if (each.getText().length() > 10) {
					try {
						Detector detector = DetectorFactory.create();
						detector.append(each.getText());

						String language = detector.detect();
						each.setLanguage(language);
					} catch (LangDetectException e) {
						e.printStackTrace();
					}
				}
			}

			AfaDao.cacheFeedbacksList(feedbacksList);
		}

		return feedbacksList;
	}

	public static List<Feedback> getAllFeedbacksList(long itemId, long scanDate) {
		// если данные из кэша не получены то вытягиваем их из интернета
		List<Feedback> feedbacksList = new ArrayList<>();

		// номер (под)страницы с отзывами
		int pageIndex = 1;

		// текст страницы
		String html = "";

		do {
			// при помощи выделенного itemid(см.выше) парсим подстраницы с
			// отзывами
			String urlFeedback = "http://feedback.aliexpress.com/display/evaluationProductDetailAjaxService.htm?productId="
					+ itemId + "&type=default&page=" + pageIndex;
			// в html загрузили ОДНУ страницу с отзывами
			html = Utils.getPageHtml(urlFeedback);
			// задержка чтоб на али не сработала защита от роботов, 200,100
			// перестали работать используй 1000

			try {
				TimeUnit.MILLISECONDS.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			List<Feedback> pageFeedbacksList = getPageFeedbacksList(itemId,
					scanDate, html);
			feedbacksList.addAll(pageFeedbacksList);

			pageIndex = pageIndex + 1;
		} while (html.length() > 300);

		return feedbacksList;
	}

	private static List<Feedback> getPageFeedbacksList(long itemId,
			long scanDate, String html) {
		int countryFirstIndex = 0;
		List<Feedback> feedbacksList = new ArrayList<>();

		// парсим страницу на отдельные отзывы - одна итерация цикла = один
		// отзыв
		while (true) {
			Feedback feedback = new Feedback();

			feedback.setItemId(itemId);

			feedback.setScanDate(scanDate);

			String countryFirstText = "\"countryName\":\"";
			String countryLastText = "\"";

			countryFirstIndex = html.indexOf(countryFirstText,
					countryFirstIndex + countryFirstText.length());

			// раз страну не находим то страница закончилась
			if (countryFirstIndex < 0) {
				break;
			}

			int countryLastIndex = html.indexOf(countryLastText,
					countryFirstIndex + countryFirstText.length());
			String countryText = html.substring(countryFirstIndex
					+ countryFirstText.length(), countryLastIndex);
			feedback.setCountry(countryText);

			String starFirstText = "\"star\":\"";
			String starLastText = "\"";
			int starFirstIndex = html.indexOf(starFirstText, countryLastIndex)
					+ starFirstText.length();
			int starLastIndex = html.indexOf(starLastText, starFirstIndex);
			String starsText = html.substring(starFirstIndex, starLastIndex);
			Integer stars = null;
			if (!starsText.isEmpty()) {
				stars = Integer.parseInt(starsText);
			}
			feedback.setStars(stars);

			String feedbackFirstText = "\"buyerFeedback\":\"";
			String feedbackLastText = "\"";

			int feedbackFirstIndex = html.indexOf(feedbackFirstText,
					starLastIndex) + feedbackFirstText.length();
			int feedbackLastIndex = html.indexOf(feedbackLastText,
					feedbackFirstIndex);
			String text = html.substring(feedbackFirstIndex, feedbackLastIndex);
			feedback.setText(text);

			feedbacksList.add(feedback);
		}

		return feedbacksList;
	}

}
