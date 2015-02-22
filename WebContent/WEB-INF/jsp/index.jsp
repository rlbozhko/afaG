<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<form method="get">
		<p>
			<input type="text" name="url" required
				style="width: 1000px; height: 25px;">
		</p>
		<p>
			<select name="stars" required>
				<option value="">Select stars</option>
				<option>0</option>
				<option>1</option>
				<option>2</option>
				<option>3</option>
				<option>4</option>
				<option>5</option>
				<option value=999>ALL</option>
			</select> <select name="language" required>
				<option value="">Select language</option>
				<option>ru</option>
				<option>en</option>
				<option>ALL</option>
			</select>
		</p>
		<p>
			<input type="submit" value="Analyze">
		</p>
	</form>

	<table border="1">
		<c:forEach var="each" items="${feedbacksList}">
			<tr>
				<td>${each.getStars()}</td>
				<td>${each.getLanguage()}</td>
				<td>${each.getCountry()}</td>
				<td>${each.getText()}</td>
			</tr>
		</c:forEach>
	</table>

</body>
</html>