package test.org.e2k;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.e2k.GW;

public class testGW extends TestCase {

	
	public void testMMSI() {
		int a;
		GW gw=new GW(null);
		List<Integer> testData=new ArrayList<Integer>();
		
		final int loadData1[]={0x84,0x63,0x66,0x85,0x60,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData1[a]);
		}
		String rs1=gw.displayGW_MMSI(testData);
		
		final int loadData2[]={0x84,0x05,0x02,0x6E,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData2[a]);
		}
		String rs2=gw.displayGW_MMSI(testData);
		
		final int loadData3[]={0x85,0x65,0x22,0xF2,0xE1,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData3[a]);
		}
		String rs3=gw.displayGW_MMSI(testData);
		
		final int loadData4[]={0xD3,0x73,0x72,0xE6,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData4[a]);
		}
		String rs4=gw.displayGW_MMSI(testData);
		
		final int loadData5[]={0xE0,0x57,0x03,0x66,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData5[a]);
		}
		String rs5=gw.displayGW_MMSI(testData);
		
		final int loadData6[]={0xb4,0x2e,0x83,0x66,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData6[a]);
		}
		String rs6=gw.displayGW_MMSI(testData);
		
		final int loadData7[]={0x84,0x63,0x2D,0x14,0x62,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData7[a]);
		}
		String rs7=gw.displayGW_MMSI(testData);
		
		final int loadData8[]={0x97,0x69,0x74,0x66,0x66,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData8[a]);
		}
		String rs8=gw.displayGW_MMSI(testData);
		
		final int loadData9[]={0x85,0x65,0x62,0x83,0x67,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData9[a]);
		}
		String rs9=gw.displayGW_MMSI(testData);
		
		final int loadData10[]={0x84,0x63,0x22,0x56,0xe6,0x66};
		testData.clear();
		for (a=0;a<6;a++) {
			testData.add(loadData10[a]);
		}
		String rs10=gw.displayGW_MMSI(testData);
		
		
		String ent="rs1="+rs1+"\nrs2="+rs2+"\nrs3="+rs3+"\nrs4="+rs4+"\nrs5="+rs5+"\nrs6="+rs6+"\nrs7="+rs7+"\nrs8="+rs8+"\nrs9="+rs9+"\nrs10="+rs10;
		
		fail("Not yet implemented");
	}

}
