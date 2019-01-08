package nz.govt.linz.AdminBoundariesTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
	UserReaderTomcat_Test.class,
	UserReaderPostgreSQL_Test.class,
	IniReader_Test.class
})

public class DABTestSuite {   
}