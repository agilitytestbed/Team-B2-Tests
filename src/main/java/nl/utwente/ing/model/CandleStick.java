package nl.utwente.ing.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CandleStick {
	private double open;
	private double close;
	private double high;
	private double low;
	private double volume;
	private long timestamp;
	
	public CandleStick() {
		
	}
	
	public CandleStick(double open, double close, double high, double low, double volume, long timestamp) {
		this.open = open;
		this.close = close;
		this.high = high;
		this.low = low;
		this.volume = volume;
		this.timestamp = timestamp;
	}

	public double getOpen() {
		return open;
	}

	public void setOpen(double open) {
		this.open = open;
	}

	public double getClose() {
		return close;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public double getHigh() {
		return high;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}
	
	@Override
	public String toString() {
		return "open= " + open + " close= " + close + " high= " + high + " low= " + low + " volume= " + volume + " time= " +
	DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
    .withZone(ZoneOffset.UTC)
    .format(Instant.ofEpochSecond(timestamp));
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
