package com.afa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import com.afa.entities.Feedback;

@Repository
public class AfaDao {

	private static final int THREE_DAYS_IN_MILLISECONDS = 3 * 24 * 60 * 60
			* 1000;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void cacheFeedbacksList(List<Feedback> feedbacksList) {
		String sql = "INSERT INTO feedbacks (item_id, language, scan_date, country, stars, text) VALUES ";

		for (int i = 0; i < feedbacksList.size(); i++) {
			sql = sql + "(?, ?, ?, ?, ?, ?), ";
		}
		sql = sql.substring(0, sql.length() - 2);

		jdbcTemplate.execute(sql, new PreparedStatementCallback<Boolean>() {
			@Override
			public Boolean doInPreparedStatement(PreparedStatement ps)
					throws SQLException, DataAccessException {
				for (int i = 0; i < feedbacksList.size(); i++) {
					Feedback feedback = feedbacksList.get(i);
					int j = i * 6 + 1;

					ps.setLong(j, feedback.getItemId());
					ps.setString(j + 1, feedback.getLanguage() == null ? ""
							: feedback.getLanguage());
					ps.setLong(j + 2, feedback.getScanDate());
					ps.setString(j + 3, feedback.getCountry() == null ? ""
							: feedback.getCountry());

					if (feedback.getStars() == null) {
						ps.setNull(j + 4, Types.NULL);
					} else {
						ps.setInt(j + 4, feedback.getStars());
					}

					ps.setString(j + 5, feedback.getText() == null ? ""
							: feedback.getText());
				}

				return null;
			}
		});
	}

	public List<Feedback> getCachedFeedbacksList(long itemId, long scanDate) {
		String sql = "SELECT * FROM feedbacks WHERE item_id = " + itemId;
		List<Feedback> feedbacksList = new ArrayList<>();

		boolean isCleared = clearOldFeedbacks(itemId, scanDate);
		if (isCleared) {
			return Collections.emptyList();
		}

		jdbcTemplate.query(sql, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				Feedback feedback = new Feedback();

				long id = rs.getLong("id");
				feedback.setId(id);

				long itemId2 = rs.getLong("item_id");
				feedback.setItemId(itemId2);

				String language = rs.getString("language");
				feedback.setLanguage(language);

				String country = rs.getString("country");
				feedback.setCountry(country);

				int stars = rs.getInt("stars");
				feedback.setStars(stars);

				String text = rs.getString("text");
				feedback.setText(text);

				feedbacksList.add(feedback);
			}
		});

		return feedbacksList;
	}

	private boolean clearOldFeedbacks(long itemId, long scanDate) {
		String sql = "SELECT scan_date FROM feedbacks WHERE item_id = "
				+ itemId + " LIMIT 1";

		Long scanDateSQL = null;
		try {
			scanDateSQL = jdbcTemplate.queryForObject(sql, Long.class);
		} catch (EmptyResultDataAccessException e) {
			return true;
		}

		// ставить мин 100 секунд с учетом, что сам запрос может секунд
		// 40-50
		// выполнятся с учетом задержек при выкачке из инета
		// 24*60*60*1000 = 1 день

		if ((scanDate - scanDateSQL) > THREE_DAYS_IN_MILLISECONDS) {
			String sqlDelete = "DELETE FROM feedbacks WHERE item_id = "
					+ itemId;
			jdbcTemplate.execute(sqlDelete);
			return true;
		}

		return false;
	}
}
