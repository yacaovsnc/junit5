package org.example;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class JUnit4Executor {

    public static void main(String... args) {
        runClass(TestSuite1.class);
        runClass(TestSuite2.class);
        runClass(TestSuite3.class);
        runClass(TestSuite4.class);
    }

    private static void runClass(Class<?> suiteClass) {
        System.out.println("Running " + suiteClass.getName());
        JUnitCore core = new JUnitCore();
        core.addListener(new RunListener() {
            @Override
            public void testStarted(Description description) {
                System.out.println("> Test started: " + description);
            }
        });
        Result result = core.run(suiteClass);
        System.out.println("Tests run: " + result.getRunCount());
        System.out.println();
    }
}
