package nl.utwente.ing.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class Transaction {
	private int id;
	
	private String externalIBAN;
	
	private double amount;
	private String description = "";
	private String date;
	private TransactionType type;
	private Category category;
	
	
	public Transaction() {
		
	}
	
	public Transaction(int id, String date, double amount,
			String externalIBAN, String type, Category category) {
		setId(id);
		setAmount(amount);
		setDate(date);
		setType(TransactionType.valueOf(type));
		setExternalIBAN(externalIBAN);
		setCategory(category);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
	
	public void setDate(Instant i) {
		this.date =  DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
                .withZone(ZoneOffset.UTC)
                .format(i);
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getExternalIBAN() {
		return externalIBAN;
	}

	public void setExternalIBAN(String external_iban) {
		this.externalIBAN = external_iban;
	}

	public TransactionType getType() {
		return type;
	}

	public void setType(TransactionType type) {
		this.type = type;
	}
	
	public int CategoryID() {
		if (category != null) {
			return category.getId();
		} else {
			return -1;
		}
	}
	
	public boolean validTransaction() {
		
		// if a value is null
		if (externalIBAN == null || date == null || type == null || description == null) {
			return false;
		}
		
		
		// if amount is negative or zero
		if (amount < 1) {
			System.out.println("Amount value problem");
			return false;
		}
		
		// if the date is not valid date-time
		try {
			DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
		    timeFormatter.parse(date);
		} catch (DateTimeParseException e) {
			return false;
		}
		
		return true;
		
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public boolean equals(Transaction t) {
		if (t.getAmount() == amount && t.CategoryID() == CategoryID() && t.getDate().equals(date) 
				&& t.getDescription().equals(description) && t.getExternalIBAN().equals(externalIBAN)
				&& t.getType().equals(type)) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return new JSONObject()
				.put("id", id)
				.put("amount", amount)
				.put("categoryID", CategoryID())
				.put("date", date)
				.put("description", description)
				.put("externalIBAN", externalIBAN)
				.put("type", type).toString();
	}
}
