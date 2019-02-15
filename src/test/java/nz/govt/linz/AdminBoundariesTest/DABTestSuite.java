package nz.govt.linz.AdminBoundariesTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
	DABConnector_Test.class,
	UserReaderPostgreSQL_Test.class,
	UserReaderTomcat_Test.class,
	UserReaderAIMS_Test.class,
	IniReader_Test.class,
	DABFormatter_Test.class,
	DABContainerComp_Test.class,
	ProcessControl_Test.class
})

public class DABTestSuite {
}