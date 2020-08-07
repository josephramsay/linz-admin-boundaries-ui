package nz.govt.linz.AdminBoundariesTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserReaderUtils {
	
	public static List<String> readCreds() { 
		List<String> lines = new ArrayList<String>();
		try {
			String line;
			BufferedReader br = new BufferedReader(new FileReader("tempdat.ini"));
			while ((line = br.readLine()) != null) { lines.add(line);}
			br.close();
		}
		catch (IOException ioe) {
			//LOGGER.info("Can't find creds file "+ioe);
			lines.add("user:pass");//fake, not needed in servlet context
			lines.add("svr:localhost");
		}
		return lines;
		
	}
	public static String[] genParams(String url_str) { 
		List<String> creds = readCreds();
		return new String[] { 
			url_str.replace("<SVR>",creds.get(1).split(":",2)[1]),
			creds.get(0).split(":",2)[0],
			creds.get(0).split(":",2)[1]
		};
	}
}
