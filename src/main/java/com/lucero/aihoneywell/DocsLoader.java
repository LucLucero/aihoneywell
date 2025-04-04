package com.lucero.aihoneywell;


import com.fasterxml.classmate.AnnotationOverrides;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
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
                            .withNumberOfTopPagesToSkipBeforeDelete(100)
                            .withNumberOfBottomTextLinesToDelete(15)
                            .withNumberOfTopTextLinesToDelete(15)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            //for(Resource resource : pdfReferences ) {
            var pdfReader = new PagePdfDocumentReader(pdfReferences, config);
            var documents = pdfReader.read();
            var textSplitter = new TokenTextSplitter(300, 50, 10, 500, false);
            var cleanedDocuments = new ArrayList<Document>();

            for (Document doc : documents) {

                String cleaned = doc.getFormattedContent()
                        .replaceAll("(?i)^.*caliper.*measurement.*$", "")
                        .replaceAll("(?i)^.*honeywell.*$", "") // "Honeywell" em headers/footers
                        .replaceAll("(?i)^.*rev\\s*\\d+.*$", "") // linhas com "Rev 01", etc
                        .replaceAll("(?i)^.*page.*\\d+.*$", "") // "Page x"
                        .replaceAll("(?i)^.*copyright.*$", "") // copyright
                        .replaceAll("(?i)^.*confidentiality.*$", "") // confidentiality headers
                        .replaceAll("(?i)^.*trademarks.*$", "") // etc
                        .replaceAll("(?i)^.*contents.*$", "") // índice
                        .replaceAll("(?m)^\\s*$", "") // remove linhas vazias
                        .trim();

                // Só adiciona se ainda tiver conteúdo
                if (!cleaned.isEmpty()) {
                    cleanedDocuments.add(new Document(cleaned, doc.getMetadata()));
                }
            }
            vectorStore.accept(textSplitter.apply(cleanedDocuments));
            log.info("Application is ready");
        };
    }
}
