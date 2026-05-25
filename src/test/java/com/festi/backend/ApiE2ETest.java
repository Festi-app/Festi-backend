package com.festi.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.favorite.FavoriteDTO;
import com.festi.backend.favorite.FavoriteRepository;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalRepository;
import com.festi.backend.menu.MenuDTO;
import com.festi.backend.menu.MenuItemRepository;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import com.festi.backend.user.UserRole;
import com.festi.backend.waiting.WaitingDTO;
import com.festi.backend.waiting.WaitingRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("e2e")
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private FestivalRepository festivalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BoothRepository boothRepository;
    @Autowired private WaitingRepository waitingRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private MenuItemRepository menuItemRepository;

    private Festival festival;
    private UUID festivalId;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        festival = festivalRepository.save(
                new Festival("Festi 2026", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc"));
        festivalId = festival.getId();
    }

    @AfterEach
    void tearDown() {
        // FK 제약 순서: waitings/favorites/menu_items → booths → users → festival
        waitingRepository.deleteAll();
        favoriteRepository.deleteAll();
        menuItemRepository.deleteAll();
        boothRepository.deleteAll();
        userRepository.deleteAll();
        festivalRepository.deleteAll();
    }

    // ── Scenario 1: Waiting Flow ─────────────────────────────────────────────

    @Test
    void waitingFlow() {
        // setup: 일반 유저 + 웨이팅 오픈된 야간 부스
        User user = new User(festival, "user01", passwordEncoder.encode("pw"), "유저원", "010-1111-1111");
        userRepository.save(user);

        Booth booth = new Booth("야간부스", BoothCategory.ALCOHOL, BoothType.NIGHT);
        booth.openWaiting();
        boothRepository.save(booth);

        String token = token("user01", festivalId, UserRole.USER);

        // 1. GET /api/booths — 부스가 목록에 있음
        webTestClient.get().uri("/api/booths")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        // 2. POST /api/booths/{boothId}/waitings — 웨이팅 등록
        WaitingDTO.Response waiting = webTestClient.post()
                .uri("/api/booths/" + booth.getId() + "/waitings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new WaitingDTO.Request((short) 2))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WaitingDTO.Response.class)
                .returnResult().getResponseBody();
        assertThat(waiting.partySize()).isEqualTo((short) 2);
        UUID waitingId = waiting.id();

        // 3. GET /api/waitings — 내 웨이팅 목록에 등록됨
        List<WaitingDTO.Response> myWaitings = webTestClient.get().uri("/api/waitings")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(WaitingDTO.Response.class)
                .returnResult().getResponseBody();
        assertThat(myWaitings).hasSize(1);

        // 4. DELETE /api/waitings/{waitingId} — 웨이팅 취소
        webTestClient.delete().uri("/api/waitings/" + waitingId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }

    // ── Scenario 2: Menu Management Flow ────────────────────────────────────

    @Test
    void menuManagementFlow() {
        // setup: 부스 매니저 유저 + 해당 매니저가 배정된 야간 부스
        User manager = new User(festival, "manager01", passwordEncoder.encode("pw"), "매니저원", "010-2222-2222");
        manager.changeRole(UserRole.BOOTH_MANAGER);
        userRepository.save(manager);

        Booth booth = new Booth("야간부스", BoothCategory.ALCOHOL, BoothType.NIGHT);
        booth.assignManager(manager);
        boothRepository.save(booth);

        String token = token("manager01", festivalId, UserRole.BOOTH_MANAGER);

        // 1. POST /api/booths/{boothId}/menus — 메뉴 생성
        MenuDTO.Response created = webTestClient.post()
                .uri("/api/booths/" + booth.getId() + "/menus")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MenuDTO.Request("떡볶이", 4000, "매운맛", (short) 1))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MenuDTO.Response.class)
                .returnResult().getResponseBody();
        assertThat(created.name()).isEqualTo("떡볶이");
        UUID menuId = created.id();

        // 2. PATCH /api/booths/{boothId}/menus/{menuId} — 메뉴 수정
        MenuDTO.Response updated = webTestClient.patch()
                .uri("/api/booths/" + booth.getId() + "/menus/" + menuId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MenuDTO.Request("라볶이", 5000, "순한맛", (short) 1))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuDTO.Response.class)
                .returnResult().getResponseBody();
        assertThat(updated.name()).isEqualTo("라볶이");

        // 3. POST /api/booths/{boothId}/menus/{menuId}/sold-out — 품절 처리
        MenuDTO.Response soldOut = webTestClient.post()
                .uri("/api/booths/" + booth.getId() + "/menus/" + menuId + "/sold-out")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuDTO.Response.class)
                .returnResult().getResponseBody();
        assertThat(soldOut.isSoldOut()).isTrue();

        // 4. DELETE /api/booths/{boothId}/menus/{menuId} — 메뉴 삭제
        webTestClient.delete()
                .uri("/api/booths/" + booth.getId() + "/menus/" + menuId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }

    // ── Scenario 3: Favorite Flow ────────────────────────────────────────────

    @Test
    void favoriteFlow() {
        // setup: DB에 favorites_user_fk 제약이 있어 User 엔티티도 필요
        User user = new User(festival, "user02", passwordEncoder.encode("pw"), "유저투", "010-3333-3333");
        userRepository.save(user);

        Booth booth = boothRepository.save(new Booth("야간부스", BoothCategory.ALCOHOL, BoothType.NIGHT));

        String token = token("user02", festivalId, UserRole.USER);

        // 1. POST /api/favorites — 즐겨찾기 추가
        FavoriteDTO.Response added = webTestClient.post().uri("/api/favorites")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FavoriteDTO.Request(booth.getId()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FavoriteDTO.Response.class)
                .returnResult().getResponseBody();
        UUID favoriteId = added.id();

        // 2. GET /api/favorites — 즐겨찾기 목록에 있음
        List<FavoriteDTO.Response> favorites = webTestClient.get().uri("/api/favorites")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FavoriteDTO.Response.class)
                .returnResult().getResponseBody();
        assertThat(favorites).hasSize(1);
        assertThat(favorites.get(0).id()).isEqualTo(favoriteId);

        // 3. DELETE /api/favorites/{favoriteId} — 즐겨찾기 삭제
        webTestClient.delete().uri("/api/favorites/" + favoriteId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String token(String userId, UUID festivalId, UserRole role) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("festivalId", festivalId.toString())
                .claim("role", role.name())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
