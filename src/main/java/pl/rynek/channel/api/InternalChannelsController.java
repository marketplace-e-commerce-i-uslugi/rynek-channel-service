package pl.rynek.channel.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static pl.rynek.channel.common.NotImplemented.notImplemented;

@RestController
public class InternalChannelsController {

    /**
     * Dane kanału dla innych serwisów. Dostęp: int.
     */
    @GetMapping("/internal/channels/{channelId}")
    public void getChannel(@PathVariable String channelId) {
        throw notImplemented();
    }

    /**
     * Wsadowo (karty ofert). Dostęp: int.
     */
    @GetMapping("/internal/channels")
    public void getChannelsBatch(@RequestParam(required = false) String ids) {
        throw notImplemented();
    }
}
