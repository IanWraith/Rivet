package test.org.e2k;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.e2k.GW;
import org.e2k.Ship;

public class testGW extends TestCase {

	// A test case to check that the data from GW 2/101 packets is being decoded into correct MMSIs
	// Here are some test case payloads which are then check against what myself and Alan W think are the correct MMSIs
	public void testShipMMSI() {
		int a,errorCount=0;
		GW gw=new GW(null);
		List<Ship> ships=new ArrayList<Ship>();
		List<String> errorStrings=new ArrayList<String>();
		
		// 304653000 MARCHICORA
		final int loadData01[]={0xE0,0x57,0x03,0x66,0x66,0x66};
		ships.add(createShip("304653000"));
		
		// 565494000 SIGAS SILVIA
		final int loadData02[]={0xD3,0x73,0x72,0xE6,0x66,0x66};
		ships.add(createShip("565494000"));
		
		// 477824000 DARYA TARA
		final int loadData04[]={0x97,0x69,0x74,0x66,0x66,0x66};
		ships.add(createShip("477824000"));
		
		// 235069271 GRETA C
		final int loadData05[]={0x84,0x63,0x2D,0x14,0x62,0x66};
		ships.add(createShip("235069271"));
		
		// 477090000
		final int loadData06[]={0x97,0x61,0x62,0xE6,0x66,0x66};
		ships.add(createShip("477090000"));
		
		// 636090405
		final int loadData07[]={0x85,0x65,0x62,0xE7,0x63,0x66};
		ships.add(createShip("636090405"));
		
		// 308574000
		final int loadData08[]={0xE0,0x36,0xF1,0x66,0x66,0x66};
		ships.add(createShip("308574000"));
		
		// 371560000
		final int loadData09[]={0x90,0x32,0x65,0x66,0x66,0x66};
		ships.add(createShip("371560000"));
		
		// 232004630
		final int loadData10[]={0x84,0x64,0x76,0x05,0x66,0x66};
		ships.add(createShip("232004630"));		
	
		// 564110000
		final int loadData11[]={0xD3,0x27,0x62,0x66,0x66,0x66};
		ships.add(createShip("564110000"));		
		
		// 477076700
		final int loadData12[]={0x97,0x61,0x51,0x61,0x66,0x66};
		ships.add(createShip("477076700"));	
		
		// 211233290
		final int loadData13[]={0xA4,0x42,0x00,0x2C,0x66,0x66};
		ships.add(createShip("211233290"));	
		
		// 477981000
		final int loadData14[]={0x97,0x29,0x26,0xE6,0x66,0x66 };
		ships.add(createShip("477981000"));	
		
		// 636090616
		final int loadData15[]={0x85,0x65,0x62,0xA5,0x65,0x66};
		ships.add(createShip("636090616"));	
		
		// 636090821
		final int loadData16[]={0x85,0x65,0x62,0xC6,0xE2,0x66};
		ships.add(createShip("636090821"));			
		
		List<Integer> iList=new ArrayList<Integer>();
		// Test each member of the ships list
		for (a=0;a<ships.size();a++)	{
			// This is all a real bodge but was needed when I got rid of the GW ident array member of the ship class
			// which was no longer needed.
			// Anyway this is just a test
			if (a==0) iList=getGWIdentasArrayList(loadData01);
			else if (a==1) iList=getGWIdentasArrayList(loadData02);
			else if (a==2) iList=getGWIdentasArrayList(loadData04);
			else if (a==3) iList=getGWIdentasArrayList(loadData05);
			else if (a==4) iList=getGWIdentasArrayList(loadData06);
			else if (a==5) iList=getGWIdentasArrayList(loadData07);
			else if (a==6) iList=getGWIdentasArrayList(loadData08);
			else if (a==7) iList=getGWIdentasArrayList(loadData09);
			else if (a==8) iList=getGWIdentasArrayList(loadData10);
			else if (a==9) iList=getGWIdentasArrayList(loadData11);
			else if (a==10) iList=getGWIdentasArrayList(loadData12);
			else if (a==11) iList=getGWIdentasArrayList(loadData13);
			else if (a==12) iList=getGWIdentasArrayList(loadData14);
			else if (a==13) iList=getGWIdentasArrayList(loadData15);
			else if (a==14) iList=getGWIdentasArrayList(loadData16);
			
			String ret=gw.displayGW_ShipMMSI(iList,9);
			if (ret.indexOf(ships.get(a).getMmsi())==-1)	{
				errorCount++;
				String s="Bad decode of MMSI "+ships.get(a).getMmsi()+" have "+ret;
				errorStrings.add(s);
			}
		}
		
		// Have there been any errors ?
		if (errorCount>0)	{
			StringBuilder sb=new StringBuilder();
			sb.append(Integer.toString(errorCount)+" MMSIs have decoded incorrectly.");
			for (a=0;a<errorStrings.size();a++)	{
				sb.append("\r\n"+errorStrings.get(a));
			}
			fail(sb.toString());
		}
		
		
	}
	
	// Create a ship object given a raw GW ident and the MMSI
	private Ship createShip (String mmsi)	{
		Ship ship=new Ship();
		ship.setMmsi(mmsi);
		return ship;
	}
	
	// Return the GW ident as a Arraylist of ints
	private List<Integer> getGWIdentasArrayList (int ida[])	{
		List <Integer> gwList=new ArrayList<Integer>();
		int i;
		for (i=0;i<6;i++)	{
			gwList.add(ida[i]);
		}
		return gwList;	
	}
	
	// Test the decoding of shore side encoded MMSIs
	public void testShoreMMSI() {
		int a,errorCount=0;
		GW gw=new GW(null);
		List<Ship> ships=new ArrayList<Ship>();
		List<String> errorStrings=new ArrayList<String>();
		
		final int loadData01[]={0x9A,0xFF,0x5F,0xF2,0x99,0xF9};
		ships.add(createShip("538002583"));
	
		// Test each member of the ships list
		for (a=0;a<ships.size();a++)	{
			List<Integer> iList=new ArrayList<Integer>();
			
			if (a==0) iList=getGWIdentasArrayList(loadData01);
			
			String ret=gw.displayGW_ShoreMMSI(iList,9);
			if (ret.indexOf(ships.get(a).getMmsi())==-1)	{
				errorCount++;
				String s="Bad decode of shore MMSI "+ships.get(a).getMmsi()+" have "+ret;
				errorStrings.add(s);
			}
		}
		
		// Have there been any errors ?
		if (errorCount>0)	{
			StringBuilder sb=new StringBuilder();
			sb.append(Integer.toString(errorCount)+" shore MMSIs have decoded incorrectly.");
			for (a=0;a<errorStrings.size();a++)	{
				sb.append("\r\n"+errorStrings.get(a));
			}
			fail(sb.toString());
		}
		
	}	
	
	

}
