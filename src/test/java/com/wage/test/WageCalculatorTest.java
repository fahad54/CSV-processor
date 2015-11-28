package com.wage.test;

import org.joda.time.Interval;
import org.junit.Test;

import com.wage.WageCalculator;

public class WageCalculatorTest {
	@Test
	public void testConvertMinutesToHoursMinutes() {
		long minutes = 493;
		System.out.println("testConvertMinuteToHoursMinutes => " + minutes
				+ " = " + WageCalculator.convertMinutesToHoursMinutes(minutes)
				+ " (HH:mm)");
	}

	@Test
	public void testEveningShiftWorked() {
		long eveningMinutes = WageCalculator.calculateEveningMinutes(
				WageCalculator.generateDate("02.12.2014 16:00"),
				WageCalculator.generateDate("02.12.2014 23:15"));
		System.out
		.println("testEveningShiftWorked => "
				+ WageCalculator
				.convertMinutesToHoursMinutes(eveningMinutes)
				+ " (HH:mm) can be classified as Evening Shift when working from \'02.12.2014 16:00\' to \'02.12.2014 23:15\'");
	}

	@Test
	public void testGenerateDate() {
		String stringDate = "02.12.2014 23:15";
		System.out.println("testGenerateDate => "
				+ WageCalculator.generateDate(stringDate)
				+ " *** is Joda based DateTime");
	}

	@Test
	public void testRoundPayToNearestCent() {
		double calculatedPay = 533.78842;
		System.out.println("testRoundPayToNearestCent => "
				+ WageCalculator.roundAmountToPrecision(calculatedPay, 2)
				+ " Pay Rounded to nearest cent from: " + calculatedPay);
	}

	@Test
	public void testRoundAtAnyDecimalPoint() {
		double calculatePayAtDifferentDecimalPoints = 533.875969;
		System.out.println("testRoundAtAnyDecimalPoint => Original="
				+ calculatePayAtDifferentDecimalPoints
				+ " - Rounded to 3 Decimal Points: "
				+ WageCalculator.roundAmountToPrecision(
						calculatePayAtDifferentDecimalPoints, 3)
						+ " --- Rounded to 5 Decimal Points: "
						+ WageCalculator.roundAmountToPrecision(
								calculatePayAtDifferentDecimalPoints, 5));
	}

	@Test
	public void testGetTotalInterval() {
		Interval totalInterval = WageCalculator.getTotalInterval(
				WageCalculator.generateDate("02.11.2015 10:02"),
				WageCalculator.generateDate("02.11.2015 20:37"));
		System.out.println("testGetTotalInterval between \'02.11.2015 10:02\' to \'02.11.2015 20:37\' is ** "
				+ totalInterval.toPeriod().getHours() + ":"
				+ totalInterval.toPeriod().getMinutes());
	}
}