package nl.utwente.ing.model;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentRequest {
	private int id;
	private String description;
	private String due_date;
	private double amount;
	private int number_of_requests;
	private boolean filled;
	private List<Transaction> transactions = new ArrayList<>();
	
	public PaymentRequest() {
		
	}
	

	public PaymentRequest(int id, String description, long unixTimestamp, double amount, int number_of_requests,
			boolean filled, List<Transaction> transactions) {
		
		due_date = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochSecond(unixTimestamp));
		this.id = id;
		this.description = description;
		this.amount = amount;
		this.number_of_requests = number_of_requests;
		this.filled = filled;
		this.transactions = transactions;
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}


	/**
	 * @return the due_date
	 */
	public String getDue_date() {
		return due_date;
	}


	/**
	 * @param due_date the due_date to set
	 */
	public void setDue_date(String due_date) {
		this.due_date = due_date;
	}


	/**
	 * @return the amount
	 */
	public double getAmount() {
		return amount;
	}


	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}


	/**
	 * @return the number_of_requests
	 */
	public int getNumber_of_requests() {
		return number_of_requests;
	}


	/**
	 * @param number_of_requests the number_of_requests to set
	 */
	public void setNumber_of_requests(int number_of_requests) {
		this.number_of_requests = number_of_requests;
	}


	/**
	 * @return the filled
	 */
	public boolean isFilled() {
		return filled;
	}


	/**
	 * @param filled the filled to set
	 */
	public void setFilled(boolean filled) {
		this.filled = filled;
	}


	/**
	 * @return the transactions
	 */
	public List<Transaction> getTransactions() {
		return transactions;
	}


	/**
	 * @param transactions the transactions to set
	 */
	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}


	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	public boolean validPaymentRequest() {
		if (description == null || amount <= 0 || due_date == null || number_of_requests <= 0 || transactions == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public long returnUnixTimestamp() {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		return LocalDateTime.from(formatter.parse(due_date)).toEpochSecond(ZoneOffset.UTC);
	}
	
	public boolean equalsData(PaymentRequest pr) {
		if (pr.getAmount() == amount && pr.getDescription().equals(description) && pr.getDue_date().equals(due_date)
				&& pr.getNumber_of_requests() == number_of_requests && pr.getTransactions().equals(transactions)) {
			return true;
		}
		return false;
	}
	
	public String toStringData() {
		JSONObject json = new JSONObject();
		json.put("amount", amount)
			.put("description", description)
			.put("due_date", due_date)
			.put("number_of_requests", number_of_requests);
		return json.toString();
	}
}
