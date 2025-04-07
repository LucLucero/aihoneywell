package com.lucero.aihoneywell;


import com.fasterxml.classmate.AnnotationOverrides;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DocsLoader {

    private static final Logger log = LoggerFactory.getLogger(DocsLoader.class);

    private final JdbcClient jdbcClient;
    private final VectorStore vectorStore;
    private final Resource pdfReferences;

    public DocsLoader(VectorStore vectorStore, @Value("classpath:/docs/caliper.pdf") Resource pdfReferences, JdbcClient jdbcClient) {
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
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                    .withLeftAlignment(true)
                                    .overrideLineSeparator("/n")
                                    .build())
                    .withPagesPerDocument(1)
                    .build();

            //for(Resource resource : pdfReferences ) {
            var pdfReader = new PagePdfDocumentReader(pdfReferences, config);
            var documents = pdfReader.get();
            var textSplitter = new TokenTextSplitter(200, 30, 10, 50000, true);
            var cleanedDocuments = new ArrayList<Document>();

            for (Document doc : documents) {
                Map<String, Object> result = doc.getMetadata();
                String cleaned = doc.getFormattedContent(MetadataMode.ALL)
                        .replaceAll("(?i)^.*copyright.*$", "")
                        .replaceAll("(?i)^.*confidentiality.*$", "")
                        .replaceAll("(?i)^.*trademarks.*$", "")
                        .replaceAll("(?m)^\\s*$", "")
                        .trim();
                if (!cleaned.isEmpty()) {
                    cleanedDocuments.add(new Document(cleaned, result));
                }
            }

            vectorStore.accept(textSplitter.apply(cleanedDocuments));

            log.info("Application is ready");
        };
    }
}
