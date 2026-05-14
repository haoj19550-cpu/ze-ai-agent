package com.zegao.zeaiagent.tools;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebScrapingTool {

    private static final int MAX_CONTENT_LENGTH = 5000;

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            String text = doc.text();
            if (text.length() > MAX_CONTENT_LENGTH) {
                text = text.substring(0, MAX_CONTENT_LENGTH) + "...(内容已截断)";
            }
            return text;
        } catch (IOException e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
