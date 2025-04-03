package com.lucero.aihoneywell;


import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DocsLoader {

    private static final Logger log = LoggerFactory.getLogger(DocsLoader.class);

    private final JdbcClient jdbcClient;
    private final VectorStore vectorStore;
    private final Resource[] pdfReferences;

    public DocsLoader(VectorStore vectorStore, Resource[] pdfReferences, JdbcClient jdbcClient) {
        this.vectorStore = vectorStore;
        this.pdfReferences = pdfReferences;
        this.jdbcClient = jdbcClient;
    }

    @PostConstruct
    public void init() {
        Integer count = jdbcClient.sql("select count(*) from vector_store")
                .query(Integer.class)
                .single();

        log.info("Current count of the Vector Store: {}", count);
        if (count == 0) {
            log.info("Loading Spring Boot Reference PDF into Vector Store");
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            for(Resource resource : pdfReferences ) {
                var pdfReader = new PagePdfDocumentReader(resource, config);
                var textSplitter = new TokenTextSplitter();
                vectorStore.accept(textSplitter.apply(pdfReader.get()));

            }


            log.info("Application is ready");
        }
    }


}
