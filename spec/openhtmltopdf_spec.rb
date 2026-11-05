# frozen_string_literal: true

require "tmpdir"

RSpec.describe OpenHtmlToPdf do
  it "exposes a version" do
    expect(Openhtmltopdf::VERSION).to match(/\A\d+\.\d+\.\d+\z/)
  end

  it "bundles the Java runner JAR" do
    expect(File.exist?(OpenHtmlToPdf.java_jar)).to be true
  end

  describe ".render" do
    it "rejects empty HTML" do
      Dir.mktmpdir do |dir|
        expect { OpenHtmlToPdf.render("  ", output: File.join(dir, "x.pdf")) }
          .to raise_error(ArgumentError, /empty/)
      end
    end

    it "requires the output keyword" do
      expect { OpenHtmlToPdf.render("<p>hi</p>") }.to raise_error(ArgumentError)
    end
  end

  describe ".render_file" do
    it "raises a clear error for a missing file" do
      expect { OpenHtmlToPdf.render_file("/no/such/file.html", output: "/tmp/x.pdf") }
        .to raise_error(OpenHtmlToPdf::Error, /not found/)
    end
  end

  describe "rendering", :integration do
    it "writes a PDF from an HTML string" do
      Dir.mktmpdir do |dir|
        out = File.join(dir, "out.pdf")
        OpenHtmlToPdf.render("<html><body><h1>Hello</h1></body></html>", output: out)

        expect(File.binread(out, 5)).to eq("%PDF-")
      end
    end

    it "writes a PDF from an HTML file and creates missing output directories" do
      Dir.mktmpdir do |dir|
        src = File.join(dir, "in.html")
        File.write(src, "<html><body><p>From file</p></body></html>")
        out = File.join(dir, "nested", "out.pdf")

        OpenHtmlToPdf.render_file(src, output: out)

        expect(File.binread(out, 5)).to eq("%PDF-")
      end
    end

    it "writes the same PDF uncompressed with compress: false" do
      Dir.mktmpdir do |dir|
        # PDF/UA can't use the renderer's built-in fonts, so the text needs the runner's Myriad Pro
        html = "<html><body style=\"font-family: MyriadPro\"><h1>Hello</h1><p>World</p></body></html>"
        compressed = File.join(dir, "compressed.pdf")
        uncompressed = File.join(dir, "uncompressed.pdf")

        OpenHtmlToPdf.render(html, output: compressed)
        OpenHtmlToPdf.render(html, output: uncompressed, compress: false)

        expect(File.binread(compressed)).to include("/FlateDecode")
        expect(File.binread(uncompressed)).to start_with("%PDF-")
        expect(File.binread(uncompressed)).not_to include("/FlateDecode")
        expect(File.binread(uncompressed)).not_to include("/ObjStm")
        # The page's drawing operators are readable once the content stream isn't compressed
        expect(File.binread(uncompressed)).to match(/\bBT\b.*\bET\b/m)
      end
    end

    it "raises OpenHtmlToPdf::Error when the runner fails" do
      Dir.mktmpdir do |dir|
        bad = OpenHtmlToPdf::Renderer.new(java_jar: File.join(dir, "missing.jar"))
        expect { bad.render(html: "<p>x</p>", output: File.join(dir, "x.pdf")) }
          .to raise_error(OpenHtmlToPdf::Error, /OpenHTMLtoPDF failed/)
      end
    end
  end
end
