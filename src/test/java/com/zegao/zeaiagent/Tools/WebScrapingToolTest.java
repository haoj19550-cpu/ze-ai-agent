package com.zegao.zeaiagent.Tools;

import com.zegao.zeaiagent.tools.WebScrapingTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class WebScrapingToolTest {

    @Test
    public void testScrapeWebPage() {
        WebScrapingTool tool = new WebScrapingTool();
        String url = "https://www.msn.cn/zh-cn/news/other/%E9%9C%87%E6%83%8A-%E8%BF%AA%E4%B8%BD%E7%83%AD%E5%B7%B4%E8%B7%91%E7%94%B7%E5%8F%AA%E5%BD%95%E4%BA%8612%E5%A4%A9-%E5%89%AA%E6%88%9012%E6%9C%9F%E6%92%AD3%E4%B8%AA%E6%9C%88-%E5%87%AD%E5%AE%9E%E5%8A%9B%E6%92%91%E8%B5%B7%E6%95%B4%E5%AD%A3/ar-AA21Kvbu?ocid=msedgdhp&pc=U531&cvid=69f084da0ac345ac9d3cc45a73c30b08&ei=52";
        String result = tool.scrapeWebPage(url);
        assertNotNull(result);
    }
}
