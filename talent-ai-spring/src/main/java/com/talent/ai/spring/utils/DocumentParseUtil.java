package com.talent.ai.spring.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * @author watson
 * @date 2026年/11月/05日/13:12
 */
@Component
public class DocumentParseUtil {


    public List<Document> parse(Resource resource, String suffix) {
        DocumentReader reader = switch (suffix) {
            case "pdf", "doc", "docx", "txt", "text" -> new TikaDocumentReader(resource);

            case "md", "markdown" -> new MarkdownDocumentReader(
                    resource,
                    MarkdownDocumentReaderConfig.defaultConfig()
            );

            default -> throw new IllegalArgumentException("不支持的文件格式: " + suffix);
        };
        return reader.get();
    }
}
