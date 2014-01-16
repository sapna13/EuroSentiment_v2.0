package eu.eurosentiment.insight;

import java.util.Date;

//For TripAdvisor data, extracts ratings corresponding to aspects
class Review {
	
	private String author;
	private String content;
	private Date date;
	private int noOfReaders;
	private int noOfHelpful;
	private int overall;
	private int value;
	private int rooms;
	private int location;
	private int cleanliness;
	private int frontDesk;
	private int service;
	private int businessService;
	
	public Review(){		
	}
		
	public Review(String author, String content, Date date, int noOfReaders, int noOfHelpful, int overall, int value, int rooms,
			int location, int cleanliness, int frontDesk, int service, int businessService){
		this.author = author;
		this.content = content;
		this.date = date;
		this.noOfReaders= noOfReaders;
		this.noOfHelpful = noOfHelpful;
		this.overall = overall;
		this.value = value;
		this.rooms = rooms;
		this.location = location;
		this.cleanliness = cleanliness;
		this.frontDesk = frontDesk;
		this.service = service;
		this.businessService = businessService;	
	}
	
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public int getNoOfReaders() {
		return noOfReaders;
	}
	public void setNoOfReaders(int noOfReaders) {
		this.noOfReaders = noOfReaders;
	}
	public int getNoOfHelpful() {
		return noOfHelpful;
	}
	public void setNoOfHelpful(int noOfHelpful) {
		this.noOfHelpful = noOfHelpful;
	}
	public int getOverall() {
		return overall;
	}
	public void setOverall(int overall) {
		this.overall = overall;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	public int getRooms() {
		return rooms;
	}
	public void setRooms(int rooms) {
		this.rooms = rooms;
	}
	public int getLocation() {
		return location;
	}
	public void setLocation(int location) {
		this.location = location;
	}
	public int getCleanliness() {
		return cleanliness;
	}
	public void setCleanliness(int cleanliness) {
		this.cleanliness = cleanliness;
	}
	public int getFrontDesk() {
		return frontDesk;
	}
	public void setFrontDesk(int frontDesk) {
		this.frontDesk = frontDesk;
	}
	public int getService() {
		return service;
	}
	public void setService(int service) {
		this.service = service;
	}
	public int getBusinessService() {
		return businessService;
	}
	public void setBusinessService(int businessService) {
		this.businessService = businessService;
	}	
	public String toString(){
		return author + " " + content;
	}
}