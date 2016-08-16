package com.checker.miner;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.checker.ApplicationProperties;
import com.checker.settings.DateRange;
import com.checker.settings.SettingIntepreterService;

/**
 * Get a Selenium page based on a mine request
 *
 * @author craftmaster2190
 *
 */
@Service
public class SeleniumReservationService {

	static Logger logger = Logger.getLogger(SeleniumReservationService.class);

	private static String buildUrl(String currentDay, long parkID, long entranceID) {
		String url = "http://www.recreation.gov/permits//r/entranceDetails.do?arvdate=" + currentDay
				+ "&contractCode=NRSO&parkId=" + parkID + "&entranceId=" + entranceID;
		logger.log(Level.DEBUG, "Searching currentDay(" + currentDay + ") and url(" + url + ")");
		return url;
	}

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	EntranceRepository entranceRepository;
	private FirefoxBinary firefoxBinary;
	@Autowired
	MineResultRepository mineResultRepository;

	@Autowired
	ParkRepository parkRepository;
	@Autowired
	SettingIntepreterService settingIntepreterService;

	private boolean useHTMLUnit;

	private Entrance findIfExistsOrCreateNewEntrance(long entranceID) {
		Entrance entrance = this.entranceRepository.findOne(entranceID);
		if (entrance == null) {
			entrance = new Entrance();
			entrance.setSysId(entranceID);
			this.entranceRepository.save(entrance);
		}
		return entrance;
	}

	private Park findIfExistsOrCreateNewPark(long parkID) {
		Park park = this.parkRepository.findOne(parkID);
		if (park == null) {
			park = new Park();
			park.setSysId(parkID);
			this.parkRepository.save(park);
		}
		return park;
	}

	private Iterable<LocalDate> getDateRangeIterableFromSettings() {
		DateRange dateRange = this.settingIntepreterService.getSettingAsDateRange("dates",
				new DateRange(this.applicationProperties.getDefaultDates()));
		logger.log(Level.DEBUG, "Searching date range: " + dateRange);
		Iterable<LocalDate> iterable = dateRange.getIterable();
		return iterable;
	}

	public void getMineResults() {
		boolean on = this.settingIntepreterService.getSettingAsBoolean("on", false);
		if (!on) {
			return;
		}
		WebDriver driver = this.getWebDriver();
		try {
			Iterable<LocalDate> dateRangeIterable = this.getDateRangeIterableFromSettings();
			int skipCount = 0;
			for (LocalDate day : dateRangeIterable) {
				if (skipCount > 0) {
					skipCount--;
					continue;
				}
				String currentDay = DateRange.dateToString(day);

				long[] parkIDs = new long[] { 115139 };
				for (long parkID : parkIDs) {
					long[] entranceIDs = new long[] { 356817 };
					for (long entranceID : entranceIDs) {
						String url = SeleniumReservationService.buildUrl(currentDay, parkID, entranceID);
						driver.get(url);

						WebElement findButton = driver.findElement(By.name("permitAvailabilitySearchButton"));
						findButton.click();

						List<WebElement> reservedStatuses = driver.findElements(By.className("permitStatus"));

						logger.log(Level.DEBUG,
								"reservedStatuses size for " + currentDay + ": " + reservedStatuses.size());
						if (reservedStatuses.size() == 14) {
							skipCount = reservedStatuses.size();
						}
						if (reservedStatuses.size() == 0) {
							logger.log(Level.DEBUG, "permit found at: " + currentDay);
							this.logNewMineResult(day, parkID, entranceID);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.ERROR, e.toString());
		} finally {
			driver.quit();
		}
	}

	private WebDriver getWebDriver() {
		WebDriver driver;
		if (this.useHTMLUnit) {
			driver = new HtmlUnitDriver();
		} else {
			driver = new FirefoxDriver(this.firefoxBinary, null);
		}
		return driver;
	}

	@PostConstruct
	public void init() {
		this.useHTMLUnit = this.applicationProperties.isUseHTMLUnit();
		if (this.useHTMLUnit) {
			return;
		}

		this.firefoxBinary = new FirefoxBinary();
		String xvfbDisplayPort = this.applicationProperties.getXvfbDisplayPort();
		if (this.applicationProperties.isUseXvfb() && xvfbDisplayPort != null) {
			this.firefoxBinary.setEnvironmentProperty("DISPLAY", xvfbDisplayPort);
		}
	}

	private void logNewMineResult(LocalDate day, long parkID, long entranceID) {
		MineResult mineResult = this.mineResultRepository.findByDateAndEntranceSysIdAndParkSysId(day, entranceID,
				parkID);
		if (mineResult == null) {
			mineResult = new MineResult();
			mineResult.setDate(day);

			Park park = this.findIfExistsOrCreateNewPark(parkID);
			mineResult.setPark(park);

			Entrance entrance = this.findIfExistsOrCreateNewEntrance(entranceID);
			mineResult.setEntrance(entrance);

			this.mineResultRepository.save(mineResult);
		}
	}

}
