package de.regioosm.housenumbercore.util;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AddressTest.class, CountryTest.class,
		HousenumberListTest.class, ImportAddressTest.class,
		MunicipalityTest.class, OSMSegmentTest.class, OSMStreetTest.class,
		CsvReaderTest.class})
public class AllTests {

}
