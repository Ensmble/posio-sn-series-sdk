# Posio Printer — Print Command Cheatsheet

Everything you need to design a receipt, two ways:

- **HTTP API** (`POST /print/receipt`) — for web/cloud/POS software. JSON "jobs". No SDK.
- **Android SDK** (`PosioPrinter`) — for native Android apps.

Both drive the same printer, so the building blocks and value tables below are identical.

---

## 1. Receipt as JSON (HTTP `POST /print/receipt`)

Send `{"jobs":[ ... ]}`. Jobs print top‑to‑bottom in order. If you omit a `cut` job, the
paper is fed out automatically at the end.

```bash
curl -X POST http://<device-ip>:8080/print/receipt \
  -H "Content-Type: application/json" \
  -d '{ "jobs": [
    {"type":"image","base64":"<logo PNG, base64>","align":"center"},
    {"type":"text","text":"MY STORE","align":"center","bold":true,"size":40},
    {"type":"text","text":"123 Market Street","align":"center"},
    {"type":"text","text":"Tel 555-1234","align":"center"},
    {"type":"feed","lines":1},
    {"type":"text","text":"Receipt #1001        2026-06-17"},
    {"type":"text","text":"--------------------------------"},
    {"type":"text","text":"Coffee                    3.50"},
    {"type":"text","text":"Muffin                    2.00"},
    {"type":"text","text":"--------------------------------"},
    {"type":"text","text":"TOTAL                     5.50","bold":true,"size":32},
    {"type":"feed","lines":1},
    {"type":"barcode","content":"1001","symbology":0,"align":"center","textPosition":2},
    {"type":"qrcode","content":"https://posio.example/r/1001","size":320,"align":"center"},
    {"type":"text","text":"Thank you!","align":"center"},
    {"type":"cut"}
  ]}'
```

### Job types

| `type` | Fields (defaults) | Notes |
|--------|-------------------|-------|
| `text` | `text`, `size`(24), `align`(`left`), `bold`(false), `underline`(false), `font`(0) | One line. `align`: `left`/`center`/`right`. `font`: 0–5 (**4 MONOSPACE**). |
| `feed` | `lines` *or* `px` | Blank space. `lines` × 24 px if `px` omitted. |
| `barcode` | `content`, `width`(300), `height`(120), `textPosition`(0), `align`(`center`), `symbology`(0) | 1D barcode. See tables below. |
| `qrcode` | `content`, `size`(300), `align`(`center`) | `size` = width & height in px. |
| `image` | `base64`, `type`(0), `align`(`center`) | PNG/JPEG, base64, no `data:` prefix. `type`: 0=B&W, 1=grayscale. |
| `escpos` | `base64` | Raw ESC/POS bytes, base64. |
| `cut` | — | Feed to cut position (and cut, on devices with a cutter). |

### Single‑shot HTTP endpoints (no jobs array)

| Method & path | Body |
|---|---|
| `POST /print/text` | plain text, or `{text,size,align,bold,underline,font}` |
| `POST /print/image` | `{base64,type,align}` |
| `POST /print/escpos` | `{base64}` |
| `GET /status` | — (printer connection + status) |

---

## 2. Receipt with the Android SDK (`PosioPrinter`)

```java
PrintTextFormat title = PosioPrinter.textFormat();
title.setAli(1);            // center
title.setTextSize(40);
title.setStyle(1);          // bold

PrintTextFormat mono = PosioPrinter.textFormat();
mono.setFont(4);            // MONOSPACE — keeps columns aligned

printer.printImage(logoBitmap, 0, 1);          // bitmap, type, align
printer.printText("MY STORE", title);
printer.printText("123 Market Street");
printer.feedPaper(24);
printer.printText("Coffee                3.50", mono);
printer.printText("TOTAL                 5.50", mono);
printer.printBarcode("1001", 300, 120, 2, 1, 0);   // content,w,h,textPos,align,symbology
printer.printQrCode("https://posio.example/r/1001", 320, 320, 1);
printer.cutPaper();
```

### Method reference

