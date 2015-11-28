package com.wage;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class WageCalculator {

	private static final Logger logger = Logger.getLogger(WageCalculator.class);
	private static final double NORMAL_RATE = 3.75;
	private static final double RATE_PER_MINUTE = NORMAL_RATE / 60;
	private static final double EVENING_RATE = RATE_PER_MINUTE + (1.15 / 60);
	private static final double OVERTIME_RATE_0_2 = RATE_PER_MINUTE * 1.25;
	private static final double OVERTIME_RATE_2_4 = RATE_PER_MINUTE * 1.5;
	private static final double OVERTIME_RATE_4_16 = RATE_PER_MINUTE * 2;

	private static Map<String, long[]> employeeRecord = new HashMap<String, long[]>();
	private static String tempPersonName = "";
	private static String tempPersonId = "";
	private static String tempDate = "";
	private static String tempStart = "";
	private static String tempEnd = "";
	private static long tempTotalMinutes = 0L;
	private static long tempNormalMinutes = 0L;
	private static long tempEveningMinutes = 0L;
	private static boolean firstRecord = true;

	public static void main(String[] args) {
		final String localPathSeparator = System.getProperty("file.separator");
		final String localDirectory = System.getProperty("user.dir");
		final String fileName = "HourList201403.csv";
		final String dataInputFile = localDirectory + localPathSeparator
				+ fileName;
		Reader dataReader = null;
		CSVParser parser = null;
		try {
			dataReader = new FileReader(dataInputFile);
			parser = new CSVParser(dataReader, CSVFormat.EXCEL.withHeader());
			for (final CSVRecord record : parser) {
				singleRecordProcessing(record.get("Person Name"),
						record.get("Person ID"), record.get("Date"),
						record.get("Start"), record.get("End"));
			}
			employeeRecord.forEach((k, v) -> {
				Map<String, Double> singleEmployee = calculateWage(v);
				System.out.print("Employee ID: " + k);
				System.out.print(", TotalPay: "
						+ singleEmployee.get("totalPay")
						+ "$, Pay For Normal Hours: "
						+ singleEmployee.get("normalPay")
						+ ", Pay For Evening Hours: "
						+ singleEmployee.get("eveningPay")
						+ ", Pay For Overtime Hours 0-2: "
						+ singleEmployee.get("overtime0_2Pay")
						+ ", Pay For Overtime Hours 2-4: "
						+ singleEmployee.get("overtime2_4Pay")
						+ ", Pay For Overtime Hours 4-16: "
						+ singleEmployee.get("overtime4_16Pay"));
				System.out.println("\n\r");
			});
		} catch (IOException e) {
			logger.error("Error occurred while reading CSV file: " + e, e);
		} catch (Exception e) {
			logger.error("Error occurred during CSV's data processing: " + e, e);
		} finally {
			try {
				if (dataReader != null)
					dataReader.close();
				if (parser != null)
					parser.close();
			} catch (IOException e) {
				logger.error("Error occurred while closing CSV file: " + e, e);
			} catch (Exception e) {
				logger.error("Error occurred while closing CSV file processors: " + e, e);
			}
		}
	}

	private static void singleRecordProcessing(String personName,
			String personId, String date, String start, String end) {
		DateTime startDate = generateDate(date + " " + start);
		DateTime endDate = correctedEndDate(startDate, generateDate(date + " "
				+ end));
		Interval startEndInterval = getTotalInterval(startDate, endDate);
		long totalMinutes = startEndInterval.toDuration().getStandardMinutes();
		long eveningMinutes = calculateEveningMinutes(startDate, endDate);
		long normalMinutes = totalMinutes - eveningMinutes;
		long currentValues[] = { totalMinutes, normalMinutes, eveningMinutes,
				0L, 0L, 0L };
		if (tempTotalMinutes > 0) {
			if (personId.equalsIgnoreCase(tempPersonId)
					&& (date.equalsIgnoreCase(tempDate))) {
				addTempWithCurrentRecord(totalMinutes, normalMinutes,
						eveningMinutes);
			} else {
				updateEmployeeRecords(personId, checkOvertime(currentValues));
			}
		} else {
			assignTempValues(personName, personId, date, start, end,
					totalMinutes, normalMinutes, eveningMinutes);
			if (firstRecord) {
				updateEmployeeRecords(personId, checkOvertime(currentValues));
				firstRecord = false;
			}
		}
	}

	private static Map<String, Double> calculateWage(long[] singleEmployee) {
		System.out.println("Total Worked Hours: "
				+ convertMinutesToHoursMinutes(singleEmployee[0])
				+ " - Total minutes: " + singleEmployee[0]
				+ " - Normal minutes: " + singleEmployee[1]
				+ " - Evening minutes: " + singleEmployee[2]
				+ " - Overtime minutes for 0-2 Hours: " + singleEmployee[3]
				+ " - Overtime minutes for 2-4 Hours: " + singleEmployee[4]
				+ " - Overtime minutes for 4+ Hours: " + singleEmployee[5]);
		double normalPay = roundAmountToPrecision(singleEmployee[1] * RATE_PER_MINUTE, 2);
		double eveningPay = roundAmountToPrecision(singleEmployee[2] * EVENING_RATE, 2);
		double overtime0_2Pay = roundAmountToPrecision(singleEmployee[3] * OVERTIME_RATE_0_2, 2);
		double overtime2_4Pay = roundAmountToPrecision(singleEmployee[4] * OVERTIME_RATE_2_4, 2);
		double overtime4_16Pay = roundAmountToPrecision(singleEmployee[5] * OVERTIME_RATE_4_16, 2);
		double totalPay = normalPay + eveningPay + overtime0_2Pay + overtime2_4Pay + overtime4_16Pay;
		Map<String, Double> returnValues = new HashMap<>();
		returnValues.put("totalPay", roundAmountToPrecision(totalPay, 2));
		returnValues.put("normalPay", normalPay);
		returnValues.put("eveningPay", eveningPay);
		returnValues.put("overtime0_2Pay", overtime0_2Pay);
		returnValues.put("overtime2_4Pay", overtime2_4Pay);
		returnValues.put("overtime4_16Pay", overtime4_16Pay);
		return returnValues;
	}

	public static double roundAmountToPrecision(double amount, int precision) {
		return new BigDecimal(amount).setScale(precision, RoundingMode.HALF_UP)
				.doubleValue();
	}

	public static Interval getTotalInterval(DateTime startDate,
			DateTime endDate) {
		Interval localInterval;
		if (startDate.isAfter(endDate.getMillis())) {
			localInterval = new Interval(endDate, startDate);
		} else {
			localInterval = new Interval(startDate, endDate);
		}
		return localInterval;
	}

	public static DateTime generateDate(String date) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormat
				.forPattern("dd.MM.yyyy HH:mm");
		return dateTimeFormatter.parseDateTime(date);
	}

	private static DateTime correctedEndDate(DateTime startDate,
			DateTime endDate) {
		DateTime localEndDate;
		if (startDate.isAfter(endDate.getMillis())) {
			localEndDate = endDate.plusDays(1);
		} else {
			localEndDate = endDate;
		}
		return localEndDate;
	}

	private static void addTempWithCurrentRecord(long totalMinutes,
			long normalMinutes, long eveningMinutes) {
		tempTotalMinutes += totalMinutes;
		tempNormalMinutes += normalMinutes;
		tempEveningMinutes += eveningMinutes;
	}

	private static void assignTempValues(String personName, String personId,
			String date, String start, String end, long totalMinutesLong,
			long normalMinutes, long eveningMinutes) {
		tempPersonName = personName;
		tempPersonId = personId;
		tempDate = date;
		tempStart = start;
		tempEnd = end;
		tempTotalMinutes = totalMinutesLong;
		tempNormalMinutes = normalMinutes;
		tempEveningMinutes = eveningMinutes;
	}

	private static void updateEmployeeRecords(String employeeId,
			long[] workHourValues) {
		if (employeeRecord.containsKey(employeeId)) {
			entryExists(employeeId, workHourValues);
		} else {
			addEmployee(employeeId, workHourValues);
		}
	}

	private static void entryExists(String employeeId, long[] workHourValues) {
		long[] existingEntry = employeeRecord.get(employeeId);
		if (existingEntry.length == workHourValues.length) {
			for (int i = 0; i < workHourValues.length; i++) {
				existingEntry[i] = existingEntry[i] + workHourValues[i];
			}
		}
		employeeRecord.put(employeeId, existingEntry);
	}

	private static void addEmployee(String employeeId, long[] workHourValues) {
		employeeRecord.put(employeeId, workHourValues);
	}

	private static long[] checkOvertime(long[] currentValues) {
		long[] returnValues;

		if (currentValues.length > 5) {
			if (currentValues[0] > 480) {
				if (currentValues[0] <= 600) {
					returnValues = new long[] { currentValues[0], 480L, 0L,
							(currentValues[0] - 480L), 0L, 0L };
				} else {
					if (currentValues[0] > 600 && currentValues[0] <= 720) {
						returnValues = new long[] { currentValues[0], 480L, 0L,
								0L, (currentValues[0] - 480L), 0L };
					} else {
						returnValues = new long[] { currentValues[0], 480L, 0L,
								0L, 0L, (currentValues[0] - 480L) };
					}
				}
			} else {
				returnValues = currentValues;
			}
		} else {
			returnValues = currentValues;
		}
		return returnValues;
	}

	public static long calculateEveningMinutes(DateTime startDate,
			DateTime endDate) {
		long workedEveningShiftMinutes = 0L;
		String eveningStart = startDate.getDayOfMonth() + "."
				+ startDate.getMonthOfYear() + "." + startDate.getYear();
		String eveningEnd = (startDate.getDayOfMonth() + 1) + "."
				+ startDate.getMonthOfYear() + "." + startDate.getYear();
		DateTime eveningStartTime = generateDate(eveningStart + " " + "18:00");
		DateTime eveningEndTime = generateDate(eveningEnd + " " + "05:59");
		Interval eveningShift = new Interval(eveningStartTime, eveningEndTime);
		Interval workedShift = new Interval(startDate, endDate);
		if (workedShift.overlaps(eveningShift)) {
			Interval workedEveningShift = workedShift.overlap(eveningShift);
			workedEveningShiftMinutes = workedEveningShift.toDuration()
					.getStandardMinutes();
		}
		return workedEveningShiftMinutes;
	}

	public static String convertMinutesToHoursMinutes(long minutes) {
		long hours = minutes / 60;
		long remainingMinutes = minutes % 60;
		String totalHours = hours + ":" + remainingMinutes;
		return totalHours;
	}
}
