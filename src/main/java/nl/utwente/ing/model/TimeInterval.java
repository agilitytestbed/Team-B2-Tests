package nl.utwente.ing.model;

import java.time.temporal.ChronoUnit;

public enum TimeInterval {
	HOUR(ChronoUnit.HOURS), DAY(ChronoUnit.DAYS), WEEK(ChronoUnit.WEEKS), MONTH(ChronoUnit.MONTHS), YEAR(ChronoUnit.YEARS);
	
	private ChronoUnit unit;
	
	TimeInterval(ChronoUnit unit) {
		this.setUnit(unit);
	}

	public ChronoUnit getUnit() {
		return unit;
	}

	public void setUnit(ChronoUnit unit) {
		this.unit = unit;
	}
	

}
