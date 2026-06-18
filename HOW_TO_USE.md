# Posio Printer — Network Printing & Scanning Guide

Print receipts **and read barcode/QR scans** on a Posio device from your own software over
the local network. No SDK, no native code — just standard HTTP requests. Works from a
browser, a backend, or any language that can make an HTTP call.

---

## 1. Set up the printer (one time, on the device)

1. Install the **Posio Printer Service** app on the Posio device.
2. Open the app once. It shows a screen with the printer's network address, for example:

   ```
   Receipt printing endpoint is RUNNING.
   Base URL: http://192.168.10.115:8080
   ```

3. That's it. The service keeps running in the background and starts again automatically
   after the device reboots — you don't need to keep the app open.

**Note the Base URL** (the `http://<ip>:8080` shown on screen). Your software will send
print requests to it. The port is always **8080**.

> Tip: ask your network admin to give the Posio device a **fixed (reserved) IP address** on
> the router, so the address never changes.

---

## 2. Check the connection

From any computer or phone on the same network:

```bash
curl http://192.168.10.115:8080/status
```

A healthy response looks like:

```json
{"connected": true, "printerVersion": "4.21", "printerStatus": 0}
```

- `connected: true` → the printer is ready.
- `printerStatus: 0` → no errors (out of paper, cover open, etc. report a non-zero value).

---

## 3. Print a quick line of text

```bash
curl -X POST http://192.168.10.115:8080/print/text \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello from our POS!","align":"center","bold":true}'
```

Response on success:

```json
{"code": 0, "message": "OK"}
```

`code: 0` always means **printed successfully**. Any other value is an error (see §6).

---

## 4. Print a full receipt

Send a list of "jobs" to `POST /print/receipt`. Each job is one element on the receipt
(a line of text, a barcode, a QR code, an image, a paper feed, or the cut).

```bash
curl -X POST http://192.168.10.115:8080/print/receipt \
  -H "Content-Type: application/json" \
  -d '{
    "jobs": [
      {"type":"text","text":"MY STORE","align":"center","bold":true,"size":36},
      {"type":"text","text":"123 Market Street","align":"center"},
      {"type":"text","text":"Tel: 555-1234","align":"center"},
      {"type":"feed","lines":1},
      {"type":"text","text":"Receipt #1001"},
      {"type":"text","text":"2026-06-12 18:45"},
      {"type":"text","text":"--------------------------------"},
      {"type":"text","text":"Coffee                    3.50"},
      {"type":"text","text":"Muffin                    2.00"},
      {"type":"text","text":"--------------------------------"},
      {"type":"text","text":"TOTAL                     5.50","bold":true,"size":30},
      {"type":"feed","lines":1},
      {"type":"qrcode","content":"https://posio.example/r/1001","size":300,"align":"center"},
      {"type":"text","text":"Thank you!","align":"center"},
      {"type":"cut"}
    ]
  }'
```

The jobs print in order. If you don't include a `{"type":"cut"}` job at the end, the paper
is fed out automatically.

---

## 4b. Aligning columns (monospace)

For receipts where prices/quantities must line up, use a **monospace** font so every
character is the same width. Set `"font": 4` on a `text` job (or on `/print/text`):

```json
{ "jobs": [
  {"type":"text","text":"Coffee                3.50","font":4},
  {"type":"text","text":"Muffin                2.00","font":4},
  {"type":"text","text":"TOTAL                 5.50","font":4,"bold":true}
]}
```

`font` values: `0` DEFAULT · `1` DEFAULT_BOLD · `2` SANS_SERIF · `3` SERIF ·
**`4` MONOSPACE**. Default is `0`. (Value `5` CUSTOM needs a `.ttf` file path, which can't be
supplied over HTTP — use it from the Android SDK instead. Unknown values fall back to `0`.)

> Requires the **Posio Printer Service** that ships with this SDK (`apks/PosioPrinterService.apk`)
> or newer. On an older service install the `font` field is ignored (text still prints in the
> default font). The two fallbacks below need no service update:

1. **Pre-aligned text** — pad columns with spaces on your side so they line up, and send
   plain `text` jobs. Keep lines to ~32 characters on 58 mm paper (~48 on 80 mm) at the
   default size.
2. **Raw ESC/POS** — send an `escpos` job (or `POST /print/escpos`) with Base64-encoded
   ESC/POS bytes that select the printer's built-in fixed-width font, followed by your text.
   Use this when you specifically want a hardware monospace font.

---

## 5. Call it from your web app (JavaScript)

The printer accepts cross-origin requests, so a browser app can call it directly:

```js
async function printReceipt(printerBaseUrl, receipt) {
  const res = await fetch(`${printerBaseUrl}/print/receipt`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(receipt),
  });
  const result = await res.json();
  if (result.code !== 0) {
    throw new Error("Print failed: " + result.message);
  }
}

await printReceipt("http://192.168.10.115:8080", {
  jobs: [
    { type: "text", text: "ORDER #42", align: "center", bold: true, size: 32 },
    { type: "text", text: "1x Latte           4.00" },
    { type: "text", text: "TOTAL              4.00", bold: true },
    { type: "cut" },
  ],
});
```

---

## Receipt job reference

