package com.icthh.xm.commons.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.util.Objects;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Utility class for handling pagination.
 *
 * <p> Pagination uses the same principles as the <a href="https://developer.github.com/v3/#pagination">Github API</a>,
 * and follow <a href="http://tools.ietf.org/html/rfc5988">RFC 5988 (Link header)</a>.
 */
@UtilityClass
public final class PaginationUtil {

    private static final String QUERY_GET_PARAM = "&query=";
    private static final String TYPEKEY_GET_PARAM = "&typeKey=";
    private static final String IDS_GET_PARAM = "&ids=";
    private static final String EMBED_GET_PARAM = "&embed=";

    public static HttpHeaders generatePaginationHttpHeaders(Page page, String baseUrl) {
        return generatePagination(EMPTY, page, baseUrl);
    }

    @SneakyThrows
    public static HttpHeaders generateByIdsPaginationHttpHeaders(Set<Long> ids, Set<String> embed, Page page, String baseUrl) {
        String escapedIds = URLEncoder.encode(Objects.toString(StringUtils.join(ids, ","), EMPTY), UTF_8);
        String escapedEmbed = URLEncoder.encode(Objects.toString(StringUtils.join(embed, ","), EMPTY), UTF_8);

        String queryString = IDS_GET_PARAM + escapedIds + EMBED_GET_PARAM + escapedEmbed;

        return generatePagination(queryString, page, baseUrl);
    }

    private static String generateUri(String baseUrl, int page, int size) {
        return UriComponentsBuilder.fromUriString(baseUrl).queryParam("page", page).queryParam("size", size)
            .toUriString();
    }

    @SneakyThrows
    public static HttpHeaders generateSearchPaginationHttpHeaders(String query, Page page, String baseUrl) {
        String escapedQuery = URLEncoder.encode(Objects.toString(query, EMPTY), UTF_8);

        String queryString = QUERY_GET_PARAM + escapedQuery;

        return generatePagination(queryString, page, baseUrl);
    }

    @SneakyThrows
    public static HttpHeaders generateSearchByTypeKeyPaginationHttpHeaders(String typeKey, String query, Page page, String baseUrl) {
        String escapedTypeKey = URLEncoder.encode(Objects.toString(typeKey, EMPTY), UTF_8);
        String escapedQuery = URLEncoder.encode(Objects.toString(query, EMPTY), UTF_8);

        String queryString = TYPEKEY_GET_PARAM + escapedTypeKey
            + QUERY_GET_PARAM + escapedQuery;

        return generatePagination(queryString, page, baseUrl);
    }

    private static HttpHeaders generatePagination(String query, Page page, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        String link = "";
        if ((page.getNumber() + 1) < page.getTotalPages()) {
            link = "<" + generateUri(baseUrl, page.getNumber() + 1, page.getSize()) + query
                + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getNumber()) > 0) {
            link += "<" + generateUri(baseUrl, page.getNumber() - 1, page.getSize()) + query
                + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getTotalPages() > 0) {
            lastPage = page.getTotalPages() - 1;
        }
        link +=
            "<" + generateUri(baseUrl, lastPage, page.getSize()) + query + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, page.getSize()) + query + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }
}
