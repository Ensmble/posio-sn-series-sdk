# Posio Printer SDK

Android SDK for printing on **Posio** devices — receipts, barcodes, QR codes, images, labels,
customer-display (LCD), and cash drawer. Add one dependency, bind, and print.

> Building a **web / cloud** POS instead of a native Android app? You don't need this SDK — the
> device also exposes a local **HTTP API for printing _and_ barcode/QR scanning**.
> See **[Web / HTTP printing & scanning](#web--http-printing--scanning)** below.

## How it works

```
┌─────────────────────┐     bind (AIDL)     ┌──────────────────────────┐
│  Your Android app    │ ──────────────────► │  Posio Printer Service    │ ──► printer hardware
│  (uses this SDK)      │                     │  (installed on the device) │
└─────────────────────┘                     └──────────────────────────┘
```

Your app talks to this SDK; the SDK binds to the **Posio Printer Service** that runs on the device
and drives the hardware. You never touch low-level printer commands.

## Requirements

- Android **5.0+** (API 21)
- A **Posio device** with the **Posio Printer Service** app installed. The APK is in the
  [`apks/`](apks) folder of this repo (`PosioPrinterService.apk`) — install it on the device once.
  A full-featured **tabbed demo app** (`PosioPrinterClient-demo.apk`, built from the
  [`sample`](sample) module) is included there too — it exercises every SDK feature across
  Printer / Scanner / Display / Cash / Status tabs.

## Installation (JitPack)

**1. Add JitPack** to your `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**2. Add the dependency** in your app module `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.Ensmble:posio-sn-series-sdk:1.0.0'
}
```

> On AGP 8.0+, make sure AIDL is enabled in your module: `android { buildFeatures { aidl = true } }`

## Quick start

```java
PosioPrinter printer = new PosioPrinter(context);

printer.connect(new PosioPrinter.ConnectionListener() {
    @Override public void onConnected() {
        // Ready. Run print calls on a background thread.
    }
    @Override public void onDisconnected() {
        // Service lost or not installed.
    }
});

// --- on a background thread ---
PrintTextFormat fmt = PosioPrinter.textFormat();
fmt.setAli(1);          // 0=left, 1=center, 2=right
fmt.setTextSize(32);

