package com.crawler.crawli;

import com.crawler.crawli.Crawler;

import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: tom
 * Date: 21.08.13
 * Time: 20:16
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    public static void main(String[] args){

        try{
    //    com.crawler.crawli.Crawler crawler = new com.crawler.crawli.Crawler(new URL("https://prism-break.org/"));
        Crawler crawler = new Crawler(new URL("https://www.mozilla.org/en-US/firefox/os/"));
        crawler.startCrawling();
        }catch(Exception e){

        }


    }
}
