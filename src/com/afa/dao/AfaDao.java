package com.afa.dao;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.afa.entities.Feedback;

public class AfaDao {

	// ������� �������
	public static List<Feedback> getFeedbacksList(String url) {

		// ���� ��� �������� �������� url
		if (url.lastIndexOf("aliexpress.com/") == -1) {
			return new ArrayList<>();
		}

		// ������ ������ ������� ����� ���� �������� � ���������� url
		// http://www.aliexpress.com/item/Original-HUAWEI-Honor-3C-5-0-Quad-Core-Mobile-Phone-MTK6582-IPS-1280-720-1GB-RAM/1594611489.html

		// �������� ���������� ������������� ��������-��������(�� ��� ������ ���
		// �������� �� ���-�� �������� �������� ��������� ����� �������)
		// ��� ������� ��� itemId =1594611489 �� ������ ����� ������-�������
		String itemFirstText = "/";
		String itemLastText = ".html";
		int itemIdFirstIndex = url.lastIndexOf(itemFirstText);
		int itemIdLastIndex = url.lastIndexOf(itemLastText);

		String itemIdText = url.substring(
				itemIdFirstIndex + itemFirstText.length(), itemIdLastIndex);

		// ���� ��� ���� ������ ����� ������� �� ���� ������ ����� ��������
		if (itemIdText.lastIndexOf("_") > -1) {
			itemIdText = itemIdText.substring(itemIdText.lastIndexOf("_") + 1,
					itemIdText.length());
		}
		long itemId = Long.parseLong(itemIdText);

		// ��� �������� ����������� ��������� ������� ����
		long scanDate = System.currentTimeMillis();

		// ���� ������ �� ���� �������� �� �� � ����������
		List<Feedback> feedbacksList = getCachedFeedbacksList(itemId, scanDate);
		if (!feedbacksList.isEmpty()) {
			return feedbacksList;
		}

		// ���� ������ �� ���� �� �������� �� ���������� �� �� ���������
		feedbacksList = new ArrayList<>();

		// ����� (���)�������� � ��������
		int pageIndex = 1;

		// ����� ��������
		String html = "";

		do {
			// ��� ������ ����������� itemid(��.����) ������ ����������� �
			// ��������
			String urlFeedback = "http://feedback.aliexpress.com/display/evaluationProductDetailAjaxService.htm?productId="
					+ itemId + "&type=default&page=" + pageIndex;
			// � html ��������� ���� �������� � ��������
			html = getPageHtml(urlFeedback);
			// �������� ���� �� ��� �� ��������� ������ �� �������, 200 � 100
			// ����� ���� ��������
			// ���� ��������
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int countryFirstIndex = 0;

			// ������ �������� �� ��������� ������ - ���� �������� ����� = ����
			// �����
			while (true) {
				Feedback feedback = new Feedback();

				feedback.setItemId(itemId);

				feedback.setScanDate(scanDate);

				String countryFirstText = "\"countryName\":\"";
				String countryLastText = "\"";

				countryFirstIndex = html.indexOf(countryFirstText,
						countryFirstIndex + countryFirstText.length());

				// ��� ������ �� ������� �� �������� �����������
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
		// ���������� � �� ���������� ������ ���� ��� ����
		if (!feedbacksList.isEmpty()) {
			cacheFeedbacksList(feedbacksList);
		}

		return feedbacksList;
	}

	public static void cacheFeedbacksList(List<Feedback> feedbacksList) {
		String sql = "INSERT INTO feedbacks (item_id, language, scan_date, country, stars, text) VALUES ";

		for (Feedback feedback : feedbacksList) {
			sql = sql + "(" + feedback.getItemId() + ", '"
					+ feedback.getLanguage() + "', " + feedback.getScanDate()
					+ ", '" + feedback.getCountry() + "', "
					+ feedback.getStars() + ", '"
					+ feedback.getText().replace("'", "\\'") + "'), ";
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
				Statement statement = connection.createStatement();) {
			statement.execute(sql);
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
				// Collections.emptyList();
				return Collections.emptyList();
			}

			// 100 ������ � ������ ��� ��� ������ ����� ������ 40-50 ����������?

			long scanDateSQL = resultSet.getLong("scan_date");
			if ((scanDate - scanDateSQL) > (100 * 1000)) {
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
