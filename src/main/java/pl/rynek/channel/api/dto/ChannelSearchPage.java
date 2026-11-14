package pl.rynek.channel.api.dto;

import java.util.List;

/** Strona kanałów po id rosnąco; {@code nextAfterId} jest pusty na ostatniej stronie. */
public record ChannelSearchPage(List<ChannelSearchItem> items, String nextAfterId) {
}
