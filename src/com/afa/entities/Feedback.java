package com.afa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

//класс Отзыв - содержащий элемент данных с к-вом звезд, текстом отзыва и т.д.

@Entity
@Table(name = "feedbacks")
public class Feedback {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(name = "item_id")
	private long itemId;

	@Column(name = "language")
	private String language;
	
	@Column(name = "country")
	private String country;
	
	@Column(name = "stars")
	private Integer stars;
	
	@Column(name = "text")
	private String text;
	
	@Column(name = "scan_date")
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