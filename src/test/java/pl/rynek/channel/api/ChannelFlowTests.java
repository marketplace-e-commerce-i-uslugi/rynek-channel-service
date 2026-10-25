package pl.rynek.channel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.channel.TestcontainersConfig;
import pl.rynek.channel.identity.IdentityClient;

/**
 * Kanały i obserwowanie na prawdziwym PostgreSQL. Tokeny w testach to {@code user-<uuid>} (dekoder zastępuje JWKS
 * identity), a wywołania identity są zastąpione atrapą.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ChannelFlowTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    IdentityClient identity;

    @BeforeEach
    void tokensAreUserIds() {
        given(jwtDecoder.decode(anyString())).willAnswer(call -> {
            String token = call.getArgument(0);
            if (!token.startsWith("user-")) {
                throw new BadJwtException("Nieważny token");
            }
            return Jwt.withTokenValue(token).header("alg", "RS256").subject(token.substring(5))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build();
        });
    }

    @Test
    void sellerCreatesChannelAndAnyoneCanReadIt() throws Exception {
        UUID owner = UUID.randomUUID();
        String handle = uniqueHandle();
        MvcResult result = create(owner, "{\"name\":\" Elektro Market \",\"handle\":\"@" + handle.toUpperCase() + "\",\"description\":\"Elektronika\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.handle").value(handle))
                .andExpect(jsonPath("$.name").value("Elektro Market"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.followersCount").value(0))
                .andExpect(jsonPath("$.rating.count").value(0))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/api/v1/channels/" + id);
        verify(identity).linkChannel(owner, UUID.fromString(id), handle);

        mvc.perform(get("/api/v1/channels/by-handle/@" + handle.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Elektronika"))
                .andExpect(jsonPath("$.following").value(nullValue()));
        // Wygasły lub zły token na publicznym endpoincie: gość, nie 401.
        mvc.perform(get("/api/v1/channels/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer wygasly"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/channels/" + id + "/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value(nullValue()))
                .andExpect(jsonPath("$.joinedAt").isNotEmpty());
    }

    @Test
    void handleMustBeValidAndUnique() throws Exception {
        String handle = uniqueHandle();
        create(UUID.randomUUID(), channel("Zła nazwa", "ab")).andExpect(status().isBadRequest());
        create(UUID.randomUUID(), channel("Zła nazwa", "sklep-łódź")).andExpect(status().isBadRequest());
        create(UUID.randomUUID(), "{\"name\":\"\",\"handle\":\"" + handle + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Podaj nazwę kanału."));

        create(UUID.randomUUID(), channel("Pierwszy", handle)).andExpect(status().isCreated());
        create(UUID.randomUUID(), channel("Drugi", handle.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Ten adres kanału jest już zajęty. Wybierz inny."));
    }

    @Test
    void oneChannelPerAccount() throws Exception {
        UUID owner = UUID.randomUUID();
        create(owner, channel("Pierwszy", uniqueHandle())).andExpect(status().isCreated());
        create(owner, channel("Drugi", uniqueHandle()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Masz już kanał. Jedno konto może mieć jeden kanał."));
    }

    @Test
    void identityRefusalRollsBackChannel() throws Exception {
        UUID buyer = UUID.randomUUID();
        willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Kanał może założyć usługodawca albo merchant."))
                .given(identity).linkChannel(eq(buyer), any(), anyString());
        String handle = uniqueHandle();

        create(buyer, channel("Kupujący", handle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Kanał może założyć usługodawca albo merchant."));
        mvc.perform(get("/api/v1/channels/by-handle/" + handle)).andExpect(status().isNotFound());
    }

    @Test
    void ownerEditsChannelAndBusinessDetails() throws Exception {
        UUID owner = UUID.randomUUID();
        String id = createdId(owner);

        mvc.perform(patch("/api/v1/channels/" + id).header(HttpHeaders.AUTHORIZATION, bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nowa nazwa\",\"description\":\"Opis\",\"phone\":\"+48 22 100 20 30\",\"email\":\"kontakt@example.pl\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nowa nazwa"));
        mvc.perform(patch("/api/v1/channels/" + id).header(HttpHeaders.AUTHORIZATION, bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/channels/" + id).header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Przejęty\"}"))
                .andExpect(status().isForbidden());

        businessDetails(owner, id, "{\"companyName\":\"Elektro Market Sp. z o.o.\",\"nip\":\"525-000-00-01\",\"registeredAddress\":\"ul. Handlowa 10, Warszawa\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ten NIP jest nieprawidłowy. Sprawdź, czy nie ma literówki."));
        businessDetails(owner, id, "{\"companyName\":\"Elektro Market Sp. z o.o.\",\"nip\":\"525-000-00\",\"registeredAddress\":\"ul. Handlowa 10, Warszawa\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("NIP musi mieć 10 cyfr."));
        businessDetails(owner, id, "{\"companyName\":\"Elektro Market Sp. z o.o.\",\"nip\":\"526-104-08-28\",\"registeredAddress\":\"ul. Handlowa 10, Warszawa\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nip").value("5261040828"));

        mvc.perform(get("/api/v1/channels/" + id + "/about"))
                .andExpect(jsonPath("$.description").value("Opis"))
                .andExpect(jsonPath("$.phone").value(nullValue()))
                .andExpect(jsonPath("$.email").value("kontakt@example.pl"))
                .andExpect(jsonPath("$.company.companyName").value("Elektro Market Sp. z o.o."));
    }

    @Test
    void verificationStatusIsVisibleOnlyToOwner() throws Exception {
        UUID owner = UUID.randomUUID();
        String id = createdId(owner);
        mvc.perform(get("/api/v1/channels/" + id + "/verification").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"));
        mvc.perform(get("/api/v1/channels/" + id + "/verification").header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/channels/" + id + "/verification")).andExpect(status().isUnauthorized());
    }

    @Test
    void followUnfollowAndBell() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID fan = UUID.randomUUID();
        String id = createdId(owner);
        String follow = "/api/v1/channels/" + id + "/follow";

        mvc.perform(post(follow).header(HttpHeaders.AUTHORIZATION, bearer(fan))).andExpect(status().isNoContent());
        mvc.perform(post(follow).header(HttpHeaders.AUTHORIZATION, bearer(fan))).andExpect(status().isNoContent());
        mvc.perform(patch(follow).header(HttpHeaders.AUTHORIZATION, bearer(fan)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationsOn\":true}"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/channels/" + id).header(HttpHeaders.AUTHORIZATION, bearer(fan)))
                .andExpect(jsonPath("$.followersCount").value(1))
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.notificationsOn").value(true));
        mvc.perform(get("/api/v1/users/me/following").header(HttpHeaders.AUTHORIZATION, bearer(fan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].notificationsOn").value(true));

        mvc.perform(delete(follow).header(HttpHeaders.AUTHORIZATION, bearer(fan))).andExpect(status().isNoContent());
        mvc.perform(delete(follow).header(HttpHeaders.AUTHORIZATION, bearer(fan))).andExpect(status().isNoContent());
        mvc.perform(patch(follow).header(HttpHeaders.AUTHORIZATION, bearer(fan)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationsOn\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/channels/" + id).header(HttpHeaders.AUTHORIZATION, bearer(fan)))
                .andExpect(jsonPath("$.followersCount").value(0))
                .andExpect(jsonPath("$.following").value(false));

        mvc.perform(post(follow).header(HttpHeaders.AUTHORIZATION, bearer(owner))).andExpect(status().isConflict());
        mvc.perform(post(follow)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/channels/" + UUID.randomUUID() + "/follow").header(HttpHeaders.AUTHORIZATION, bearer(fan)))
                .andExpect(status().isNotFound());
    }

    @Test
    void internalBatchKeepsRequestedOrderAndSkipsUnknown() throws Exception {
        String first = createdId(UUID.randomUUID());
        String second = createdId(UUID.randomUUID());
        mvc.perform(get("/internal/channels").param("ids", second + "," + UUID.randomUUID() + ",nie-uuid," + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(second, first)));
        mvc.perform(get("/internal/channels/" + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void mediaDependentEndpointsStillReturn501() throws Exception {
        UUID owner = UUID.randomUUID();
        String id = createdId(owner);
        mvc.perform(put("/api/v1/channels/" + id + "/avatar").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotImplemented());
    }

    private String createdId(UUID owner) throws Exception {
        String body = create(owner, channel("Kanał testowy", uniqueHandle())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions create(UUID owner, String json) throws Exception {
        return mvc.perform(post("/api/v1/channels").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions businessDetails(UUID owner, String id, String json) throws Exception {
        return mvc.perform(put("/api/v1/channels/" + id + "/business-details").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String channel(String name, String handle) {
        return "{\"name\":\"" + name + "\",\"handle\":\"" + handle + "\"}";
    }

    private static String bearer(UUID userId) {
        return "Bearer user-" + userId;
    }

    private static String uniqueHandle() {
        return "k" + UUID.randomUUID().toString().substring(0, 8);
    }
}