Put these objects inside the `"jobs"` array. `align` is `left` (default), `center`, or `right`.

| `type` | Fields | Description |
|--------|--------|-------------|
| `text` | `text`, `size` (px, default 24), `align`, `bold`, `underline`, `font` (0–5, default 0; **4 = MONOSPACE**) | Print a line of text |
| `feed` | `lines` (or `px`) | Advance the paper |
| `barcode` | `content`, `width`, `height`, `textPosition`, `align`, `symbology` | Print a 1D barcode |
| `qrcode` | `content`, `size`, `align` | Print a QR code |
| `image` | `base64` (PNG/JPEG, Base64), `align` | Print a logo or image |
| `escpos` | `base64` (raw ESC/POS bytes, Base64) | Send raw ESC/POS commands |
| `cut` | — | Feed and cut the paper |

### Barcode types (`symbology`)
`0` CODE128 · `1` CODE39 · `2` CODE93 · `3` UPC-A · `4` UPC-E · `5` EAN13 · `6` EAN8 · `7` ITF · `8` CODABAR

### Barcode text position (`textPosition`)
`0` none · `1` above · `2` below · `3` both

---

## All endpoints

| Method & path | Body | Purpose |
|---------------|------|---------|
| `GET /status` | — | Check the printer connection and status |
| `POST /print/text` | plain text, or `{text,size,align,bold,underline,font}` | Print one line of text |
| `POST /print/receipt` | `{"jobs":[ ... ]}` | Print a full receipt (recommended) |
| `POST /print/image` | `{"base64":"...","align":"center"}` | Print an image / logo |
| `POST /print/escpos` | `{"base64":"..."}` | Send raw ESC/POS commands |
| `GET /scan` | query `?timeout=ms&trigger=true` | Wait for the next hardware scan |
| `GET /scan/stream` | — | Live stream of scans (Server-Sent Events) |
| `GET /scan/last` | — | The most recent scan |
| `GET /scan/status` | — | Scanner connection and listener count |
| `POST /scan/trigger` | `{"open":true\|false}` | Start/stop the scan engine by software |

Every response is JSON: `{"code": 0, "message": "OK"}` on success.

---

## Reading barcode / QR scans

The same service also exposes the device's **built-in hardware scanner** (the infrared/laser
scan engine triggered by the physical scan button) over the same Base URL and port — no
on-screen camera. Your web app reads scans with plain HTTP, exactly like printing.

### Option A — wait for the next scan (long-poll)

```bash
# Waits up to 30s; pass trigger=true to also fire the scan engine for the user.
curl "http://192.168.10.115:8080/scan?trigger=true&timeout=30000"
```

```json
{"timedOut": false, "code": "012345678905"}
```

If nothing is scanned before the timeout you get `{"timedOut": true}` — just call again
(this is the simplest way to scan continuously from a web page):

```js
async function scanLoop(baseUrl, onCode) {
  while (true) {
    const res = await fetch(`${baseUrl}/scan?trigger=true&timeout=30000`);
    const r = await res.json();
    if (!r.timedOut) onCode(r.code);
  }
}
scanLoop("http://192.168.10.115:8080", code => console.log("scanned:", code));
```

### Option B — live stream (Server-Sent Events)

Subscribe once and receive every scan as it happens:

```js
const es = new EventSource("http://192.168.10.115:8080/scan/stream");
es.onmessage = (e) => {
  const msg = JSON.parse(e.data);      // {"type":"scan","code":"012345678905"}
  console.log("scanned:", msg.code);
};
```

### Trigger the engine yourself

```bash
curl -X POST http://192.168.10.115:8080/scan/trigger -d '{"open":true}'   # start
curl -X POST http://192.168.10.115:8080/scan/trigger -d '{"open":false}'  # stop
```

> Scans from the device's **physical scan button** are delivered to `/scan` and
> `/scan/stream` automatically — triggering by software is optional.

---

## 6. Result codes

`code: 0` means success. Common non-zero values:

| Code | Meaning |
|------|---------|
| `-1100` | Printer not ready / not connected — check the device |
| `-1201` | Printer cover is open |
| `-1203` | Out of paper |
| `-1204` | Printer overheated |
| `-1206` | Printer busy |

If you get `-1100`, open the **Posio Printer Service** app on the device and confirm the
status screen shows it is running, then retry.

---

## 7. Troubleshooting

- **No response / connection refused** → the device and your software must be on the **same
  network**. Confirm the IP from the app's status screen and that port **8080** isn't blocked.
- **The address changed** → DHCP gave the device a new IP. Reserve a static IP for it.
- **Prints but looks wrong** → adjust `size`, `align`, and use a `--------` separator line; keep
  line widths to ~32 characters for 58 mm paper.
- **Need a logo at the top** → send an `image` job with your logo as Base64 (monochrome prints best).
- **Extra blank space around an image** → the printer prints every row/column of the bitmap,
  so any white/transparent margin baked into the source image (a logo on a tall canvas, a QR
  with a wide quiet-zone, a padded signature) comes out as blank paper. **Crop the image to
  its content before Base64-encoding it** — the service prints exactly what you send. (The
  Android sample does this automatically; see `trimBlankMargins(...)` in `PrinterFragment.java`.)

---

For questions or help integrating, contact your Posio representative.
