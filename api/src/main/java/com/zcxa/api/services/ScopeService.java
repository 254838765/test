package com.zcxa.api.services;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

// @Scope("request")
@Service
public class ScopeService {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private String tmp;


    public void init(String tmp){
        this.tmp = tmp;
    }

    public String getTmp(){

        logger.info(tmp);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return tmp;
    }

}
