package com.cureus.openhtmltopdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PdfRenderer {

    private static final String FONT_FAMILY = "MyriadPro";

    private static final String LIGHT_FONT =
        "/fonts/myriadprolight.ttf";

    private static final String REGULAR_FONT =
        "/fonts/myriadproregular.ttf";

    private static final String SEMIBOLD_FONT =
        "/fonts/myriadprosemibold.ttf";

    private static final String BOLD_FONT =
        "/fonts/myriadprobold.ttf";

    private static final String NO_COMPRESSION_FLAG =
        "--no-compression";

    public static void main(String[] commandLine) throws Exception {

        // Some upload firewalls (Turnitin's) reject compressed PDFs;
        // --no-compression writes the same PDF with its streams uncompressed.
        boolean noCompression = false;
        List<String> positional = new ArrayList<>();

        for (String arg : commandLine) {
            if (arg.equals(NO_COMPRESSION_FLAG)) {
                noCompression = true;
            } else {
                positional.add(arg);
            }
        }

        String[] args = positional.toArray(new String[0]);

        if (args.length != 2 && args.length != 3) {
            System.err.println(
                "Usage: PdfRenderer <input.html> <output.pdf> [base-uri] [--no-compression]"
            );
            System.exit(1);
        }

        File htmlFile =
            new File(args[0]).getAbsoluteFile();

        File outputFile =
            new File(args[1]).getAbsoluteFile();

        if (!htmlFile.exists()) {
            throw new IllegalArgumentException(
                "HTML file not found: " +
                htmlFile.getAbsolutePath()
            );
        }

        File parentDirectory =
            htmlFile.getParentFile();

        if (parentDirectory == null) {
            throw new IllegalArgumentException(
                "Could not determine HTML parent directory"
            );
        }

        String html =
            Files.readString(
                htmlFile.toPath(),
                StandardCharsets.UTF_8
            );

        String baseUri;

        if (args.length == 3 && !args[2].isEmpty()) {
            // Explicit base: a URL (contains "://") is used as-is,
            // otherwise it is treated as a filesystem directory.
            baseUri = args[2].contains("://")
                ? args[2]
                : new File(args[2]).getAbsoluteFile().toURI().toString();
        } else {
            baseUri = parentDirectory.toURI().toString();
        }

        System.out.println(
            "Input HTML : " +
            htmlFile.getAbsolutePath()
        );

        System.out.println(
            "Output PDF : " +
            outputFile.getAbsolutePath()
        );

        System.out.println(
            "Base URI   : " +
            baseUri
        );

        System.out.println(
            "HTML size  : " +
            html.length()
        );

        File lightFont = extractFont(LIGHT_FONT);
        File regularFont = extractFont(REGULAR_FONT);
        File semiboldFont = extractFont(SEMIBOLD_FONT);
        File boldFont = extractFont(BOLD_FONT);

        System.out.println(
            "Light font     : " +
            lightFont.getAbsolutePath()
        );

        System.out.println(
            "Regular font   : " +
            regularFont.getAbsolutePath()
        );

        System.out.println(
            "Semibold font  : " +
            semiboldFont.getAbsolutePath()
        );

        System.out.println(
            "Bold font      : " +
            boldFont.getAbsolutePath()
        );

        ByteArrayOutputStream rendered =
            new ByteArrayOutputStream();

        try (
            OutputStream outputStream = rendered
        ) {

            PdfRendererBuilder builder =
                new PdfRendererBuilder();

            builder
                .withHtmlContent(
                    html,
                    baseUri
                )

                .useFont(
                    lightFont,
                    FONT_FAMILY,
                    300,
                    PdfRendererBuilder.FontStyle.NORMAL,
                    true
                )

                .useFont(
                    regularFont,
                    FONT_FAMILY,
                    400,
                    PdfRendererBuilder.FontStyle.NORMAL,
                    true
                )

                .useFont(
                    semiboldFont,
                    FONT_FAMILY,
                    600,
                    PdfRendererBuilder.FontStyle.NORMAL,
                    true
                )

                .useFont(
                    boldFont,
                    FONT_FAMILY,
                    700,
                    PdfRendererBuilder.FontStyle.NORMAL,
                    true
                )

                .usePdfUaAccessibility(true)

                .toStream(outputStream);

            builder.run();
        }

        try (
            OutputStream outputStream =
                new FileOutputStream(outputFile)
        ) {
            if (noCompression) {
                writeUncompressed(
                    rendered.toByteArray(),
                    outputStream
                );
            } else {
                rendered.writeTo(outputStream);
            }
        }

        System.out.println(
            "PDF generated successfully."
        );
    }

    // Like wkhtmltopdf's --no-pdf-compression: every Flate stream is stored
    // decoded and there are no object streams. JPEG (DCT) images stay as they are.
    private static void writeUncompressed(
        byte[] pdf,
        OutputStream outputStream
    ) throws Exception {

        try (PDDocument document = Loader.loadPDF(pdf)) {

            for (COSObjectKey key :
                    new ArrayList<>(document.getDocument().getXrefTable().keySet())) {

                COSBase object =
                    document.getDocument().getObjectFromPool(key).getObject();

                if (object instanceof COSStream) {
                    decodeFlate((COSStream) object);
                }
            }

            // The trailer comes from the compressed file's cross-reference
            // stream; a plain trailer has none of that stream's keys
            COSDictionary trailer = document.getDocument().getTrailer();

            for (COSName key : new COSName[] {
                    COSName.TYPE, COSName.FILTER, COSName.DECODE_PARMS,
                    COSName.LENGTH, COSName.W, COSName.INDEX, COSName.PREV }) {
                trailer.removeItem(key);
            }

            document.save(
                outputStream,
                CompressParameters.NO_COMPRESSION
            );
        }
    }

    private static void decodeFlate(COSStream stream) throws Exception {

        COSBase filters = stream.getFilters();

        if (filters instanceof COSArray && ((COSArray) filters).size() == 1) {
            filters = ((COSArray) filters).getObject(0);
        }

        if (!COSName.FLATE_DECODE.equals(filters)) {
            return;
        }

        byte[] decoded;

        try (InputStream input = stream.createInputStream()) {
            decoded = input.readAllBytes();
        }

        stream.removeItem(COSName.FILTER);
        stream.removeItem(COSName.DECODE_PARMS);

        try (OutputStream output = stream.createRawOutputStream()) {
            output.write(decoded);
        }
    }

    private static File extractFont(String resource)
        throws Exception {

        InputStream inputStream =
            PdfRenderer.class.getResourceAsStream(
                resource
            );

        if (inputStream == null) {
            throw new IllegalStateException(
                "Font resource not found: " +
                resource
            );
        }

        File fontFile =
            File.createTempFile(
                "openhtmltopdf-",
                ".ttf"
            );

        fontFile.deleteOnExit();

        System.out.println(
            "Extracting font: " +
            resource
        );

        try (
            InputStream input = inputStream;
            OutputStream output =
                new FileOutputStream(fontFile)
        ) {
            input.transferTo(output);
        }

        if (fontFile.length() == 0) {
            throw new IllegalStateException(
                "Extracted font is empty: " +
                resource
            );
        }

        return fontFile;
    }
}