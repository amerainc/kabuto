package com.rainc.kabuto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class KabutoApplicationTests {
    @Autowired
    com.rainc.kabuto.Test test;

    @org.junit.Test
    void contextLoads() throws InterruptedException {
        test.asyncTest(new Date(),"test","123");
        TimeUnit.MINUTES.sleep(2);
    }
}
