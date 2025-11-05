package com.was.employeemanagementsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OcrService {

    private final Tika tika = new Tika();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LocalOcrService localOcrService;

    // Cloud OCR configuration
    @Value("${ocr.cloud.enabled:false}")
    private boolean cloudOcrEnabled;

    @Value("${ocr.api.key:K87899142388957}")
    private String ocrApiKey;

    @Value("${ocr.api.url:https://api.ocr.space/parse/image}")
    private String ocrApiUrl;

    public OcrService(LocalOcrService localOcrService) {
        this.localOcrService = localOcrService;
    }

    /**
     * Main entry point for text extraction
     * Priority: Local OCR (Tesseract) → Cloud OCR (API) → Tika fallback
     */
    public String extractTextFromDocument(MultipartFile file) throws IOException, TikaException {
        return extractTextFromDocument(file, false);
    }

    /**
     * Extract text from document with option to process only first page (for contracts)
     * @param file The document file
     * @param firstPageOnly If true, only extract from first page (useful for contracts)
     */
    public String extractTextFromDocument(MultipartFile file, boolean firstPageOnly) throws IOException, TikaException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        log.info("📄 Extracting text from document: {} (type: {}, firstPageOnly: {})", filename, contentType, firstPageOnly);
        log.info("   Local OCR available: {}, Cloud OCR enabled: {}",
                 localOcrService.isAvailable(), cloudOcrEnabled);

        // Method 1: Try Local OCR first (RECOMMENDED for controlled environments)
        if (localOcrService.isAvailable()) {
            try {
                log.info("🔧 Attempting LOCAL OCR (Tesseract)...");
                String result;
                if (firstPageOnly && contentType != null && contentType.equals("application/pdf")) {
                    result = localOcrService.extractTextFromFirstPage(file);
                } else {
                    result = localOcrService.extractText(file);
                }

                if (result != null && !result.trim().isEmpty()) {
                    log.info("✅ LOCAL OCR successful - extracted {} characters", result.length());
                    return result;
                } else {
                    log.warn("⚠ Local OCR returned empty result");
                }
            } catch (Exception e) {
                log.warn("⚠ Local OCR failed: {} - Trying fallback methods", e.getMessage());
            }
        } else {
            log.info("⚠ Local OCR not available");
        }

        // Method 2: Try Cloud OCR if enabled
        if (cloudOcrEnabled && contentType != null && contentType.startsWith("image/")) {
            try {
                log.info("☁️ Attempting CLOUD OCR (API)...");
                String result = extractTextFromImageViaApi(file);

                if (result != null && !result.trim().isEmpty()) {
                    log.info("✅ CLOUD OCR successful - extracted {} characters", result.length());
                    return result;
                }
            } catch (Exception e) {
                log.warn("⚠ Cloud OCR failed: {} - Trying Tika fallback", e.getMessage());
            }
        }

        // Method 3: Fallback to Tika (for PDFs with selectable text)
        log.info("📝 Attempting Tika text extraction...");
        return extractTextWithTika(file, firstPageOnly);
    }

    /**
     * Extract text from image using OCR.space cloud API
     * No installation required - works on any platform (Windows, Linux, Docker)
     */
    private String extractTextFromImageViaApi(MultipartFile file) throws IOException {
        log.info("🔍 Attempting cloud OCR on image: {} (size: {} bytes)",
                 file.getOriginalFilename(), file.getSize());

        // Try Method 1: Multipart file upload
        try {
            String result = tryMultipartOcr(file);
            if (result != null && !result.trim().isEmpty()) {
                log.info("✓ Cloud OCR successful via multipart upload - {} characters", result.length());
                return result;
            }
        } catch (Exception e) {
            log.warn("⚠ Multipart OCR failed: {}", e.getMessage());
        }

        // Try Method 2: Base64 encoded upload (more reliable)
        try {
            log.info("🔄 Trying base64 encoding method...");
            String result = tryBase64Ocr(file);
            if (result != null && !result.trim().isEmpty()) {
                log.info("✓ Cloud OCR successful via base64 - {} characters", result.length());
                return result;
            }
        } catch (Exception e) {
            log.warn("⚠ Base64 OCR failed: {}", e.getMessage());
        }

        // Fallback: Try with Tika (works for some image formats with metadata)
        log.info("🔄 Attempting Tika fallback...");
        try {
            String result = extractTextWithTika(file, false); // false = process all pages (though this is for images)
            if (result != null && !result.trim().isEmpty()) {
                log.info("✓ Tika fallback successful - {} characters", result.length());
                return result;
            }
        } catch (Exception e) {
            log.error("✗ All OCR methods failed", e);
        }

        throw new IOException("Unable to extract text from image. All OCR methods failed. " +
                            "Please check: 1) Image quality, 2) Internet connection, 3) API key validity");
    }

    private String tryMultipartOcr(MultipartFile file) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("apikey", ocrApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });
        body.add("language", "eng");
        body.add("isOverlayRequired", "false");
        body.add("detectOrientation", "true");
        body.add("scale", "true");
        body.add("OCREngine", "2"); // Engine 2 for better accuracy

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        log.debug("Sending multipart OCR request to: {}", ocrApiUrl);
        ResponseEntity<String> response = restTemplate.postForEntity(ocrApiUrl, requestEntity, String.class);

        log.debug("OCR API response status: {}", response.getStatusCode());
        log.debug("OCR API response body: {}", response.getBody());

        return parseOcrResponse(response.getBody());
    }

    private String tryBase64Ocr(MultipartFile file) throws Exception {
        // Convert image to base64
        byte[] imageBytes = file.getBytes();
        String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("apikey", ocrApiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("base64Image", "data:image/jpeg;base64," + base64Image);
        body.add("language", "eng");
        body.add("isOverlayRequired", "false");
        body.add("detectOrientation", "true");
        body.add("scale", "true");
        body.add("OCREngine", "2");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        log.debug("Sending base64 OCR request to: {}", ocrApiUrl);
        ResponseEntity<String> response = restTemplate.postForEntity(ocrApiUrl, requestEntity, String.class);

        log.debug("OCR API response status: {}", response.getStatusCode());

        return parseOcrResponse(response.getBody());
    }

    private String parseOcrResponse(String responseBody) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(responseBody);

        log.debug("Parsing OCR response: {}", responseBody.substring(0, Math.min(200, responseBody.length())));

        if (jsonResponse.has("IsErroredOnProcessing") && jsonResponse.get("IsErroredOnProcessing").asBoolean()) {
            String errorMessage = jsonResponse.has("ErrorMessage")
                ? jsonResponse.get("ErrorMessage").asText()
                : jsonResponse.has("ErrorDetails") ? jsonResponse.get("ErrorDetails").asText() : "Unknown error";
            log.error("❌ OCR API returned error: {}", errorMessage);
            throw new IOException("OCR API error: " + errorMessage);
        }

        if (jsonResponse.has("ParsedResults") && jsonResponse.get("ParsedResults").isArray()
            && jsonResponse.get("ParsedResults").size() > 0) {

            JsonNode firstResult = jsonResponse.get("ParsedResults").get(0);

            if (firstResult.has("ErrorMessage") && !firstResult.get("ErrorMessage").asText().isEmpty()) {
                String error = firstResult.get("ErrorMessage").asText();
                log.warn("⚠ OCR result contains error: {}", error);
            }

            if (firstResult.has("ParsedText")) {
                String extractedText = firstResult.get("ParsedText").asText();

                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    log.info("✓ Successfully extracted {} characters", extractedText.length());
                    log.debug("📄 Extracted text preview:\n{}",
                             extractedText.substring(0, Math.min(500, extractedText.length())));
                    return extractedText;
                }
            }
        }

        log.warn("⚠ No parsed text found in response");
        return "";
    }

    private String extractTextWithTika(MultipartFile file, boolean firstPageOnly) throws IOException, TikaException {
        String contentType = file.getContentType();

        // Check if it's a PDF - PDFs often contain images that need OCR
        if (contentType != null && contentType.equals("application/pdf")) {
            log.info("📄 Detected PDF file: {}", file.getOriginalFilename());

            // First try: Extract text directly with Tika (works for text-based PDFs)
            try (InputStream inputStream = file.getInputStream()) {
                String text = tika.parseToString(inputStream);
                if (text != null && text.trim().length() > 50) {
                    log.info("✓ Tika extracted {} characters from PDF", text.length());
                    return text;
                }
                log.warn("⚠ Tika extracted minimal text ({} chars) - PDF likely contains images", text.length());
            } catch (Exception e) {
                log.warn("⚠ Tika text extraction failed: {}", e.getMessage());
            }

            // Second try: Extract images from PDF and run OCR on them
            log.info("🔄 Attempting to extract images from PDF for OCR...");
            return extractTextFromPdfImages(file, firstPageOnly);
        }

        // For non-PDF files, use Tika normally
        try (InputStream inputStream = file.getInputStream()) {
            log.info("Extracting text with Tika from: {}", file.getOriginalFilename());
            String text = tika.parseToString(inputStream);
            log.info("✓ Text extraction complete - {} characters extracted", text.length());

            if (!text.trim().isEmpty()) {
                log.debug("Extracted text preview (first 500 chars): {}",
                    text.length() > 500 ? text.substring(0, 500) : text);
            } else {
                log.warn("⚠ No text extracted from document");
            }

            return text;
        } catch (IOException | TikaException e) {
            log.error("✗ Failed to extract text from document: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Extract images from PDF and run OCR on them
     * This handles PDFs that contain scanned passport images
     * @param file The PDF file
     * @param firstPageOnly If true, only extract from first page (useful for contracts)
     */
    private String extractTextFromPdfImages(MultipartFile file, boolean firstPageOnly) throws IOException {
        StringBuilder allText = new StringBuilder();

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            int pageCount = document.getNumberOfPages();
            log.info("📄 PDF has {} page(s){}", pageCount, firstPageOnly ? ", processing only first page" : "");

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // Determine how many pages to process
            int pagesToProcess = firstPageOnly ? Math.min(1, pageCount) : pageCount;

            // Process pages
            for (int pageIndex = 0; pageIndex < pagesToProcess; pageIndex++) {
                log.info("🔍 Processing PDF page {} of {}", pageIndex + 1, pageCount);

                try {
                    // Start with 200 DPI (good balance between quality and size)
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);

                    // Convert BufferedImage to byte array with compression
                    byte[] imageBytes = compressImage(image, 0.85f); // 85% quality

                    log.info("✓ Rendered page {} as image ({} bytes)", pageIndex + 1, imageBytes.length);

                    // Check if image is too large (over 900KB to leave margin)
                    if (imageBytes.length > 900 * 1024) {
                        log.warn("⚠ Image too large ({} KB), compressing further...", imageBytes.length / 1024);
                        imageBytes = compressImage(image, 0.70f); // Reduce to 70% quality
                        log.info("✓ Compressed to {} KB", imageBytes.length / 1024);
                    }

                    // If still too large, resize the image
                    if (imageBytes.length > 900 * 1024) {
                        log.warn("⚠ Still too large, resizing image...");
                        BufferedImage resized = resizeImage(image, 0.7); // 70% of original size
                        imageBytes = compressImage(resized, 0.80f);
                        log.info("✓ Resized and compressed to {} KB", imageBytes.length / 1024);
                    }

                    // Run OCR on the image
                    String pageText = runOcrOnImageBytes(imageBytes, "page" + (pageIndex + 1) + ".png");

                    if (pageText != null && !pageText.trim().isEmpty()) {
                        allText.append(pageText).append("\n");
                        log.info("✓ Extracted {} characters from page {}", pageText.length(), pageIndex + 1);
                    } else {
                        log.warn("⚠ No text extracted from page {}", pageIndex + 1);
                    }

                } catch (Exception e) {
                    log.error("✗ Failed to process page {}: {}", pageIndex + 1, e.getMessage());
                }
            }

            String finalText = allText.toString().trim();

            if (!finalText.isEmpty()) {
                log.info("✅ Successfully extracted {} total characters from PDF images", finalText.length());
                log.debug("📄 Extracted text preview:\n{}",
                         finalText.substring(0, Math.min(500, finalText.length())));
                return finalText;
            } else {
                log.error("❌ No text could be extracted from PDF images");
                throw new IOException("Failed to extract text from PDF images. The PDF may be encrypted or corrupted.");
            }

        } catch (IOException e) {
            log.error("✗ Failed to process PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to extract images from PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Run OCR on image bytes
     */
    private String runOcrOnImageBytes(byte[] imageBytes, String filename) throws IOException {
        log.debug("Running OCR on image bytes for: {} ({} KB)", filename, imageBytes.length / 1024);

        // Ensure image is under size limit
        if (imageBytes.length > 900 * 1024) {
            log.warn("⚠ Image too large ({} KB), attempting to compress...", imageBytes.length / 1024);
            try {
                // Read the image and recompress it
                BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
                if (img != null) {
                    imageBytes = compressImage(img, 0.70f);
                    log.info("✓ Compressed to {} KB", imageBytes.length / 1024);

                    // If still too large, resize
                    if (imageBytes.length > 900 * 1024) {
                        BufferedImage resized = resizeImage(img, 0.7);
                        imageBytes = compressImage(resized, 0.80f);
                        log.info("✓ Resized to {} KB", imageBytes.length / 1024);
                    }
                }
            } catch (Exception e) {
                log.error("✗ Failed to compress image: {}", e.getMessage());
                throw new IOException("Image too large and compression failed");
            }
        }

        // Try Method 1: Base64 encoding (more reliable for generated images)
        try {
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("apikey", ocrApiKey);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("base64Image", "data:image/jpeg;base64," + base64Image);
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("detectOrientation", "true");
            body.add("scale", "true");
            body.add("OCREngine", "2");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(ocrApiUrl, requestEntity, String.class);
            String result = parseOcrResponse(response.getBody());

            if (result != null && !result.trim().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("⚠ Base64 OCR failed for {}: {}", filename, e.getMessage());
        }

        // Try Method 2: Multipart upload as fallback
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("apikey", ocrApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("detectOrientation", "true");
            body.add("scale", "true");
            body.add("OCREngine", "2");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(ocrApiUrl, requestEntity, String.class);

            return parseOcrResponse(response.getBody());
        } catch (Exception e) {
            log.error("✗ All OCR methods failed for {}: {}", filename, e.getMessage());
            return "";
        }
    }

    public Map<String, Object> extractPassportInformation(String text) {
        log.info("Extracting passport information from text");
        log.debug("Input text length: {} characters", text != null ? text.length() : 0);

        // Log the full extracted text for debugging
        if (text != null && !text.trim().isEmpty()) {
            log.info("📄 ========== EXTRACTED TEXT START ==========");
            log.info("{}", text);
            log.info("📄 ========== EXTRACTED TEXT END ==========");
        }

        Map<String, Object> info = new HashMap<>();

        if (text == null || text.trim().isEmpty()) {
            log.warn("⚠ Empty text provided for passport extraction");
            return info;
        }

        try {
            // === UNIVERSAL PASSPORT NUMBER EXTRACTION ===
            // ...existing code...

            // Pattern 1: Standard labeled format (most countries)
            Pattern passportPattern1 = Pattern.compile(
                "(?:Passport|Passeport|Pasaporte|Reisepass|Passaporto)\\s*(?:No|Number|Nr|N°)?\\s*:?\\s*([A-Z0-9]{6,12})",
                Pattern.CASE_INSENSITIVE);

            // Pattern 2: Letter(s) + Numbers (Pakistani: MZ7482170, Indian: J1234567, UK: 123456789)
            Pattern passportPattern2 = Pattern.compile(
                "\\b([A-Z]{1,3}[0-9]{6,9})\\b");

            // Pattern 3: All numbers (China, Japan: 9 digits)
            Pattern passportPattern3 = Pattern.compile(
                "(?:Passport|护照)\\s*(?:No|Number)?\\s*:?\\s*([0-9]{8,9})\\b",
                Pattern.CASE_INSENSITIVE);

            // Pattern 4: Letters only + numbers (Germany: varies, France: 2 letters + 7 digits)
            Pattern passportPattern4 = Pattern.compile(
                "\\b([A-Z]{2}\\d{7,8}|[A-Z]\\d{8,9})\\b");

            // Try all patterns
            String docNumber = null;
            Matcher matcher1 = passportPattern1.matcher(text);
            Matcher matcher2 = passportPattern2.matcher(text);
            Matcher matcher3 = passportPattern3.matcher(text);
            Matcher matcher4 = passportPattern4.matcher(text);

            if (matcher1.find()) {
                docNumber = matcher1.group(1).trim();
                log.info("✓ Passport number extracted (labeled format): {}", docNumber);
            } else if (matcher2.find()) {
                docNumber = matcher2.group(1).trim();
                log.info("✓ Passport number extracted (letter-number format): {}", docNumber);
            } else if (matcher3.find()) {
                docNumber = matcher3.group(1).trim();
                log.info("✓ Passport number extracted (numeric format): {}", docNumber);
            } else if (matcher4.find()) {
                docNumber = matcher4.group(1).trim();
                log.info("✓ Passport number extracted (alternative format): {}", docNumber);
            } else {
                log.warn("⚠ Could not extract passport number from text");
            }

            if (docNumber != null) {
                info.put("documentNumber", docNumber);
            }

            // Extract dates
            extractDates(text, info);

            // Debug: Log what dates were found
            log.info("📅 After extractDates - issueDate in map: {}, expiryDate in map: {}, dateOfBirth in map: {}",
                     info.get("issueDate"), info.get("expiryDate"), info.get("dateOfBirth"));

            // === UNIVERSAL NATIONALITY EXTRACTION ===
            // Support for major countries and their language variants

            Pattern nationalityPattern = Pattern.compile(
                "(?:Nationality|National|Nationalité|Nacionalidad|Staatsangehörigkeit|Nazionalità)\\s*:?\\s*([A-Za-zÀ-ÿ\\s]+?)(?:\\n|\\s{2,}|Date|DOB|Birth|$)",
                Pattern.CASE_INSENSITIVE);
            Matcher nationalityMatcher = nationalityPattern.matcher(text);
            if (nationalityMatcher.find()) {
                String nationality = nationalityMatcher.group(1).trim();
                info.put("nationality", nationality);
                log.info("✓ Nationality extracted: {}", nationality);
            } else {
                // Fallback: detect by country keywords
                detectNationalityByKeywords(text, info);
            }

            // === UNIVERSAL ISSUING COUNTRY DETECTION ===
            detectIssuingCountry(text, info);

            // === UNIVERSAL NAME EXTRACTION ===
            // Supports various name formats and scripts

            // Pattern 1: Labeled name fields
            Pattern namePattern1 = Pattern.compile(
                "(?:Name|Surname|Given\\s*Names?|Full\\s*Name|Nom|Apellidos?|Nome|Nachname)\\s*:?\\s*([A-ZÀ-ÿ][A-ZÀ-ÿ\\s]{2,50}?)(?:\\n|Date|DOB|Birth|Passport|\\d{2})",
                Pattern.CASE_INSENSITIVE);
            Matcher nameMatcher1 = namePattern1.matcher(text);

            // Pattern 2: Sequences of capital letters (likely names in passports)
            Pattern namePattern2 = Pattern.compile(
                "\\b([A-ZÀ-Ÿ]{3,}(?:\\s+[A-ZÀ-Ÿ]{2,}){1,4})\\b");

            if (nameMatcher1.find()) {
                String fullName = nameMatcher1.group(1).trim();
                info.put("fullName", fullName);
                log.info("✓ Full name extracted (labeled): {}", fullName);
            } else {
                Matcher nameMatcher2 = namePattern2.matcher(text);
                if (nameMatcher2.find()) {
                    String fullName = nameMatcher2.group(1).trim();
                    // Filter out common non-name words
                    if (!isCommonPassportKeyword(fullName)) {
                        info.put("fullName", fullName);
                        log.info("✓ Full name extracted (caps pattern): {}", fullName);
                    }
                }
            }

            log.info("✓ Passport information extraction complete - {} fields extracted", info.size());
            log.info("📋 Final extracted info map:");
            log.info("   - documentNumber: {}", info.get("documentNumber"));
            log.info("   - issueDate: {}", info.get("issueDate"));
            log.info("   - expiryDate: {}", info.get("expiryDate"));
            log.info("   - dateOfBirth: {}", info.get("dateOfBirth"));
            log.info("   - nationality: {}", info.get("nationality"));
            log.info("   - issuingCountry: {}", info.get("issuingCountry"));
            log.info("   - fullName: {}", info.get("fullName"));

            if (info.isEmpty()) {
                log.warn("⚠ No passport information could be extracted - OCR quality may be poor");
                log.debug("Text sample for debugging: {}", text.length() > 200 ? text.substring(0, 200) : text);
            }
        } catch (Exception e) {
            log.error("✗ Error extracting passport information", e);
        }

        return info;
    }

    /**
     * Detects nationality by looking for country-specific keywords
     */
    private void detectNationalityByKeywords(String text, Map<String, Object> info) {
        String textUpper = text.toUpperCase();

        // Map of country keywords to nationality
        if (textUpper.contains("PAKISTAN") || textUpper.contains("PAK") || textUpper.contains("اسلامی جمہوریہ پاکستان")) {
            info.put("nationality", "Pakistani");
            log.info("✓ Nationality detected by keyword: Pakistani");
        } else if (textUpper.contains("INDIA") || textUpper.contains("IND") || textUpper.contains("भारत")) {
            info.put("nationality", "Indian");
            log.info("✓ Nationality detected by keyword: Indian");
        } else if (textUpper.contains("UNITED KINGDOM") || textUpper.contains("BRITISH") || textUpper.contains("GBR")) {
            info.put("nationality", "British");
            log.info("✓ Nationality detected by keyword: British");
        } else if (textUpper.contains("UNITED STATES") || textUpper.contains("USA") || textUpper.contains("AMERICAN")) {
            info.put("nationality", "American");
            log.info("✓ Nationality detected by keyword: American");
        } else if (textUpper.contains("CANADA") || textUpper.contains("CAN") || textUpper.contains("CANADIAN")) {
            info.put("nationality", "Canadian");
            log.info("✓ Nationality detected by keyword: Canadian");
        } else if (textUpper.contains("AUSTRALIA") || textUpper.contains("AUS") || textUpper.contains("AUSTRALIAN")) {
            info.put("nationality", "Australian");
            log.info("✓ Nationality detected by keyword: Australian");
        } else if (textUpper.contains("CHINA") || textUpper.contains("CHN") || textUpper.contains("中国")) {
            info.put("nationality", "Chinese");
            log.info("✓ Nationality detected by keyword: Chinese");
        } else if (textUpper.contains("JAPAN") || textUpper.contains("JPN") || textUpper.contains("日本")) {
            info.put("nationality", "Japanese");
            log.info("✓ Nationality detected by keyword: Japanese");
        } else if (textUpper.contains("GERMANY") || textUpper.contains("DEU") || textUpper.contains("DEUTSCHLAND")) {
            info.put("nationality", "German");
            log.info("✓ Nationality detected by keyword: German");
        } else if (textUpper.contains("FRANCE") || textUpper.contains("FRA") || textUpper.contains("FRANÇAISE")) {
            info.put("nationality", "French");
            log.info("✓ Nationality detected by keyword: French");
        }
        // Add more countries as needed
    }

    /**
     * Detects issuing country from passport text
     */
    private void detectIssuingCountry(String text, Map<String, Object> info) {
        String textUpper = text.toUpperCase();

        if (textUpper.contains("PAKISTAN") || textUpper.contains("PAK") ||
            textUpper.contains("ISLAMIC REPUBLIC OF PAKISTAN")) {
            info.put("issuingCountry", "Pakistan");
            log.info("✓ Issuing country detected: Pakistan");
        } else if (textUpper.contains("INDIA") || textUpper.contains("IND") ||
                  textUpper.contains("REPUBLIC OF INDIA") || textUpper.contains("भारत गणराज्य")) {
            info.put("issuingCountry", "India");
            log.info("✓ Issuing country detected: India");
        } else if (textUpper.contains("UNITED KINGDOM") || textUpper.contains("UK") ||
                  textUpper.contains("GBR") || textUpper.contains("ENGLAND")) {
            info.put("issuingCountry", "United Kingdom");
            log.info("✓ Issuing country detected: United Kingdom");
        } else if (textUpper.contains("UNITED STATES") || textUpper.contains("USA") ||
                  textUpper.contains("UNITED STATES OF AMERICA")) {
            info.put("issuingCountry", "United States");
            log.info("✓ Issuing country detected: United States");
        } else if (textUpper.contains("CANADA") || textUpper.contains("CAN")) {
            info.put("issuingCountry", "Canada");
            log.info("✓ Issuing country detected: Canada");
        } else if (textUpper.contains("AUSTRALIA") || textUpper.contains("AUS") ||
                  textUpper.contains("COMMONWEALTH OF AUSTRALIA")) {
            info.put("issuingCountry", "Australia");
            log.info("✓ Issuing country detected: Australia");
        } else if (textUpper.contains("CHINA") || textUpper.contains("CHN") ||
                  textUpper.contains("PEOPLE'S REPUBLIC OF CHINA") || textUpper.contains("中华人民共和国")) {
            info.put("issuingCountry", "China");
            log.info("✓ Issuing country detected: China");
        } else if (textUpper.contains("JAPAN") || textUpper.contains("JPN") || textUpper.contains("日本国")) {
            info.put("issuingCountry", "Japan");
            log.info("✓ Issuing country detected: Japan");
        } else if (textUpper.contains("GERMANY") || textUpper.contains("DEU") ||
                  textUpper.contains("BUNDESREPUBLIK DEUTSCHLAND")) {
            info.put("issuingCountry", "Germany");
            log.info("✓ Issuing country detected: Germany");
        } else if (textUpper.contains("FRANCE") || textUpper.contains("FRA") ||
                  textUpper.contains("RÉPUBLIQUE FRANÇAISE")) {
            info.put("issuingCountry", "France");
            log.info("✓ Issuing country detected: France");
        }
        // Add more countries as needed
    }

    /**
     * Filters out common passport keywords that are not names
     */
    private boolean isCommonPassportKeyword(String text) {
        String upper = text.toUpperCase();
        return upper.contains("PASSPORT") || upper.contains("REPUBLIC") ||
               upper.contains("NATIONALITY") || upper.contains("SURNAME") ||
               upper.contains("GIVEN") || upper.contains("DATE") ||
               upper.contains("BIRTH") || upper.contains("SEX") ||
               upper.contains("PLACE") || upper.contains("ISSUE") ||
               upper.contains("EXPIRY") || upper.contains("TYPE") ||
               upper.contains("CODE") || upper.contains("AUTHORITY");
    }

    public Map<String, Object> extractVisaInformation(String text) {
        log.info("📋 Extracting VISA information");
        log.debug("Full extracted text length: {} characters", text != null ? text.length() : 0);
        log.debug("Full extracted text:\n{}", text);  // Show FULL text for debugging

        Map<String, Object> info = new HashMap<>();

        // === UK HOME OFFICE VISA SPECIFIC EXTRACTION ===

        // Extract Name (appears at the top of UK VISA, before work permission text)
        // Pattern: Look for name in ALL CAPS before "They have permission"
        Pattern ukNamePattern = Pattern.compile(
            "(?:Home\\s*Office|dl\\s*Home\\s*Office)\\s+([A-Z][A-Z\\s]{2,50}?)\\s+(?:They\\s+have\\s+permission|Conditions)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher ukNameMatcher = ukNamePattern.matcher(text);
        if (ukNameMatcher.find()) {
            String fullName = ukNameMatcher.group(1).trim();
            // Clean up the name (remove extra spaces)
            fullName = fullName.replaceAll("\\s+", " ");
            info.put("fullName", fullName);
            log.info("✓ Full name extracted: {}", fullName);
        } else {
            // Fallback: Look for name in caps near the beginning
            Pattern namePatternFallback = Pattern.compile(
                "^[\\s\\S]{0,200}([A-Z]{2,}\\s+[A-Z]{2,}(?:\\s+[A-Z]{2,})?)",
                Pattern.MULTILINE
            );
            Matcher nameFallbackMatcher = namePatternFallback.matcher(text);
            if (nameFallbackMatcher.find()) {
                String fullName = nameFallbackMatcher.group(1).trim();
                if (fullName.length() > 5 && fullName.length() < 50) { // Reasonable name length
                    info.put("fullName", fullName);
                    log.info("✓ Full name extracted (fallback): {}", fullName);
                }
            }
        }

        // Extract "permission to work in the UK until [date]"
        Pattern ukWorkPermissionPattern = Pattern.compile(
            "permission\\s+to\\s+work\\s+in\\s+the\\s+UK\\s+until\\s+(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
        );
        Matcher ukWorkMatcher = ukWorkPermissionPattern.matcher(text);
        if (ukWorkMatcher.find()) {
            String dateStr = ukWorkMatcher.group(1).trim();
            log.info("Found UK work permission date: '{}'", dateStr);

            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
            };

            LocalDate expiryDate = parseDate(dateStr, formatters);
            if (expiryDate != null) {
                info.put("expiryDate", expiryDate);
                log.info("✓ Expiry date extracted from work permission: {}", expiryDate);
            }
        } else {
            log.warn("⚠ Could not find work permission date pattern in text");
        }

        // === UK HOME OFFICE DETAILS SECTION ===
        // Extract Company Name, Date of Check, and Reference Number from Details section

        // Extract Company Name - handles interleaved text from multi-column layout
        // Pattern 1: Look for "Company name" followed by text until "Date of check" or "Ltd" or "Limited"
        Pattern companyPattern1 = Pattern.compile(
            "Company\\s*[Nn]ame\\s*(?:\\[\\||[:\\s])*\\s*([A-Za-z0-9][A-Za-z0-9\\s&.,'-]*?(?:Ltd|Limited|LLC|Inc|Corporation|LLP|Plc))",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: Broader pattern that captures text between "Company name" and "Date of check"
        Pattern companyPattern2 = Pattern.compile(
            "Company\\s*[Nn]ame[^A-Za-z0-9]*([A-Za-z][A-Za-z0-9\\s&.,'-]+?)\\s*(?:\\[\\||Date\\s*of|services\\s+Ltd)",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 3: Look for company name in "Details of check" section
        Pattern companyPattern3 = Pattern.compile(
            "Details\\s+of\\s+check.*?Company\\s*[Nn]ame.*?([A-Z][a-zA-Z]+\\s+[a-z]+.*?(?:Ltd|Limited|services))",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        String companyName = null;
        Matcher companyMatcher1 = companyPattern1.matcher(text);
        Matcher companyMatcher2 = companyPattern2.matcher(text);
        Matcher companyMatcher3 = companyPattern3.matcher(text);

        if (companyMatcher1.find()) {
            companyName = companyMatcher1.group(1).trim();
            log.info("✓ Company name extracted (pattern 1): {}", companyName);
        } else if (companyMatcher2.find()) {
            companyName = companyMatcher2.group(1).trim();
            log.info("✓ Company name extracted (pattern 2): {}", companyName);
        } else if (companyMatcher3.find()) {
            companyName = companyMatcher3.group(1).trim();
            log.info("✓ Company name extracted (pattern 3): {}", companyName);
        }

        if (companyName != null) {
            // Clean up company name
            companyName = companyName.replaceAll("\\s+", " ").trim();
            // Remove any trailing artifacts
            companyName = companyName.replaceAll("\\[\\|.*", "").trim();
            info.put("companyName", companyName);
            log.info("✓ Final company name: {}", companyName);
        } else {
            log.warn("⚠ Could not find company name in text");
        }

        // Extract Date of Check - handles interleaved text
        // Pattern 1: Direct "Date of check" followed by date
        Pattern dateOfCheckPattern1 = Pattern.compile(
            "Date\\s*of\\s*[Cc]heck\\s*[:\\s]*(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: Date appears after "Date of check" with possible interference text
        Pattern dateOfCheckPattern2 = Pattern.compile(
            "Date\\s*of\\s*[Cc]heck.*?(\\d{1,2}\\s+[A-Z][a-z]+\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        // Pattern 3: Look for date in numeric format
        Pattern dateOfCheckPattern3 = Pattern.compile(
            "Date\\s*of\\s*[Cc]heck\\s*[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
        );

        String dateStr = null;
        Matcher dateOfCheckMatcher1 = dateOfCheckPattern1.matcher(text);
        Matcher dateOfCheckMatcher2 = dateOfCheckPattern2.matcher(text);
        Matcher dateOfCheckMatcher3 = dateOfCheckPattern3.matcher(text);

        if (dateOfCheckMatcher1.find()) {
            dateStr = dateOfCheckMatcher1.group(1).trim();
            log.info("Found date of check (pattern 1): '{}'", dateStr);
        } else if (dateOfCheckMatcher2.find()) {
            dateStr = dateOfCheckMatcher2.group(1).trim();
            log.info("Found date of check (pattern 2): '{}'", dateStr);
        } else if (dateOfCheckMatcher3.find()) {
            dateStr = dateOfCheckMatcher3.group(1).trim();
            log.info("Found date of check (pattern 3): '{}'", dateStr);
        }

        if (dateStr != null) {
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy")
            };

            LocalDate checkDate = parseDate(dateStr, formatters);
            if (checkDate != null) {
                info.put("dateOfCheck", checkDate);
                log.info("✓ Date of check extracted: {}", checkDate);
            }
        } else {
            log.warn("⚠ Could not find date of check in text");
        }

        // Extract Reference Number - handles interleaved text with multiple patterns
        // Pattern 1: Direct "Reference number" followed by alphanumeric code
        Pattern referencePattern1 = Pattern.compile(
            "Reference\\s*[Nn]umber\\s*[:\\s]*([A-Z0-9]{2,3}-[A-Z0-9]{5,10}-[A-Z0-9]{2,3})",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: Just "Reference" followed by code (for when "number" is on next line)
        Pattern referencePattern2 = Pattern.compile(
            "Reference\\s*(?:[Nn]umber)?\\s*[:\\s]*([A-Z]{2,3}-[A-Z0-9]{5,10}-[A-Z]{2,3})",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 3: Look for UK reference format anywhere in text
        Pattern referencePattern3 = Pattern.compile(
            "\\b([A-Z]{2}-[A-Z0-9]{7,9}-[A-Z]{2})\\b",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 4: More general reference number pattern
        Pattern referencePattern4 = Pattern.compile(
            "Reference.*?([A-Z]{2,3}-[A-Z0-9-]{5,15})",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        String referenceNumber = null;
        Matcher referenceMatcher1 = referencePattern1.matcher(text);
        Matcher referenceMatcher2 = referencePattern2.matcher(text);
        Matcher referenceMatcher3 = referencePattern3.matcher(text);
        Matcher referenceMatcher4 = referencePattern4.matcher(text);

        if (referenceMatcher1.find()) {
            referenceNumber = referenceMatcher1.group(1).trim();
            log.info("✓ Reference number extracted (pattern 1): {}", referenceNumber);
        } else if (referenceMatcher2.find()) {
            referenceNumber = referenceMatcher2.group(1).trim();
            log.info("✓ Reference number extracted (pattern 2): {}", referenceNumber);
        } else if (referenceMatcher3.find()) {
            referenceNumber = referenceMatcher3.group(1).trim();
            log.info("✓ Reference number extracted (pattern 3): {}", referenceNumber);
        } else if (referenceMatcher4.find()) {
            referenceNumber = referenceMatcher4.group(1).trim();
            log.info("✓ Reference number extracted (pattern 4): {}", referenceNumber);
        }

        if (referenceNumber != null) {
            info.put("referenceNumber", referenceNumber);
            // Use reference number as document number if not found
            if (!info.containsKey("documentNumber")) {
                info.put("documentNumber", referenceNumber);
            }
            log.info("✓ Final reference number: {}", referenceNumber);
        } else {
            log.warn("⚠ Could not find reference number in text");
        }

        // === STANDARD VISA EXTRACTION (fallback for non-UK visas) ===

        // Extract visa number (if not already found via reference)
        if (!info.containsKey("documentNumber")) {
            Pattern visaPattern = Pattern.compile("(?:Visa\\s*(?:No|Number)?\\s*:?\\s*)([A-Z0-9]{6,15})",
                    Pattern.CASE_INSENSITIVE);
            Matcher visaMatcher = visaPattern.matcher(text);
            if (visaMatcher.find()) {
                info.put("documentNumber", visaMatcher.group(1).trim());
                log.info("✓ Visa number extracted: {}", visaMatcher.group(1).trim());
            }
        }

        // Extract dates (only if not already found from UK format)
        if (!info.containsKey("expiryDate") || !info.containsKey("issueDate")) {
            extractDates(text, info);
        }

        // For UK visa
        if (text.contains("United Kingdom") || text.contains("UK") || text.contains("Home Office") ||
            text.contains("permission to work")) {
            info.put("issuingCountry", "United Kingdom");
            log.info("✓ Issuing country: United Kingdom");
        }

        // Extract visa holder name (if not found from UK pattern)
        if (!info.containsKey("fullName")) {
            Pattern namePattern = Pattern.compile("(?:Name|Holder|Full\\s*Name)\\s*:?\\s*([A-Z][A-Z\\s]+[A-Z])",
                    Pattern.CASE_INSENSITIVE);
            Matcher nameMatcher = namePattern.matcher(text);
            if (nameMatcher.find()) {
                info.put("fullName", nameMatcher.group(1).trim());
                log.info("✓ Full name extracted: {}", nameMatcher.group(1).trim());
            }
        }

        // Extract nationality
        Pattern nationalityPattern = Pattern.compile("(?:Nationality|Country\\s*of\\s*Birth)\\s*:?\\s*([A-Za-z\\s]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher nationalityMatcher = nationalityPattern.matcher(text);
        if (nationalityMatcher.find()) {
            info.put("nationality", nationalityMatcher.group(1).trim());
            log.info("✓ Nationality extracted: {}", nationalityMatcher.group(1).trim());
        }

        log.info("📋 VISA extraction complete. Extracted fields: {}", info.keySet());
        return info;
    }

    /**
     * Extract contract information from employment contract documents
     */
    public Map<String, Object> extractContractInformation(String text) {
        log.info("📝 Extracting CONTRACT information");
        log.debug("Full extracted text length: {} characters", text != null ? text.length() : 0);
        log.debug("Full extracted text:\n{}", text);  // Show FULL text for debugging

        Map<String, Object> info = new HashMap<>();

        if (text == null || text.trim().isEmpty()) {
            log.warn("⚠ Empty text provided for contract extraction");
            return info;
        }

        // === EXTRACT CONTRACT DATE (Date of Employment) ===
        // Pattern 1: "Date of Employment" or "Start Date" or "employment start date is"
        Pattern contractDatePattern1 = Pattern.compile(
            "(?:Date\\s*of\\s*Employment|Employment\\s*[Ss]tart\\s*[Dd]ate|[Ss]tart\\s*[Dd]ate|Commencement\\s*Date)(?:\\s+is)?\\s*[:\\s]*(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: Numeric date format for employment start date
        Pattern contractDatePattern2 = Pattern.compile(
            "(?:Date\\s*of\\s*Employment|Employment\\s*[Ss]tart\\s*[Dd]ate|[Ss]tart\\s*[Dd]ate|Commencement\\s*Date)(?:\\s+is)?\\s*[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 3: Look for "dated" followed by date (common in contracts)
        Pattern contractDatePattern3 = Pattern.compile(
            "\\bdated\\s+(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
        );

        String contractDateStr = null;
        Matcher dateMatcher1 = contractDatePattern1.matcher(text);
        Matcher dateMatcher2 = contractDatePattern2.matcher(text);
        Matcher dateMatcher3 = contractDatePattern3.matcher(text);

        if (dateMatcher1.find()) {
            contractDateStr = dateMatcher1.group(1).trim();
            log.info("Found contract date (pattern 1): '{}'", contractDateStr);
        } else if (dateMatcher2.find()) {
            contractDateStr = dateMatcher2.group(1).trim();
            log.info("Found contract date (pattern 2): '{}'", contractDateStr);
        } else if (dateMatcher3.find()) {
            contractDateStr = dateMatcher3.group(1).trim();
            log.info("Found contract date (pattern 3 - dated): '{}'", contractDateStr);
        }

        if (contractDateStr != null) {
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
            };

            LocalDate contractDate = parseDate(contractDateStr, formatters);
            if (contractDate != null) {
                info.put("contractDate", contractDate);
                log.info("✓ Contract date extracted: {}", contractDate);
            }
        } else {
            log.warn("⚠ Could not find contract date in text");
        }

        // === EXTRACT PLACE OF WORK ===
        // Pattern 1: "Place of Work" or "Work Location" or "Location"
        Pattern placePattern1 = Pattern.compile(
            "(?:Place\\s*of\\s*Work|Work\\s*Location|Location|Principal\\s*Place\\s*of\\s*Work)[:\\s]+([A-Za-z0-9\\s,.-]+?)(?:\\n|\\.|;|$)",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: Address pattern (for when place is described as address)
        Pattern placePattern2 = Pattern.compile(
            "(?:based\\s+at|work\\s+at|located\\s+at)[:\\s]+([A-Za-z0-9\\s,.-]+?)(?:\\n|\\.|;|$)",
            Pattern.CASE_INSENSITIVE
        );

        String placeOfWork = null;
        Matcher placeMatcher1 = placePattern1.matcher(text);
        Matcher placeMatcher2 = placePattern2.matcher(text);

        if (placeMatcher1.find()) {
            placeOfWork = placeMatcher1.group(1).trim();
            placeOfWork = placeOfWork.replaceAll("\\s+", " ").trim();
            // Clean up if too long (likely captured too much)
            if (placeOfWork.length() > 100) {
                placeOfWork = placeOfWork.substring(0, 100);
            }
            info.put("placeOfWork", placeOfWork);
            log.info("✓ Place of work extracted (pattern 1): {}", placeOfWork);
        } else if (placeMatcher2.find()) {
            placeOfWork = placeMatcher2.group(1).trim();
            placeOfWork = placeOfWork.replaceAll("\\s+", " ").trim();
            if (placeOfWork.length() > 100) {
                placeOfWork = placeOfWork.substring(0, 100);
            }
            info.put("placeOfWork", placeOfWork);
            log.info("✓ Place of work extracted (pattern 2): {}", placeOfWork);
        } else {
            log.warn("⚠ Could not find place of work in text");
        }

        // === EXTRACT CONTRACT BETWEEN (Parties) ===
        // Pattern 1: "between X and Y"
        Pattern contractBetweenPattern1 = Pattern.compile(
            "(?:Contract|Agreement)\\s+between\\s+([A-Za-z0-9\\s&.,'-]+?)\\s+and\\s+([A-Za-z\\s]+?)(?:\\n|\\(|dated|$)",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: "This Agreement is made between..."
        Pattern contractBetweenPattern2 = Pattern.compile(
            "(?:made|entered)\\s+between\\s+([A-Za-z0-9\\s&.,'-]+?)\\s+(?:and|&)\\s+([A-Za-z\\s]+?)(?:\\n|\\(|dated|$)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher betweenMatcher1 = contractBetweenPattern1.matcher(text);
        Matcher betweenMatcher2 = contractBetweenPattern2.matcher(text);

        if (betweenMatcher1.find()) {
            String party1 = betweenMatcher1.group(1).trim();
            String party2 = betweenMatcher1.group(2).trim();
            String contractBetween = party1 + " and " + party2;
            contractBetween = contractBetween.replaceAll("\\s+", " ").trim();
            info.put("contractBetween", contractBetween);
            log.info("✓ Contract between extracted (pattern 1): {}", contractBetween);
        } else if (betweenMatcher2.find()) {
            String party1 = betweenMatcher2.group(1).trim();
            String party2 = betweenMatcher2.group(2).trim();
            String contractBetween = party1 + " and " + party2;
            contractBetween = contractBetween.replaceAll("\\s+", " ").trim();
            info.put("contractBetween", contractBetween);
            log.info("✓ Contract between extracted (pattern 2): {}", contractBetween);
        } else {
            log.warn("⚠ Could not find contract parties in text");
        }

        // === EXTRACT JOB TITLE ===
        // Pattern 1: "Position" or "Job Title" or "Role"
        Pattern jobTitlePattern1 = Pattern.compile(
            "(?:Position|Job\\s*Title|Role|Post)[:\\s]+([A-Za-z\\s&/-]+?)(?:\\n|\\.|;|$)",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: "employed as" or "appointed as"
        Pattern jobTitlePattern2 = Pattern.compile(
            "(?:employed\\s+as|appointed\\s+as)[:\\s]+([A-Za-z\\s&/-]+?)(?:\\n|\\.|;|at|$)",
            Pattern.CASE_INSENSITIVE
        );

        String jobTitle = null;
        Matcher jobMatcher1 = jobTitlePattern1.matcher(text);
        Matcher jobMatcher2 = jobTitlePattern2.matcher(text);

        if (jobMatcher1.find()) {
            jobTitle = jobMatcher1.group(1).trim();
            jobTitle = jobTitle.replaceAll("\\s+", " ").trim();
            info.put("jobTitleContract", jobTitle);
            log.info("✓ Job title extracted (pattern 1): {}", jobTitle);
        } else if (jobMatcher2.find()) {
            jobTitle = jobMatcher2.group(1).trim();
            jobTitle = jobTitle.replaceAll("\\s+", " ").trim();
            info.put("jobTitleContract", jobTitle);
            log.info("✓ Job title extracted (pattern 2): {}", jobTitle);
        } else {
            log.warn("⚠ Could not find job title in text");
        }

        // Also extract full name if present
        Pattern namePattern = Pattern.compile(
            "(?:Employee|Employee\\s*Name)[:\\s]+([A-Z][A-Za-z\\s]+?)(?:\\n|\\(|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher nameMatcher = namePattern.matcher(text);
        if (nameMatcher.find()) {
            String fullName = nameMatcher.group(1).trim();
            fullName = fullName.replaceAll("\\s+", " ").trim();
            if (fullName.length() > 5 && fullName.length() < 50) {
                info.put("fullName", fullName);
                log.info("✓ Full name extracted from contract: {}", fullName);
            }
        }

        log.info("📝 CONTRACT extraction complete. Extracted fields: {}", info.keySet());
        return info;
    }

    private void extractDates(String text, Map<String, Object> info) {
        log.debug("🔍 Extracting dates from text");
        log.debug("Text length: {} characters", text.length());
        log.debug("Full extracted text:\n{}", text);  // Show FULL text for debugging

        // Common date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH)
        };

        // === DATE OF BIRTH EXTRACTION (Do this first) ===
        log.debug("Searching for date of birth...");

        Pattern[] dobPatterns = {
            // Standard patterns
            Pattern.compile("(?:Date\\s*of\\s*Birth|DOB|Birth\\s*Date|Born)\\s*:?\\s*(\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Date\\s*of\\s*Birth|DOB|Birth\\s*Date)\\s*:?\\s*(\\d{1,2}[/\\-.\\s]+\\d{1,2}[/\\-.\\s]+\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // Pakistani passport specific: "Date of Birt." or "Date of Birth" followed by date
            Pattern.compile("Date\\s*of\\s*Birt[h.]?\\s*[:\\s]*(\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:DOB)\\s*:?\\s*(\\d{2}[/\\-.\\s]*\\d{2}[/\\-.\\s]*\\d{2,4})", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : dobPatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String dateStr = matcher.group(1).trim();
                log.debug("Found potential birth date string: '{}'", dateStr);
                LocalDate dob = parseDate(dateStr, formatters);
                if (dob != null && dob.isBefore(LocalDate.now())) {
                    info.put("dateOfBirth", dob);
                    log.info("✓ Date of birth extracted: {}", dob);
                    break;
                }
            }
        }

        if (!info.containsKey("dateOfBirth")) {
            log.warn("⚠ Could not extract date of birth");
        }

        // === ISSUE AND EXPIRY DATE EXTRACTION ===
        // For Pakistani passports, issue and expiry dates often appear near "Issuing Authority"
        // Format: "Issuing Authority\n03 DEC 2024\nPAKISTAN\n02 DEC 2034"

        log.debug("Searching for issue and expiry dates near Issuing Authority...");

        // Try to find two dates near "Issuing Authority" or "Tracking Number"
        Pattern issuingAuthorityPattern = Pattern.compile(
            "(?:Issuing\\s*Authority|Authority)[\\s\\S]*?(\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4})[\\s\\S]*?(\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
        );

        Matcher issuingMatcher = issuingAuthorityPattern.matcher(text);
        if (issuingMatcher.find()) {
            String firstDate = issuingMatcher.group(1).trim();
            String secondDate = issuingMatcher.group(2).trim();

            log.debug("Found dates near Issuing Authority: '{}' and '{}'", firstDate, secondDate);

            LocalDate date1 = parseDate(firstDate, formatters);
            LocalDate date2 = parseDate(secondDate, formatters);

            if (date1 != null && date2 != null) {
                // The first date is usually issue date, second is expiry
                // Issue date should be in the past, expiry in the future
                if (date1.isBefore(LocalDate.now()) && date2.isAfter(LocalDate.now())) {
                    info.put("issueDate", date1);
                    info.put("expiryDate", date2);
                    log.info("✓ Issue date extracted: {}", date1);
                    log.info("✓ Expiry date extracted: {}", date2);
                } else if (date1.isAfter(date2)) {
                    // If first date is after second, they might be reversed
                    info.put("issueDate", date2);
                    info.put("expiryDate", date1);
                    log.info("✓ Issue date extracted (reversed): {}", date2);
                    log.info("✓ Expiry date extracted (reversed): {}", date1);
                } else {
                    log.warn("⚠ Found dates but they don't make sense: issue={}, expiry={}", date1, date2);
                }
            }
        }

        // === FALLBACK: Standard EXPIRY DATE patterns ===
        if (!info.containsKey("expiryDate")) {
            log.debug("Trying standard expiry date patterns...");

            Pattern[] expiryPatterns = {
                // Standard patterns
                Pattern.compile("(?:Expiry|Expiration|Valid\\s*Until|Date\\s*of\\s*Expiry|Expires?)\\s*:?\\s*(\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:Expiry|Expiration|Valid\\s*Until|Date\\s*of\\s*Expiry)\\s*:?\\s*(\\d{1,2}[/\\-.\\s]+\\d{1,2}[/\\-.\\s]+\\d{2,4})", Pattern.CASE_INSENSITIVE),
                // Compact format
                Pattern.compile("(?:EXP|EXPIRY)\\s*:?\\s*(\\d{2}[/\\-.\\s]*\\d{2}[/\\-.\\s]*\\d{2,4})", Pattern.CASE_INSENSITIVE),
                // Date that's in the future (likely expiry)
                Pattern.compile("\\b(\\d{2}\\s+[A-Z]{3,9}\\s+20[3-9]\\d)\\b")
            };

            for (Pattern pattern : expiryPatterns) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String dateStr = matcher.group(1).trim();
                    log.debug("Found potential expiry date string: '{}'", dateStr);
                    LocalDate expiryDate = parseDate(dateStr, formatters);
                    if (expiryDate != null && expiryDate.isAfter(LocalDate.now())) {
                        info.put("expiryDate", expiryDate);
                        log.info("✓ Expiry date extracted: {}", expiryDate);
                        break;
                    }
                }
            }
        }

        if (!info.containsKey("expiryDate")) {
            log.warn("⚠ Could not extract expiry date");
        }

        // === FALLBACK: Standard ISSUE DATE patterns ===
        if (!info.containsKey("issueDate")) {
            log.debug("Trying standard issue date patterns...");

            Pattern[] issuePatterns = {
                Pattern.compile("(?:Issue|Issued|Date\\s*of\\s*Issue|Issue\\s*Date)\\s*:?\\s*(\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:Issue|Issued|Date\\s*of\\s*Issue)\\s*:?\\s*(\\d{1,2}[/\\-.\\s]+\\d{1,2}[/\\-.\\s]+\\d{2,4})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:ISS|ISSUE)\\s*:?\\s*(\\d{2}[/\\-.\\s]*\\d{2}[/\\-.\\s]*\\d{2,4})", Pattern.CASE_INSENSITIVE),
                // Date that's in the recent past (likely issue date)
                Pattern.compile("\\b(\\d{2}\\s+[A-Z]{3,9}\\s+20[12]\\d)\\b")
            };

            for (Pattern pattern : issuePatterns) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String dateStr = matcher.group(1).trim();
                    log.debug("Found potential issue date string: '{}'", dateStr);
                    LocalDate issueDate = parseDate(dateStr, formatters);
                    if (issueDate != null && issueDate.isBefore(LocalDate.now().plusDays(1))) {
                        info.put("issueDate", issueDate);
                        log.info("✓ Issue date extracted: {}", issueDate);
                        break;
                    }
                }
            }
        }

        if (!info.containsKey("issueDate")) {
            log.warn("⚠ Could not extract issue date");
        }


        log.debug("Date extraction complete. Found: issueDate={}, expiryDate={}, dateOfBirth={}",
                  info.containsKey("issueDate"), info.containsKey("expiryDate"), info.containsKey("dateOfBirth"));
    }

    private LocalDate parseDate(String dateStr, DateTimeFormatter[] formatters) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Clean up the date string
        String cleanDate = dateStr.trim()
                .replaceAll("\\s+", " ")  // Normalize multiple spaces to single space
                .replaceAll("[..]", "-")  // Replace dots with dashes
                .replaceAll("/", "-");    // Replace slashes with dashes

        log.debug("Attempting to parse date: '{}' (cleaned: '{}')", dateStr, cleanDate);

        // Try "DD MMM YYYY" format FIRST (most common in passports like "03 DEC 2024")
        try {
            // The month abbreviations need to match Java's Locale.ENGLISH format exactly
            // "DEC" needs to be recognized as December
            DateTimeFormatter monthFormatterCaseSensitive = DateTimeFormatter.ofPattern("dd MMM yyyy")
                .withLocale(java.util.Locale.ENGLISH);

            // Try original case first (should work with proper case like "03 Dec 2024")
            try {
                LocalDate date = LocalDate.parse(cleanDate, monthFormatterCaseSensitive);
                log.debug("✓ Successfully parsed date with month format: {}", date);
                return date;
            } catch (DateTimeParseException e) {
                // Continue to uppercase attempt
            }

            // Convert to title case for month (e.g., "DEC" -> "Dec")
            String titleCaseDate = cleanDate;
            if (cleanDate.matches("\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4}")) {
                String[] parts = cleanDate.split("\\s+");
                if (parts.length == 3) {
                    String month = parts[1];
                    // Convert "DEC" to "Dec", "JUN" to "Jun", etc.
                    String monthTitleCase = month.charAt(0) + month.substring(1).toLowerCase();
                    titleCaseDate = parts[0] + " " + monthTitleCase + " " + parts[2];
                    log.debug("Converted to title case: '{}'", titleCaseDate);
                }
            }

            LocalDate date = LocalDate.parse(titleCaseDate, monthFormatterCaseSensitive);
            log.debug("✓ Successfully parsed date with title case month: {}", date);
            return date;
        } catch (DateTimeParseException e) {
            log.debug("Month format parsing failed: {}", e.getMessage());
        }

        // Try single digit day format "D MMM YYYY" (like "3 DEC 2024" or "19 JUN 1991")
        try {
            DateTimeFormatter singleDayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
                .withLocale(java.util.Locale.ENGLISH);

            // Convert to title case if needed
            String titleCaseDate = cleanDate;
            if (cleanDate.matches("\\d{1,2}\\s+[A-Z]{3,9}\\s+\\d{4}")) {
                String[] parts = cleanDate.split("\\s+");
                if (parts.length == 3) {
                    String month = parts[1];
                    String monthTitleCase = month.charAt(0) + month.substring(1).toLowerCase();
                    titleCaseDate = parts[0] + " " + monthTitleCase + " " + parts[2];
                }
            }

            LocalDate date = LocalDate.parse(titleCaseDate, singleDayFormatter);
            log.debug("✓ Successfully parsed date with single digit day format: {}", date);
            return date;
        } catch (DateTimeParseException e) {
            log.debug("Single day format parsing failed: {}", e.getMessage());
        }

        // Try with original format
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                log.debug("✓ Successfully parsed date with original format: {}", date);
                return date;
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // Try with cleaned format
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate date = LocalDate.parse(cleanDate, formatter);
                log.debug("✓ Successfully parsed date with cleaned format: {}", date);
                return date;
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // Try to handle 2-digit years
        if (cleanDate.matches("\\d{2}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = cleanDate.split("-");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);

                // Convert 2-digit year to 4-digit
                if (year < 50) {
                    year += 2000; // 00-49 -> 2000-2049
                } else {
                    year += 1900; // 50-99 -> 1950-1999
                }

                LocalDate date = LocalDate.of(year, month, day);
                log.debug("✓ Successfully parsed 2-digit year date: {}", date);
                return date;
            } catch (Exception e) {
                log.debug("Failed to parse 2-digit year format: {}", e.getMessage());
            }
        }

        log.warn("⚠ Could not parse date string: '{}' - tried all formats", dateStr);
        return null;
    }

    /**
     * Compress BufferedImage to JPEG with specified quality
     * @param image The image to compress
     * @param quality Quality factor (0.0 to 1.0)
     * @return Compressed image as byte array
     */
    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Use JPEG compression for better file size
        javax.imageio.ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(quality);

        jpgWriter.setOutput(ImageIO.createImageOutputStream(baos));
        jpgWriter.write(null, new javax.imageio.IIOImage(image, null, null), jpgWriteParam);
        jpgWriter.dispose();

        return baos.toByteArray();
    }

    /**
     * Resize BufferedImage by a scale factor
     * @param image The image to resize
     * @param scale Scale factor (e.g., 0.7 for 70% of original size)
     * @return Resized image
     */
    private BufferedImage resizeImage(BufferedImage image, double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();

        // Use high-quality rendering hints
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                          java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }
}
