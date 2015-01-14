package com.afa.entities;

//класс Отзыв - содержащий элемент данных с к-вом звезд, текстом отзыва и т.д.

public class Feedback {

	private long id;
	private long itemId;
	private String language;
	private String country;
	private Integer stars;
	private String text;
	private long scanDate;

	public long getScanDate() {
		return scanDate;
	}

	public void setScanDate(long scanDate) {
		this.scanDate = scanDate;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getItemId() {
		return itemId;
	}

	public void setItemId(long itemId) {
		this.itemId = itemId;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Integer getStars() {
		return stars;
	}

	public void setStars(Integer stars) {
		this.stars = stars;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}