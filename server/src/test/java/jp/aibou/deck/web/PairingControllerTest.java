package jp.aibou.deck.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "app.deck.executor=mock",
        "app.deck.data-dir=${java.io.tmpdir}/deck-pairing-test-${random.uuid}"
})
class PairingControllerTest {

    @Autowired
    private PairingController controller;

    private MockHttpServletRequest local() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    @Test
    void pairingReturnsClientUrlsWithCode() {
        Map<String, Object> result = controller.pairing(local(), false);
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) result.get("urls");
        assertTrue(urls.size() >= 1);
        for (String url : urls) {
            assertTrue(url.contains("/deck/?pair="), url);
        }
        assertEquals(300, result.get("expiresInSeconds"));
    }

    @Test
    void qrIsAPng() {
        controller.pairing(local(), false);
        byte[] png = controller.qr(local()).getBody();
        // PNG magic number.
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);
    }

    @Test
    void rejectsNonLocalRequests() {
        MockHttpServletRequest remote = new MockHttpServletRequest();
        remote.setRemoteAddr("192.168.1.50");
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.pairing(remote, false));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
}