| Method | Purpose |
|--------|---------|
| `printText(text)` / `printText(text, fmt)` | Print a line (optionally styled). |
| `printText(text, fmt, textWidth, align)` | Print text constrained to a max block width. |
| `printBarcode(content, w, h, textPosition, align, symbology)` | 1D barcode. |
| `printQrCode(content, w, h, align)` | QR code. |
| `printImage(bitmap, type, align)` | Image (`type` 0=B&W, 1=grayscale). |
| `printTableRow(String[] cols, int[] weights, PrintTextFormat[] fmts)` | One table row; `weights` set column widths. |
| `printEscPos(byte[])` | Raw ESC/POS. |
| `printRaster(byte[])` | Raw raster bitmap data. |
| `feedPaper(px)` / `feedPaperBack(px)` | Advance / reverse paper. |
| `cutPaper()` | Finish: feed to cut position (+ cut if supported). |
| `openCashBox()` | Open the cash drawer. |
| `showLcdBitmap(bmp)` / `configLcd(flag)` / `setLcdLogo(bmp)` | Customer‑display LCD. |
| `triggerInfraredScan(open)` | Arm/stop the hardware scanner. |
| `detectLabelAuto()` / `hasLabelLearning()` / `locateLabel(h,gap)` / `locateLabelAuto(h,gap)` / `endLabel()` / `clearLabelLearning()` | Label printing. |
| `getPrinterStatus()` / `getPrinterVersion()` / `getPrinterModel()` / `getServiceVersion()` | Info. |
| `getPrinterDensity()` / `setPrinterDensity(d)` | Print darkness. |
| `isConnected()` / `raw()` | Connection state / raw `IPrinterService`. |

> Print calls are synchronous IPC — run them on a **background thread**. Each returns an
> `int` result code (see §5).

### Text style (`PrintTextFormat`)

| Setter | Values |
|--------|--------|
| `setTextSize(int)` | pixels (default 24) |
| `setAli(int)` | 0 left · 1 center · 2 right |
| `setStyle(int)` | 0 normal · 1 bold · 2 italic · 3 bold+italic |
| `setUnderline(boolean)` | underline on/off |
| `setFont(int)` | 0 DEFAULT · 1 DEFAULT_BOLD · 2 SANS_SERIF · 3 SERIF · **4 MONOSPACE** · 5 CUSTOM |
| `setPath(String)` | path to a `.ttf` (use with `setFont(5)`) |
| `setTextScaleX/Y(float)` | stretch (1.0 = normal) |
| `setLetterSpacing(float)` / `setLineSpacing(float)` | spacing |
| `setTopPadding(int)` / `setLeftPadding(int)` | padding in px |

---

## 3. Value tables

**Alignment** — `0` left · `1` center · `2` right (JSON: `"left"`/`"center"`/`"right"`).

**Font** (`setFont` / JSON `font`) — `0` DEFAULT · `1` DEFAULT_BOLD · `2` SANS_SERIF ·
`3` SERIF · **`4` MONOSPACE** · `5` CUSTOM (SDK: pair with `setPath(".ttf")`).

**Barcode symbology** — `0` CODE128 · `1` CODE39 · `2` CODE93 · `3` UPC‑A · `4` UPC‑E ·
`5` EAN13 · `6` EAN8 · `7` ITF · `8` CODABAR.

**Barcode text position** — `0` none · `1` above · `2` below · `3` both.

**Image render type** — `0` black & white (dithered; best for logos/line art) ·
`1` grayscale (best for photos).

**LCD `configLcd` flag** — `0` init · `1` wake · `2` sleep · `3` clear · `4` reset.

**Print density** — 58 mm: 80/90/100/110/120/130 · 80 mm: 100/110/120/130 (higher = darker).

---

## 4. Receipt‑design tips

- **Line width:** ~**32 characters** per line on 58 mm paper (~**48** on 80 mm) at the
  default text size. Use a `"--------------------------------"` separator to gauge width.
- **Columns:** use **monospace** so prices line up — SDK `setFont(4)` / `printTableRow(...)`,
  or HTTP `"font":4` on a `text` job. No-`font` fallback: pad columns with spaces.
- **Logos/images:** PNG, pure black‑on‑white, width ≤ the printer dots — **≈384 px (58 mm)**,
  **≈576 px (80 mm)**. Height is unlimited. **Crop blank margins first** — the printer prints
  every blank row/column, so whitespace baked into a logo/QR/signature prints as extra space.
- **QR:** keep `size` ≈ 240–360 px so it scans reliably; center it.
- **Spacing:** use `feed` (lines/px) rather than blank `text` lines for predictable gaps.
- **Always finish with `cut`** (or rely on the auto feed‑out) so the receipt tears cleanly.

---

## 5. Result codes

`0` = success. Negative = error:

| Code | Meaning |
|------|---------|
| `-1100` | Printer service not connected / not installed |
| `-1201` | Cover open |
| `-1202` | Invalid parameter |
| `-1203` | Out of paper |
| `-1204` | Overheated |
| `-1206` | Busy |
| `-1209` | Low battery |
| `-1099` | Feature not supported on this device |

SDK: `PrinterResult.message(code)` gives the text. HTTP: every response is
`{"code":0,"message":"OK"}` on success.
