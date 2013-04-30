package test.org.e2k;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.e2k.GW;

public class testGW extends TestCase {

	// A test case to check that the data from GW 2/101 packets is being decoded into correct MMSIs
	// Here are some test case payloads which are then check against what myself and Alan W think are the correct MMSIs
	public void testMMSI() {
		int a;
		GW gw=new GW(null);
		List<Integer> testData=new ArrayList<Integer>();
		
		// 235000633 (000) CITY OF AMSTERDAM
		final int loadData1[]={0x84,0x63,0x66,0x85,0x60,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData1[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("235000633")==-1) fail("Bad decode of MMSI 235000633");
	
		// 236313000 (000) STEN MOSTER
		final int loadData2[]={0x84,0x05,0x02,0x6E,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData2[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("236313000")==-1) fail("Bad decode of MMSI 236313000");
		
		// 636011947 (000) PORT ARTHUR
		final int loadData3[]={0x85,0x65,0x22,0xF2,0xE1,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData3[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("636011947")==-1) fail("Bad decode of MMSI 636011947");
	
		// 565494000 (000) SIGAS SILVIA
		final int loadData4[]={0xD3,0x73,0x72,0xE6,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData4[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("565494000")==-1) fail("Bad decode of MMSI 565494000");
		
		// 304653000 (000) MARCHICORA
		final int loadData5[]={0xE0,0x57,0x03,0x66,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData5[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("304653000")==-1) fail("Bad decode of MMSI 304653000");
		
		// 258953000 (000) STEN AURORA
		final int loadData6[]={0xb4,0x2e,0x83,0x66,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData6[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("258953000")==-1) fail("Bad decode of MMSI 258953000");
		
		// 235069271 (000) GRETA C
		final int loadData7[]={0x84,0x63,0x2D,0x14,0x62,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData7[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("235069271")==-1) fail("Bad decode of MMSI 235069271");
	
		// 477824000 (000) DARYA TARA
		final int loadData8[]={0x97,0x69,0x74,0x66,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData8[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("477824000")==-1) fail("Bad decode of MMSI 477824000");
	
		// 636010534 (000) CUMBRIA
		final int loadData9[]={0x85,0x65,0x62,0x83,0x67,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData9[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("636010534")==-1) fail("Bad decode of MMSI 636010534");
	
		// 235011060 (000) PAUL SCHULTE
		final int loadData10[]={0x84,0x63,0x22,0x56,0xe6,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData10[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("235011060")==-1) fail("Bad decode of MMSI 235011060");
		
		// 232004630 (000) CITY OF AMSTERDAM
		final int loadData11[]={0x84,0x64,0x76,0x5,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData11[a]);
		}
		if (gw.displayGW_MMSI(testData).indexOf("232004630")==-1) fail("Bad decode of MMSI 232004630");
		
	}

}
