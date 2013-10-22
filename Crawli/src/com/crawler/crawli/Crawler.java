package com.crawler.crawli;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Hashtable;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: tom
 * Date: 21.08.13
 * Time: 20:16
 * To change this template use File | Settings | File Templates.
 */
public class Crawler {

    private URL startUrl;
    private BlockingQueue<String> urls = new LinkedBlockingDeque<String>();
    private AtomicCounter visited = new AtomicCounter();
    private AtomicCounter exitet = new AtomicCounter();
    private ExecutorService executor = Executors.newFixedThreadPool(50);
    private boolean exit = false;

    public Crawler(URL url) {
        this.startUrl = url;
    }

    public void startCrawling() {
        System.out.println("Start crawler");

        try {
            InputStreamReader in = null;
            try {
                in = new InputStreamReader(startUrl.openStream());
            } catch (UnknownHostException e) {
                return;
            }
            ParserDelegator parser = new ParserDelegator();
            parser.parse(in, new HTMLEditorKit.ParserCallback() {
                public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {

                    if (t == HTML.Tag.A) {
                        Enumeration attrNames = a.getAttributeNames();
                        while (attrNames.hasMoreElements()) {
                            Object key = attrNames.nextElement();
                            if ("href".equals(key.toString())) {
                                if (((String) a.getAttribute(key)).contains("http") &&
                                        !((String) a.getAttribute(key)).contains("vpn") &&
                                        !((String) a.getAttribute(key)).contains("ftp") &&
                                        !((String) a.getAttribute(key)).contains("download")) {
                                    try {
                                        urls.put((String) a.getAttribute(key));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                public void handleText(char[] data, int pos) {
                    System.out.println(data);
                }

            }, true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        Thread exitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                if(System.in.read()>0)
                    exit = true;
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        });
        exitThread.start();


        System.out.println(urls.size());
        while (true) {
            while(urls.size()==0);

            if(exit)
                break;

            executor.execute(new Runnable() {
                private InputStreamReader in = null;
                private long start = 0;
                private String urlString = null;
                @Override
                public void run() {
                     start = System.currentTimeMillis();
                    try {
                        urlString = urls.poll(100, TimeUnit.MILLISECONDS);
                        if(urlString == null){
                            exitet.getNextValue();
                            return;
                        }
                        URL url = new URL(urlString);

                        if (url != null)
                            try {
                                URLConnection c = url.openConnection();
                                c.setConnectTimeout(5000);
                                c.setReadTimeout(10000);
                                in = new InputStreamReader(c.getInputStream());
                            } catch (Exception e) {
                                exitet.getNextValue();
                                return;
                            }

                        else {
                            exitet.getNextValue();
                            return;
                        }
                        visited.getNextValue();

                        try {
                            ParserDelegator parser = new ParserDelegator();
                            parser.parse(in, new HTMLEditorKit.ParserCallback() {
                                public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                                    if (t == HTML.Tag.A) {
                                        Enumeration attrNames = a.getAttributeNames();
                                        StringBuilder b = new StringBuilder();
                                        while (attrNames.hasMoreElements()) {
                                            Object key = attrNames.nextElement();
                                            if ("href".equals(key.toString())) {
                                                String url = (String) a.getAttribute(key);
                                                if (url.contains("http") && !url.contains("vpn") &&
                                                        !url.contains("ftp") && !url.contains("download")) {
                                                    if (url.matches("([^\\s]+(\\.(?i)(html|php|xml))$)") ||
                                                            url.matches("([^\\s]+(\\.(?i)(com|org|gov|info))$)"))
                                                        try {
                                                            if (urls.size() < 2000)
                                                                urls.put((String) a.getAttribute(key));
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            exitet.getNextValue();
                                                            return;
                                                        }
                                                    //writeLock.unlock();
                                                }
                                            }
                                        }
                                    }

                                }
                                public void handleText(char[] data, int pos) {
                                    String s = new String(data);

                                    s.replace(",","");
                                    s.replace("-","");
                                    s.replace(";","");
                                    s.replace(":","");
                                    s.replace(".","");

                                    String words[] = s.split(" ");

                                    Hashtable<String,Integer> wordVector = new Hashtable();

                                    for(String word : words)
                                        if(wordVector.containsKey(word)){
                                            Integer i = wordVector.get(word);
                                            i++;
                                            wordVector.put(word,i);
                                        }
                                        else
                                            wordVector.put(word,new Integer(1));


                                    Set<String> keys = wordVector.keySet();
                                    for(String key : keys)
                                        System.out.println(key + ": " + wordVector.get(key) );

                                }
                            }, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                            exitet.getNextValue();
                            return;
                        }


                    } catch (Exception ex) {
                        exitet.getNextValue();
                        ex.printStackTrace();
                        return;
                    }
                    exitet.getNextValue();
                    System.out.println("Visited (" + visited.getValue() + "): " + urlString + "(" + (System.currentTimeMillis() - start) + "ms)");
                }
            });
        }
        executor.shutdown();
        System.out.println("All threads startet.");

        while (!executor.isTerminated());


        System.out.println("All threads finished");
    }

    public void stopCrawling() {


    }

    public class AtomicCounter {
        private final AtomicInteger value = new AtomicInteger(0);

        public int getValue() {
            return value.get();
        }

        public int getNextValue() {
            return value.incrementAndGet();
        }

        public int getPreviousValue() {
            return value.decrementAndGet();
        }
    }
}
