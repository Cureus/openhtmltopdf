# PDF fonts

TrueType fonts for `@font-face` rules, found with `OpenHtmlToPdf.fonts_dir` (Cureus declares them in
`Cureus::PdfGenerators::FontFaces`).
The renderer can't use OpenType/CFF or variable fonts, so variable fonts were cut into static
instances with fontTools `varLib.instancer`.
The Latin fonts have their ligature tables (GSUB) removed: the PDF can't map a ligature glyph such as
"fi" back to text, which PDF/UA requires. Each file's name table names its own weight.
Myriad Pro is registered by the Java runner (java/src/main/resources/fonts).

| File | Source | Licence |
| --- | --- | --- |
| NotoSans-Regular/Bold | google/fonts `ofl/notosans/NotoSans[wdth,wght].ttf` (wght 400/700, wdth 100) | OFL |
| NotoSansSymbols2-Regular | google/fonts `ofl/notosanssymbols2` | OFL |
| NotoSansMath-Regular | google/fonts `ofl/notosansmath` | OFL |
| NotoSansSC/JP/KR-Regular | google/fonts `ofl/notosans{sc,jp,kr}/*[wght].ttf` (wght 400) | OFL |
| NotoEmoji-Regular | google/fonts `ofl/notoemoji/NotoEmoji[wght].ttf` (wght 400) | OFL |
| LiberationSans-* | liberation-fonts 2.1.5 release, metric-compatible with Arial | OFL |
| LiberationSerif-Regular/Bold/Italic | liberation-fonts 2.1.5 release, metric-compatible with Times New Roman | OFL |

Licence texts are in `licenses/`.
