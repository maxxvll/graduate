package com.maxxvll.utils;

import com.maxxvll.common.vo.CloudPreviewContentVO;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CloudDocumentPreviewRenderer {

    private static final int MAX_TEXT_CHARS = 200_000;
    private static final int MAX_SHEET_ROWS = 200;
    private static final int MAX_SHEET_COLUMNS = 24;
    private static final int MAX_SHEETS = 5;
    private static final int MAX_SLIDES = 60;

    private CloudDocumentPreviewRenderer() {
    }

    public static CloudPreviewContentVO renderText(String fileName, String contentType, byte[] data) {
        String decoded = decodeText(data);
        boolean truncated = decoded.length() > MAX_TEXT_CHARS;
        String safeText = truncated ? decoded.substring(0, MAX_TEXT_CHARS) : decoded;
        return CloudPreviewContentVO.builder()
                .mode("text")
                .title(fileName)
                .contentType(contentType)
                .textContent(safeText)
                .truncated(truncated)
                .build();
    }

    public static CloudPreviewContentVO renderDocument(String fileName, String contentType, String extension, byte[] data) throws Exception {
        return switch (extension) {
            case "docx" -> buildHtmlPreview(fileName, contentType, renderDocx(data), false);
            case "doc" -> buildHtmlPreview(fileName, contentType, wrapPre(extractLegacyWord(data)), false);
            case "xlsx", "xls" -> buildHtmlPreview(fileName, contentType, renderWorkbook(data), false);
            case "pptx", "ppt" -> buildHtmlPreview(fileName, contentType, renderSlides(data), false);
            default -> CloudPreviewContentVO.builder()
                    .mode("unsupported")
                    .title(fileName)
                    .contentType(contentType)
                    .message("当前文件类型暂不支持在线预览，请先下载后查看。")
                    .build();
        };
    }

    private static CloudPreviewContentVO buildHtmlPreview(String fileName, String contentType, String htmlContent, boolean truncated) {
        return CloudPreviewContentVO.builder()
                .mode("html")
                .title(fileName)
                .contentType(contentType)
                .htmlContent(htmlContent)
                .truncated(truncated)
                .build();
    }

    private static String renderDocx(byte[] data) throws Exception {
        StringBuilder html = new StringBuilder(4096);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(data))) {
            html.append("<section class=\"doc-preview\">");
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text == null || text.isBlank()) {
                    continue;
                }
                html.append("<p>").append(HtmlUtils.htmlEscape(text)).append("</p>");
            }
            for (XWPFTable table : document.getTables()) {
                html.append("<table class=\"doc-table\">");
                for (XWPFTableRow row : table.getRows()) {
                    html.append("<tr>");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        html.append("<td>").append(HtmlUtils.htmlEscape(cell.getText())).append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table>");
            }
            html.append("</section>");
        }
        return html.toString();
    }

    private static String extractLegacyWord(byte[] data) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(data));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String renderWorkbook(byte[] data) throws Exception {
        StringBuilder html = new StringBuilder(4096);
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            html.append("<section class=\"sheet-preview\">");
            int renderedSheets = 0;
            for (Sheet sheet : workbook) {
                if (renderedSheets >= MAX_SHEETS) {
                    break;
                }
                renderedSheets += 1;
                html.append("<section class=\"sheet-block\">")
                        .append("<h3>").append(HtmlUtils.htmlEscape(sheet.getSheetName())).append("</h3>")
                        .append("<table class=\"doc-table\">");
                int renderedRows = 0;
                for (Row row : sheet) {
                    if (renderedRows >= MAX_SHEET_ROWS) {
                        break;
                    }
                    renderedRows += 1;
                    html.append("<tr>");
                    short lastCellNum = row.getLastCellNum();
                    int cellLimit = lastCellNum < 0 ? 0 : Math.min(lastCellNum, MAX_SHEET_COLUMNS);
                    for (int cellIndex = 0; cellIndex < cellLimit; cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        String text = cell == null ? "" : formatter.formatCellValue(cell);
                        html.append("<td>").append(HtmlUtils.htmlEscape(text)).append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table></section>");
            }
            html.append("</section>");
        }
        return html.toString();
    }

    private static String renderSlides(byte[] data) throws Exception {
        StringBuilder html = new StringBuilder(4096);
        try (SlideShow<?, ?> slideShow = SlideShowFactory.create(new ByteArrayInputStream(data))) {
            html.append("<section class=\"slide-preview\">");
            int slideIndex = 0;
            for (Slide<?, ?> slide : slideShow.getSlides()) {
                if (slideIndex >= MAX_SLIDES) {
                    break;
                }
                slideIndex += 1;
                List<String> texts = new ArrayList<>();
                for (Shape<?, ?> shape : slide.getShapes()) {
                    if (shape instanceof TextShape<?, ?> textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            texts.add(HtmlUtils.htmlEscape(text));
                        }
                    }
                }
                html.append("<section class=\"slide-block\">")
                        .append("<h3>第 ").append(slideIndex).append(" 页</h3>");
                if (texts.isEmpty()) {
                    html.append("<p>该页未提取到可读文本。</p>");
                } else {
                    for (String text : texts) {
                        html.append("<p>").append(text).append("</p>");
                    }
                }
                html.append("</section>");
            }
            html.append("</section>");
        }
        return html.toString();
    }

    private static String wrapPre(String text) {
        return "<pre class=\"doc-pre\">" + HtmlUtils.htmlEscape(limitText(text)) + "</pre>";
    }

    private static String limitText(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
    }

    private static String decodeText(byte[] data) {
        byte[] normalized = stripUtf8Bom(data);
        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return utf8Decoder.decode(ByteBuffer.wrap(normalized)).toString();
        } catch (CharacterCodingException ignored) {
            return new String(normalized, StandardCharsets.UTF_8);
        }
    }

    private static byte[] stripUtf8Bom(byte[] data) {
        if (data == null || data.length < 3) {
            return data == null ? new byte[0] : data;
        }
        if ((data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            byte[] stripped = new byte[data.length - 3];
            System.arraycopy(data, 3, stripped, 0, stripped.length);
            return stripped;
        }
        return data;
    }
}
