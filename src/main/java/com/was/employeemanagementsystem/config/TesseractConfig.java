package com.was.employeemanagementsystem.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class TesseractConfig {

    @Value("${ocr.tesseract.datapath:C:\\Users\\waseem.uddin\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language:eng}")
    private String tesseractLanguage;

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();

        // Set TESSDATA_PREFIX environment variable if not already set
        // This is required for Tesseract native library to find language data
        String tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        if (tessdataPrefix == null || tessdataPrefix.isEmpty()) {
            // Try to set it from the configured path
            if (tesseractDataPath != null && !tesseractDataPath.isEmpty()) {
                System.setProperty("TESSDATA_PREFIX", tesseractDataPath);
            }
        }

        // Set the Tesseract data path from configuration
        tesseract.setDatapath(tesseractDataPath);

        // Verify tessdata directory exists
        File tessdataDir = new File(tesseractDataPath);
        if (!tessdataDir.exists()) {
            System.err.println("⚠️ WARNING: Tesseract tessdata directory not found at: " + tesseractDataPath);
            System.err.println("Please install Tesseract OCR or update ocr.tesseract.datapath in application.properties");

            // Try alternative paths (Linux/Docker first, then Windows)
            String[] alternativePaths = {
                // Linux/Docker paths (most common)
                "/usr/share/tesseract-ocr/5/tessdata",        // Tesseract 5.x (Debian/Ubuntu)
                "/usr/share/tesseract-ocr/4.00/tessdata",     // Tesseract 4.x
                "/usr/share/tesseract-ocr/tessdata",          // Generic
                "/usr/local/share/tessdata",                   // Alternative location
                "/usr/share/tessdata",                        // System-wide
                // Windows paths
                "C:\\Users\\waseem.uddin\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata",
                "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata",
                "C:\\Program Files\\Tesseract-OCR\\tessdata",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata"
            };

            for (String altPath : alternativePaths) {
                File altDir = new File(altPath);
                if (altDir.exists()) {
                    System.out.println("✓ Found tessdata at: " + altPath);
                    tesseract.setDatapath(altPath);
                    break;
                }
            }
        } else {
            System.out.println("✓ Tesseract tessdata found at: " + tesseractDataPath);
        }

        // Set language
        tesseract.setLanguage(tesseractLanguage);

        // Set OCR Engine Mode (OEM)
        // 1 = Neural nets LSTM engine only (better for modern text/tables)
        tesseract.setOcrEngineMode(1);

        // Set Page Segmentation Mode (PSM)
        // 6 = Assume a single uniform block of text (better for tables with consistent structure)
        tesseract.setPageSegMode(6);

        // Additional Tesseract variables for better table/colored cell recognition
        tesseract.setVariable("tessedit_char_whitelist",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 -:./");
        tesseract.setVariable("preserve_interword_spaces", "1");

        // Improve accuracy for table detection
        tesseract.setVariable("textord_heavy_nr", "1"); // Better table structure detection
        tesseract.setVariable("textord_tablefind_recognize_tables", "1"); // Recognize tables

        System.out.println("✓ Tesseract OCR configured successfully");
        System.out.println("  - Language: " + tesseractLanguage);
        System.out.println("  - Engine Mode: 1 (LSTM Neural Network)");
        System.out.println("  - Page Segmentation: 6 (Uniform text block)");
        System.out.println("  - Table recognition: ENABLED");
        System.out.println("  - Preserve spaces: ENABLED");

        return tesseract;
    }
}

