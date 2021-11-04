package com.zcxa.api;

import com.zcxa.api.services.ScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

@SpringBootTest
class ApiApplicationTests {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Autowired
    private ScopeService scopeService;

    @Test
    void contextLoads() throws IOException, InterruptedException {
        for(int i=0;i<10;i++){
            Thread thread = new Thread(()->{
                String tmp = UUID.randomUUID().toString();
                logger.info(tmp);
                scopeService.init(tmp);
                scopeService.getTmp();
            });
            thread.start();
        }
        System.in.read();
    }

}
