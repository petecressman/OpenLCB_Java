package scenarios.can;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TwoOnASegment.class,
        NineOnASegment.class
})

/**
 * This Package is named so that maven will automatically pick it up to run.
 * @author  Bob Jacobsen   Copyright 2009
 * @version $Revision$
 */
public class CanScenariosTest {
}
