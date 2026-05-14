//package rag;
//
//import com.zegao.zeaiagent.rag.LoveAppVectorStoreConfig;
//import org.junit.jupiter.api.Test;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class LoveAppVectorStoreConfigTest {
//    private EmbeddingModel dashscopeEmbeddingModel;
//
//    @Test
//    void testLoveAppVectorStore() {
//        LoveAppVectorStoreConfig config = new LoveAppVectorStoreConfig();
//        VectorStore vectorStore = config.loveAppVectorStore(dashscopeEmbeddingModel);
//        assertNotNull(vectorStore);
//
//    }
//}