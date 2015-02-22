package com.afa.dao;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.afa.entities.Feedback;
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

public class AfaDao {

	private static final int THREE_DAYS_IN_MILLISECONDS = 3 * 24 * 60 * 60
			* 1000;
	private static int countDetectorFactory = 0;

	// парсинг отзывов
	public static List<Feedback> getFeedbacksList(String url) {

		// если нам передали неверный url
		if (url.lastIndexOf("aliexpress.com/") == -1) {
			return new ArrayList<>();
		}

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

		// Для вопросов кэширования запомнили текущую дату
		long scanDate = System.currentTimeMillis();

		// если данные из кэша получены то их и возвращаем
		List<Feedback> feedbacksList = getCachedFeedbacksList(itemId, scanDate);
		if (!feedbacksList.isEmpty()) {
			return feedbacksList;
		}

		// если данные из кэша не получены то вытягиваем их из интернета
		feedbacksList = new ArrayList<>();

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
			html = getPageHtml(urlFeedback);
			// задержка чтоб на али не сработала защита от роботов, 200,100
			// перестали работать используй 1000

			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int countryFirstIndex = 0;

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
				int starFirstIndex = html.indexOf(starFirstText,
						countryLastIndex) + starFirstText.length();
				int starLastIndex = html.indexOf(starLastText, starFirstIndex);
				String starsText = html
						.substring(starFirstIndex, starLastIndex);
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
				String text = html.substring(feedbackFirstIndex,
						feedbackLastIndex);
				feedback.setText(text);

				feedbacksList.add(feedback);
			}

			pageIndex = pageIndex + 1;
		} while (html.length() > 300);

		// записываем в БД отобранные записи если они есть
		if (!feedbacksList.isEmpty()) {
			// Так можно узнать какая директория текущая
			// System.out.println(new File(".").getAbsolutePath());
			if (countDetectorFactory == 0) {
				try {
					DetectorFactory.clear();
					DetectorFactory
							.loadProfile("C:\\Java\\workspace1\\.metadata\\.plugins\\org.eclipse.wst.server.core\\tmp0\\wtpwebapps\\AFA\\WEB-INF\\lib\\profiles");
					countDetectorFactory = countDetectorFactory + 1;
					// System.out.println("countDetectorFactory = "
					// + countDetectorFactory);
				} catch (LangDetectException e) {
					e.printStackTrace();
				}
			}
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

			cacheFeedbacksList(feedbacksList);
		}

		return feedbacksList;
	}

	public static void cacheFeedbacksList(List<Feedback> feedbacksList) {
		String sql = "INSERT INTO feedbacks (item_id, language, scan_date, country, stars, text) VALUES ";

		for (int i = 0; i < feedbacksList.size(); i++) {
			sql = sql + "(?, ?, ?, ?, ?, ?), ";
		}
		sql = sql.substring(0, sql.length() - 2);

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try (Connection connection = DriverManager
				.getConnection(
						"jdbc:mysql://localhost:3306/afa?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8",
						"root", "123456");
				PreparedStatement preparedStatement = connection
						.prepareStatement(sql);) {

			for (int i = 0; i < feedbacksList.size(); i++) {
				Feedback feedback = feedbacksList.get(i);
				int j = i * 6 + 1;

				preparedStatement.setLong(j, feedback.getItemId());
				preparedStatement.setString(
						j + 1,
						feedback.getLanguage() == null ? "" : feedback
								.getLanguage());
				preparedStatement.setLong(j + 2, feedback.getScanDate());
				preparedStatement.setString(
						j + 3,
						feedback.getCountry() == null ? "" : feedback
								.getCountry());

				if (feedback.getStars() == null) {
					preparedStatement.setNull(j + 4, Types.NULL);
				} else {
					preparedStatement.setInt(j + 4, feedback.getStars());
				}

				preparedStatement.setString(j + 5,
						feedback.getText() == null ? "" : feedback.getText());
			}

			preparedStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static List<Feedback> getCachedFeedbacksList(long itemId,
			long scanDate) {
		String sql = "SELECT * FROM feedbacks WHERE item_id = " + itemId;
		List<Feedback> feedbacksList = new ArrayList<>();

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try (Connection connection = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/afa", "root", "123456");
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql + " LIMIT 1");) {

			if (!resultSet.next()) {
				return Collections.emptyList();
			}

			// ставить мин 100 секунд с учетом, что сам запрос может секунд
			// 40-50
			// выполнятся с учетом задержек при выкачке из инета
			// 24*60*60*1000 = 1 день
			long scanDateSQL = resultSet.getLong("scan_date");
			if ((scanDate - scanDateSQL) > THREE_DAYS_IN_MILLISECONDS) {
				String sqlDelete = "DELETE FROM feedbacks WHERE item_id = "
						+ itemId;
				statement.execute(sqlDelete);
				return Collections.emptyList();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		try (Connection connection = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/afa", "root", "123456");
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);) {

			while (resultSet.next()) {
				Feedback feedback = new Feedback();

				long id = resultSet.getLong("id");
				feedback.setId(id);

				long itemId2 = resultSet.getLong("item_id");
				feedback.setItemId(itemId2);

				String language = resultSet.getString("language");
				feedback.setLanguage(language);

				String country = resultSet.getString("country");
				feedback.setCountry(country);

				int stars = resultSet.getInt("stars");
				feedback.setStars(stars);

				String text = resultSet.getString("text");
				feedback.setText(text);

				feedbacksList.add(feedback);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return feedbacksList;
	}

	public static String getPageHtml(String url) {
		String html = "";
		try {
			URL siteUrl = new URL(url);
			HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl
					.openConnection();
			httpURLConnection.connect();

			InputStream inputStream = httpURLConnection.getInputStream();
			Scanner scanner = new Scanner(inputStream, "UTF-8");
			html = scanner.useDelimiter("\\A").next();
			scanner.close();
			httpURLConnection.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return html;
	}

}
