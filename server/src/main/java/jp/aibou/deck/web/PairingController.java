package jp.aibou.deck.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import jp.aibou.deck.auth.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pairing support for the /pair page (localhost-only, like the editor):
 * issuing a one-time pairing code builds the client URL per reachable
 * address, and the QR image encodes the primary one.
 */
@RestController
public class PairingController {

    private final AuthService authService;
    private final int port;
    private volatile String primaryUrl;

    public PairingController(AuthService authService, @Value("${server.port:8081}") int port) {
        this.authService = authService;
        this.port = port;
    }

    @GetMapping("/api/pairing")
    public Map<String, Object> pairing(HttpServletRequest request,
                                       @org.springframework.web.bind.annotation.RequestParam(
                                               defaultValue = "false") boolean force) {
        EditorController.requireLocal(request);
        String code = authService.createPairingCode(force);
        List<String> urls = new ArrayList<>();
        for (String host : lanHosts()) {
            urls.add("http://" + host + ":" + port + "/deck/?pair=" + code);
        }
        urls.add("http://localhost:" + port + "/deck/?pair=" + code);
        primaryUrl = urls.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiresInSeconds", 300);
        result.put("urls", urls);
        result.put("primaryUrl", primaryUrl);
        return result;
    }

    @GetMapping(value = "/api/pairing/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(HttpServletRequest request) {
        EditorController.requireLocal(request);
        String url = primaryUrl;
        if (url == null) {
            pairing(request, false); // make sure a code exists, then encode its URL
            url = primaryUrl;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrPng(url, 360));
    }

    static byte[] qrPng(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new UncheckedIOException(new IOException("QR generation failed", e));
        }
    }

    /** Non-loopback IPv4 addresses of up interfaces, site-local first. */
    static List<String> lanHosts() {
        List<String> hosts = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        hosts.add(address.getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            // No LAN address found; the caller adds localhost as a fallback.
        }
        hosts.sort((a, b) -> Boolean.compare(!isSiteLocal(a), !isSiteLocal(b)));
        return hosts;
    }

    private static boolean isSiteLocal(String host) {
        return host.startsWith("192.168.") || host.startsWith("10.")
                || host.matches("172\\.(1[6-9]|2\\d|3[01])\\..*");
    }
}