printer.printText("MY STORE", fmt);
printer.printText("Coffee            3.50");
printer.printText("TOTAL             5.50");
printer.printQrCode("https://posio.example/r/1001", 300, 300, 1);
printer.cutPaper();
```

Every method returns an `int` result — `PrinterResult.OK` (0) means success:

```java
int code = printer.printText("Hello");
if (!PrinterResult.isSuccess(code)) {
    Log.w("POS", PrinterResult.message(code));   // e.g. "out of paper"
}
```

Call `printer.disconnect()` when you're done (e.g. in `onDestroy`).

## What you can do

| Area | Methods |
|------|---------|
| Text | `printText(text)`, `printText(text, format)` |
| Barcode / QR | `printBarcode(...)`, `printQrCode(...)` |
| Image | `printImage(bitmap, type, align)` |
| Tables | `printTableRow(columns, weights, formats)` |
| Raw ESC/POS | `printEscPos(bytes)` |
| Paper | `feedPaper(px)`, `feedPaperBack(px)`, `cutPaper()` |
| Labels | `detectLabelAuto()`, `hasLabelLearning()`, `locateLabel(...)`, `locateLabelAuto(...)`, `endLabel()`, `clearLabelLearning()` |
| Cash drawer | `openCashBox()` |
| Customer LCD | `showLcdBitmap(bitmap)`, `configLcd(flag)`, `setLcdLogo(bitmap)` |
| Infrared scan | `triggerInfraredScan(open)` |
| Status / device | `getPrinterStatus()`, `getPrinterVersion()`, `getPrinterModel()`, `getPrinterDensity()`, `setPrinterDensity(d)` |

Need something not wrapped here? `printer.raw()` returns the full `IPrinterService` interface.

**📋 Full command reference:** see the **[Print Command Cheatsheet](CHEATSHEET.md)** — every
command for both the SDK and the HTTP `/print/receipt` API, with all value tables (alignment,
barcode symbology, fonts, image type, result codes) and receipt‑design tips.

### Text formatting & fonts

Text style is set via `PrintTextFormat`:

```java
PrintTextFormat fmt = PosioPrinter.textFormat();
fmt.setTextSize(28);     // px
fmt.setAli(0);           // 0=left, 1=center, 2=right
fmt.setStyle(1);         // 0=normal, 1=bold, 2=italic, 3=bold+italic
fmt.setUnderline(true);
fmt.setFont(4);          // see font table below
printer.printText("Item        Qty   Price", fmt);
```

Built-in fonts (`setFont`): `0` DEFAULT · `1` DEFAULT_BOLD · `2` SANS_SERIF · `3` SERIF ·
**`4` MONOSPACE** · `5` CUSTOM.

**Monospace is supported** — use `fmt.setFont(4)`. It's the best choice for receipts because
every character has the same width, so columns line up. For your own typeface, use
`fmt.setFont(5)` and `fmt.setPath("/sdcard/.../myfont.ttf")`. Other knobs: `setTextScaleX/Y`,
`setLetterSpacing`, `setLineSpacing`.

Monospace is also selectable over the **HTTP API** — pass `"font": 4` on a `text` job or
`/print/text` (values `0`–`4`, same as `setFont`; `5` CUSTOM is SDK-only as it needs a font
path). See [HOW_TO_USE.md](HOW_TO_USE.md) §4b.

### Printing images

```java
printer.printImage(bitmap, 0, 1);   // bitmap, type, align
```

- **Input:** an Android `Bitmap`. Use **PNG or JPEG** (also WebP) — these always decode via
  `BitmapFactory.decodeResource(...)` / `decodeFile(...)`. Avoid **BMP**: Android decodes it
  inconsistently (device/sub-format dependent), so it often fails. Over the **HTTP API** send
  the raw image bytes **Base64-encoded** (standard base64, no `data:` prefix, no line wraps).
- **`type`** — `0` = **black & white** (1-bit, dithered; best for logos and line art),
  `1` = **grayscale** (best for photos). The printer head is monochrome, so high-contrast,
  pure black-on-white artwork prints the cleanest.
- **`align`** — `0` left, `1` center, `2` right.
- **Width** — keep the image within the printer's dot width so it isn't scaled down:
  **≈384 px for 58 mm** paper, **≈576 px for 80 mm** paper. Height is unlimited.
- **Crop blank margins** — the printer prints every row/column of the bitmap, so any
  white/transparent border baked into the source (a logo on a tall canvas, a QR with a wide
  quiet-zone, a padded signature) prints as **extra blank space**. Trim the image to its
  content first. The sample app shows this: `trimBlankMargins(...)` in
  [`PrinterFragment.java`](sample/src/main/java/com/posio/printersdk/sample/PrinterFragment.java).
- **Extra space *after* the image** — `cutPaper()` (and the receipt auto feed-out) advances the
  paper to the tear bar, which shows up as blank space below the image. To end tight against the
  artwork — exactly like a QR code — **print the image and don't call `cutPaper()`**. The sample
  prints images this way. Add `cutPaper()` / `feedPaper(px)` only when you actually want the
  receipt to advance for tearing.

> Right format in short: a **PNG**, sized to the paper width (e.g. 384 px wide for 58 mm),
> pure black-and-white, printed with `type = 0`, and **no trailing `cutPaper()`** if you want
> it tight.

## Scanning

Devices with a scanner expose two modes via `PosioScanner`:

**Camera scanner** — opens the capture screen, returns the result to `onActivityResult`:

```java
// start scanning
PosioScanner.launchCameraScanner(activity, REQUEST_SCAN);

// in onActivityResult(requestCode, resultCode, data):
String code = PosioScanner.parseScanResult(data);
```

**Infrared (laser) scanner** — receive scans from the hardware trigger:

```java
BroadcastReceiver r = PosioScanner.registerInfraredScan(context, code -> {
    // handle scanned code
});
// later: PosioScanner.unregisterInfraredScan(context, r);

// trigger by software instead of the side button:
printer.triggerInfraredScan(true);   // false to stop
```

## Sample app

The [`sample`](sample) module is a complete, runnable example. Open the project in Android Studio,
select the `sample` run configuration, and deploy it to a Posio device.

## Web / HTTP printing & scanning

If your software is web-based (browser or cloud) and can't run native Android code, the Posio
Printer Service exposes a local **HTTP API** on port `8080` — no SDK needed. Install the
**Posio Printer Service** app ([`apks/PosioPrinterService.apk`](apks)), open it once to see the
device URL, and call it over the network.

**Printing** — POST receipt JSON:

```bash
curl -X POST http://<device-ip>:8080/print/receipt \
  -H "Content-Type: application/json" \
  -d '{"jobs":[{"type":"text","text":"Hello","align":"center"},{"type":"cut"}]}'
```

**Scanning** — read the device's hardware barcode/QR scanner over the same URL (no on-screen
camera). Long-poll the next scan, or subscribe to a live stream:

```bash
# wait for the next scan (and fire the scan engine for the user)
curl "http://<device-ip>:8080/scan?trigger=true&timeout=30000"
# -> {"timedOut":false,"code":"012345678905"}
# The engine is armed for this one read and stopped automatically when the call returns,
# so the scanner does not keep scanning afterwards.
```

```js
// live stream in a web page (Server-Sent Events)
const es = new EventSource("http://<device-ip>:8080/scan/stream");
es.onmessage = e => console.log("scanned:", JSON.parse(e.data).code);
```

Full request/response reference, JavaScript examples, and result codes are in
**[HOW_TO_USE.md](HOW_TO_USE.md)**.

## License

[MIT](LICENSE)
