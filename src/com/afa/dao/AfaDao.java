package com.afa.dao;

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

import com.afa.entities.Feedback;

public class AfaDao {

	private static final int THREE_DAYS_IN_MILLISECONDS = 3 * 24 * 60 * 60
			* 1000;

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

		boolean isCleared = clearOldFeedbacks(sql, itemId, scanDate);
		if (isCleared) {
			return Collections.emptyList();
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

	private static boolean clearOldFeedbacks(String sql, long itemId,
			long scanDate) {
		try (Connection connection = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/afa", "root", "123456");
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql + " LIMIT 1");) {

			if (!resultSet.next()) {
				return true;
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
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

}
