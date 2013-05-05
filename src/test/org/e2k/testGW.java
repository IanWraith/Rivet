package test.org.e2k;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.e2k.GW;
import org.e2k.Ship;

public class testGW extends TestCase {

	// A test case to check that the data from GW 2/101 packets is being decoded into correct MMSIs
	// Here are some test case payloads which are then check against what myself and Alan W think are the correct MMSIs
	public void testMMSI() {
		int a;
		GW gw=new GW(null);
		List<Ship> ships=new ArrayList<Ship>();
		Ship ship=new Ship();
	
		// 304653000 (000) MARCHICORA
		int loadData01[]={0xE0,0x57,0x03,0x66,0x66,0x66};
		ship.setGwIdent(loadData01);
		ship.setMmsi("304653000");
		ship.setName("MARCHICORA");
		ships.add(ship);
		
		// 565494000 (000) SIGAS SILVIA
		final int loadData02[]={0xD3,0x73,0x72,0xE6,0x66,0x66};
		ship.setGwIdent(loadData02);
		ship.setMmsi("565494000");
		ship.setName("SIGAS SILVIA");
		ships.add(ship);	
		
		// 236313000 STEN MOSTER
		final int loadData03[]={0x84,0x05,0x02,0x6E,0x66,0x66};
		ship.setGwIdent(loadData03);
		ship.setMmsi("236313000");
		ship.setName("STEN MOSTER");
		ships.add(ship);
		
		// 477824000 DARYA TARA
		final int loadData04[]={0x97,0x69,0x74,0x66,0x66,0x66};
		ship.setGwIdent(loadData04);
		ship.setMmsi("477824000");
		ship.setName("DARYA TARA");
		ships.add(ship);
		
		// 235069271 GRETA C
		final int loadData05[]={0x84,0x63,0x2D,0x14,0x62,0x66};
		ship.setGwIdent(loadData05);
		ship.setMmsi("235069271");
		ship.setName("GRETA C");
		ships.add(ship);
		
		
		// Test each member of the ships list
		for (a=0;a<ships.size();a++)	{
			if (gw.displayGW_MMSI(getGWIdentasArrayList(ships.get(a))).indexOf(ships.get(a).getMmsi())==-1)	{
				String s="Bad decode of MMSI "+ships.get(a).getMmsi()+" ("+ships.get(a).getName()+")";
				fail(s);
			}
		}
		
		
		
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
