package test.org.e2k;

import junit.framework.TestCase;
import org.e2k.UserIdentifier;
import org.e2k.Ship;

public class testUserIdentifier extends TestCase {
	
	public void testgetShipDetails()	{
		UserIdentifier uid=new UserIdentifier();
		// Test with
		// <name val='THORNBURY-C6RS7'/>
		// <mmsi val='311168000'/>
		// <flag val='Bahamas'/>
		Ship ship=uid.getShipDetails("311168000");
		// Check the returns
		if (ship==null) fail("No ship found !");
		else if (ship.getName().indexOf("THORNBURY-C6RS7")!=0) fail("Wrong ships name returned !");
		else if (ship.getFlag().indexOf("Bahamas")!=0) fail("Wrong ships name returned !");
	}

}
