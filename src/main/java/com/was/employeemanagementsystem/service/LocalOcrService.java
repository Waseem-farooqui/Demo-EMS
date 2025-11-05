package com.was.employeemanagementsystem.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local OCR Service using Tesseract
 * This runs entirely on your server without external API dependencies
 */
@Service
@Slf4j
public class LocalOcrService {

    @Value("${ocr.tesseract.datapath:#{null}}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language:eng}")
    private String tesseractLanguage;

    @Value("${ocr.local.enabled:true}")
    private boolean localOcrEnabled;

    private Tesseract tesseract;

    @PostConstruct
    public void init() {
        if (!localOcrEnabled) {
            log.info("🔴 Local OCR is disabled");
            return;
        }

        try {
            tesseract = new Tesseract();

            // Try to auto-detect Tesseract installation
            if (tesseractDataPath == null || tesseractDataPath.isEmpty()) {
                tesseractDataPath = autoDetectTesseractPath();
            }

            if (tesseractDataPath != null) {
                tesseract.setDatapath(tesseractDataPath);
                log.info("✓ Tesseract data path set to: {}", tesseractDataPath);
            } else {
                log.warn("⚠ Tesseract data path not configured - will use system default");
            }

            tesseract.setLanguage(tesseractLanguage);
            tesseract.setPageSegMode(1); // Auto page segmentation with OSD
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine

            log.info("✅ Local OCR (Tesseract) initialized successfully");
            log.info("   Language: {}", tesseractLanguage);
            log.info("   Data path: {}", tesseractDataPath != null ? tesseractDataPath : "system default");

        } catch (Exception e) {
            log.error("✗ Failed to initialize Tesseract: {}", e.getMessage());
            log.warn("⚠ Local OCR will not be available. Please install Tesseract OCR.");
            tesseract = null;
        }
    }

    /**
     * Auto-detect Tesseract installation path on Windows
     */
    private String autoDetectTesseractPath() {
        String[] possiblePaths = {
            "C:\\Program Files\\Tesseract-OCR\\tessdata",
            "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
            System.getenv("TESSDATA_PREFIX"),
            "C:\\Tesseract-OCR\\tessdata"
        };

        for (String path : possiblePaths) {
            if (path != null && Files.exists(Path.of(path))) {
                log.info("✓ Auto-detected Tesseract at: {}", path);
                return path;
            }
        }

        log.warn("⚠ Could not auto-detect Tesseract installation");
        return null;
    }

    /**
     * Check if local OCR is available
     */
    public boolean isAvailable() {
        return localOcrEnabled && tesseract != null;
    }

    /**
     * Extract text from an image file using Tesseract
     */
    public String extractTextFromImage(MultipartFile file) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Local OCR is not available");
        }

        log.info("🔍 Extracting text from image using Tesseract: {}", file.getOriginalFilename());

        try {
            // Read image
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IOException("Failed to read image file");
            }

            // Perform OCR
            String text = tesseract.doOCR(image);

            log.info("✓ Extracted {} characters from image", text != null ? text.length() : 0);
            return text != null ? text : "";

        } catch (TesseractException e) {
            log.error("✗ Tesseract OCR failed: {}", e.getMessage());
            throw new IOException("OCR processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from PDF by rendering pages as images and OCR
     */
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Local OCR is not available");
        }

        log.info("📄 Extracting text from PDF using Tesseract: {}", file.getOriginalFilename());
        StringBuilder allText = new StringBuilder();

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            int pageCount = document.getNumberOfPages();
            log.info("📄 PDF has {} page(s)", pageCount);

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // Process each page
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                log.info("🔍 Processing PDF page {} of {}", pageIndex + 1, pageCount);

                try {
                    // Render page at 300 DPI for good quality
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);

                    // Perform OCR on the page
                    String pageText = tesseract.doOCR(image);

                    if (pageText != null && !pageText.trim().isEmpty()) {
                        allText.append(pageText).append("\n\n");
                        log.info("✓ Extracted {} characters from page {}", pageText.length(), pageIndex + 1);
                    } else {
                        log.warn("⚠ No text extracted from page {}", pageIndex + 1);
                    }

                } catch (TesseractException e) {
                    log.error("✗ OCR failed for page {}: {}", pageIndex + 1, e.getMessage());
                    // Continue with next page
                }
            }

            String result = allText.toString().trim();
            log.info("✅ Total extracted {} characters from {} pages", result.length(), pageCount);
            return result;

        } catch (Exception e) {
            log.error("✗ PDF processing failed: {}", e.getMessage());
            throw new IOException("Failed to process PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from ONLY the first page of a PDF (optimized for contracts)
     * This is faster than processing all pages when data is on the first page
     */
    public String extractTextFromFirstPage(MultipartFile file) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Local OCR is not available");
        }

        log.info("📄 Extracting text from FIRST PAGE ONLY of PDF: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            int pageCount = document.getNumberOfPages();
            log.info("📄 PDF has {} page(s), processing only first page", pageCount);

            if (pageCount == 0) {
                log.warn("⚠ PDF has no pages");
                return "";
            }

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // Process ONLY the first page (index 0)
            log.info("🔍 Processing first page only");

            try {
                // Render first page at 300 DPI for good quality
                BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);

                // Perform OCR on the first page
                String pageText = tesseract.doOCR(image);

                if (pageText != null && !pageText.trim().isEmpty()) {
                    log.info("✅ Extracted {} characters from first page", pageText.length());
                    return pageText.trim();
                } else {
                    log.warn("⚠ No text extracted from first page");
                    return "";
                }

            } catch (TesseractException e) {
                log.error("✗ OCR failed for first page: {}", e.getMessage());
                throw new IOException("OCR processing failed: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("✗ PDF first page processing failed: {}", e.getMessage());
            throw new IOException("Failed to process PDF first page: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from BufferedImage using Tesseract
     */
    public String extractTextFromImage(BufferedImage image) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Local OCR is not available");
        }

        try {
            String text = tesseract.doOCR(image);
            return text != null ? text : "";
        } catch (TesseractException e) {
            log.error("✗ Tesseract OCR failed: {}", e.getMessage());
            throw new IOException("OCR processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from a file (auto-detect type)
     */
    public String extractText(MultipartFile file) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Local OCR is not available");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("File has no name");
        }

        String lowercaseFilename = filename.toLowerCase();

        if (lowercaseFilename.endsWith(".pdf")) {
            return extractTextFromPdf(file);
        } else if (lowercaseFilename.matches(".*\\.(jpg|jpeg|png|tif|tiff|bmp|gif)$")) {
            return extractTextFromImage(file);
        } else {
            throw new IOException("Unsupported file type: " + filename);
        }
    }

    /**
     * Get OCR engine information
     */
    public String getEngineInfo() {
        if (!isAvailable()) {
            return "Local OCR is not available";
        }

        return String.format("Tesseract OCR - Language: %s, Data path: %s",
                tesseractLanguage,
                tesseractDataPath != null ? tesseractDataPath : "system default");
    }
}

