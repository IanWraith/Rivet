package test.org.e2k;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.e2k.GW;
import org.e2k.Ship;

public class testGW extends TestCase {

	// A test case to check that the data from GW 2/101 packets is being decoded into correct MMSIs
	// Here are some test case payloads which are then check against what myself and Alan W think are the correct MMSIs
	////////////////////////////////////////////////////////////////////////////////
	// Yes I know some of these test cases fail ! That I'm afraid is the problem. //
    ////////////////////////////////////////////////////////////////////////////////
	public void testMMSI() {
		int a,errorCount=0;
		GW gw=new GW(null);
		List<Ship> ships=new ArrayList<Ship>();
		List<String> errorStrings=new ArrayList<String>();
		
		// 304653000 MARCHICORA
		final int loadData01[]={0xE0,0x57,0x03,0x66,0x66,0x66};
		ships.add(createShip("304653000",loadData01));
		
		// 565494000 SIGAS SILVIA
		//final int loadData02[]={0xD3,0x73,0x72,0xE6,0x66,0x66};
		//ships.add(createShip("565494000",loadData02));
		
		// 236313000 STEN MOSTER
		//final int loadData03[]={0x84,0x05,0x02,0x6E,0x66,0x66};
		//ships.add(createShip("236313000",loadData03));
		
		// 477824000 DARYA TARA
		final int loadData04[]={0x97,0x69,0x74,0x66,0x66,0x66};
		ships.add(createShip("477824000",loadData04));
		
		// 235069271 GRETA C
		final int loadData05[]={0x84,0x63,0x2D,0x14,0x62,0x66};
		ships.add(createShip("235069271",loadData05));
		
		// 477090000
		final int loadData06[]={0x97,0x61,0x62,0xE6,0x66,0x66};
		ships.add(createShip("477090000",loadData06));
		
		// 636090405
		final int loadData07[]={0x85,0x65,0x62,0xE7,0x63,0x66};
		ships.add(createShip("636090405",loadData07));
		
		// 308574000
		final int loadData08[]={0xE0,0x36,0xF1,0x66,0x66,0x66};
		ships.add(createShip("308574000",loadData08));
		
		// 371560000
		final int loadData09[]={0x90,0x32,0x65,0x66,0x66,0x66};
		ships.add(createShip("371560000",loadData09));
		
		// 232004630
		final int loadData10[]={0x84,0x64,0x76,0x05,0x66,0x66};
		ships.add(createShip("232004630",loadData10));		
	
		// 564110000
		final int loadData11[]={0xD3,0x27,0x62,0x66,0x66,0x66};
		ships.add(createShip("564110000",loadData11));		
		
		// 477076700
		final int loadData12[]={0x97,0x61,0x51,0x61,0x66,0x66};
		ships.add(createShip("477076700",loadData12));	
		
		// 211233290
		final int loadData13[]={0xA4,0x42,0x00,0x2C,0x66,0x66};
		ships.add(createShip("211233290",loadData13));	
		
		// 477981000
		final int loadData14[]={0x97,0x29,0x26,0xE6,0x66,0x66 };
		ships.add(createShip("477981000",loadData14));	
		
		// 636090616
		final int loadData15[]={0x85,0x65,0x62,0xA5,0x65,0x66};
		ships.add(createShip("636090616",loadData15));	
		
		// 636090821
		final int loadData16[]={0x85,0x65,0x62,0xC6,0xE2,0x66};
		ships.add(createShip("636090821",loadData16));			
		
		
		// Test each member of the ships list
		for (a=0;a<ships.size();a++)	{
			String ret=gw.displayGW_MMSI(getGWIdentasArrayList(ships.get(a)));
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
	private Ship createShip (String mmsi,int[]rawident)	{
		Ship ship=new Ship();
		ship.setGwIdent(rawident);
		ship.setMmsi(mmsi);
		return ship;
	}
	
	// Return the GW ident as a Arraylist of ints
	private List<Integer> getGWIdentasArrayList (Ship ship)	{
		List <Integer> gwList=new ArrayList<Integer>();
		int i;
		int ida[]=ship.getGwIdent();
		for (i=0;i<6;i++)	{
			gwList.add(ida[i]);
		}
		return gwList;	
	}

}
