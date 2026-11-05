package com.cureus.openhtmltopdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PdfRenderer {

    public static void main(String[] args) throws Exception {

        if (args.length != 2 && args.length != 3) {
            System.err.println(
                "Usage: PdfRenderer <input.html> <output.pdf> [base-uri]"
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

        try (
            OutputStream outputStream =
                new FileOutputStream(outputFile)
        ) {

            PdfRendererBuilder builder =
                new PdfRendererBuilder();

            builder
                .withHtmlContent(
                    html,
                    baseUri
                )

                .usePdfUaAccessibility(true)

                .toStream(outputStream);

            builder.run();
        }

        System.out.println(
            "PDF generated successfully."
        );
    }
}