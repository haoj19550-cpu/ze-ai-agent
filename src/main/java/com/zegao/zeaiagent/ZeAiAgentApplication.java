package com.zegao.zeaiagent;

import com.zegao.zeaiagent.rag.PgVectorVectorStoreConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
@MapperScan("com.zegao.zeaiagent.repository")
public class ZeAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZeAiAgentApplication.class, args);
    }

}
