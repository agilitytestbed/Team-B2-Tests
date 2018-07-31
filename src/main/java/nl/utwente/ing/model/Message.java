package nl.utwente.ing.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Message {
	private int id;
	private String message;
	private String date;
	private boolean read;
	private MessageType type;
	
	public Message() {
		
	}
	
	public Message(int id, String message, long unixTimestamp, 
			boolean read, String type) {
		String date = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochSecond(unixTimestamp));
		setId(id);
		setMessage(message);
		setDate(date);
		setRead(read);
		setType(MessageType.valueOf(type));
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}
	
	public long returnUnixTimestamp() {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		return LocalDateTime.from(formatter.parse(date)).toEpochSecond(ZoneOffset.UTC);
	}
	
	public boolean equalsData(Message m) {
		if (m.getMessage().equals(message) && m.returnUnixTimestamp() == returnUnixTimestamp() &&
				m.getType().equals(type) && read == m.isRead()) {
			return true;
		} else {
			return false;
		}
	}
}
