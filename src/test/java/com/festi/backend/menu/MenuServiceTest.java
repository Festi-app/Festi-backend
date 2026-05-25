package com.festi.backend.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.security.BoothAuthorizationService;
import com.festi.backend.user.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private BoothAuthorizationService boothAuthorizationService;

    private MenuService menuService;

    private AuthenticatedUser manager;
    private AuthenticatedUser festivalAdmin;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(boothRepository, menuItemRepository, boothAuthorizationService);
        manager = new AuthenticatedUser("manageruser", UUID.randomUUID(), UserRole.BOOTH_MANAGER);
        festivalAdmin = new AuthenticatedUser("adminuser", UUID.randomUUID(), UserRole.FESTIVAL_ADMIN);
    }

    // ── getMenus ─────────────────────────────────────────────────────────────

    @Test
    void returnsMenusInRepositoryOrder() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);
        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.findByBoothIdOrderBySortOrder(boothId))
                .thenReturn(List.of(menu(booth, "tteokbokki", (short) 1), menu(booth, "ramen", (short) 2)));

        List<MenuDTO.Response> response = menuService.getMenus(boothId);

        assertThat(response).extracting(MenuDTO.Response::name).containsExactly("tteokbokki", "ramen");
    }

    @Test
    void rejectsMenusForMissingBooth() {
        UUID boothId = UUID.randomUUID();
        when(boothRepository.findById(boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.getMenus(boothId))
                .isInstanceOf(NotFoundException.class);
    }

    // ── createMenu ───────────────────────────────────────────────────────────

    @Test
    void createMenuSucceedsForNightBooth() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);
        MenuDTO.Request request = new MenuDTO.Request("메뉴A", 5000, "설명", null, (short) 1);
        MenuItem saved = menu(booth, request.name(), request.sortOrder());
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(saved);

        MenuDTO.Response response = menuService.createMenu(manager, boothId, request);

        assertThat(response.name()).isEqualTo("메뉴A");
        verify(menuItemRepository).save(any(MenuItem.class));
    }

    @Test
    void createMenuThrowsBadRequestForDayBooth() {
        UUID boothId = UUID.randomUUID();
        Booth booth = dayBooth(boothId);
        MenuDTO.Request request = new MenuDTO.Request("메뉴B", 3000, null, null, (short) 1);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));

        assertThatThrownBy(() -> menuService.createMenu(manager, boothId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("NIGHT and FOOD_TRUCK booths");
    }

    @Test
    void createMenuThrowsNotFoundForMissingBooth() {
        UUID boothId = UUID.randomUUID();
        MenuDTO.Request request = new MenuDTO.Request("메뉴C", 1000, null, null, (short) 1);

        when(boothRepository.findById(boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.createMenu(manager, boothId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Booth not found");
    }

    @Test
    void createMenuThrowsAccessDeniedWhenNotAuthorized() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);
        MenuDTO.Request request = new MenuDTO.Request("메뉴D", 2000, null, null, (short) 1);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        doThrow(new AccessDeniedException("Access is denied."))
                .when(boothAuthorizationService).assertCanManageBooth(manager, booth);

        assertThatThrownBy(() -> menuService.createMenu(manager, boothId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── updateMenu ───────────────────────────────────────────────────────────

    @Test
    void updateMenuSucceedsForNightBooth() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);
        MenuItem existing = menu(booth, "기존메뉴", (short) 1);
        ReflectionTestUtils.setField(existing, "id", menuId);
        MenuDTO.Request request = new MenuDTO.Request("수정메뉴", 8000, "새설명", null, (short) 2);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.findByIdAndBoothId(menuId, boothId)).thenReturn(Optional.of(existing));

        MenuDTO.Response response = menuService.updateMenu(manager, boothId, menuId, request);

        assertThat(response.name()).isEqualTo("수정메뉴");
        assertThat(response.price()).isEqualTo(8000);
    }

    @Test
    void updateMenuThrowsBadRequestForDayBooth() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        Booth booth = dayBooth(boothId);
        MenuDTO.Request request = new MenuDTO.Request("수정", 1000, null, null, (short) 1);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));

        assertThatThrownBy(() -> menuService.updateMenu(manager, boothId, menuId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("NIGHT and FOOD_TRUCK booths");
    }

    @Test
    void updateMenuThrowsNotFoundForMissingMenu() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);
        MenuDTO.Request request = new MenuDTO.Request("수정", 1000, null, null, (short) 1);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.findByIdAndBoothId(menuId, boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.updateMenu(manager, boothId, menuId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Menu not found");
    }

    // ── deleteMenu ───────────────────────────────────────────────────────────

    @Test
    void deleteMenuSucceeds() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);
        MenuItem existing = menu(booth, "삭제메뉴", (short) 1);
        ReflectionTestUtils.setField(existing, "id", menuId);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.findByIdAndBoothId(menuId, boothId)).thenReturn(Optional.of(existing));

        menuService.deleteMenu(manager, boothId, menuId);

        verify(menuItemRepository).delete(existing);
    }

    @Test
    void deleteMenuThrowsNotFoundForMissingBooth() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();

        when(boothRepository.findById(boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.deleteMenu(manager, boothId, menuId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteMenuThrowsNotFoundForMissingMenu() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.findByIdAndBoothId(menuId, boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.deleteMenu(manager, boothId, menuId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Menu not found");
    }

    // ── markSoldOut ──────────────────────────────────────────────────────────

    @Test
    void markSoldOutSucceeds() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);
        MenuItem existing = menu(booth, "품절메뉴", (short) 1);
        ReflectionTestUtils.setField(existing, "id", menuId);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.findByIdAndBoothId(menuId, boothId)).thenReturn(Optional.of(existing));

        MenuDTO.Response response = menuService.markSoldOut(manager, boothId, menuId);

        assertThat(response.isSoldOut()).isTrue();
    }

    @Test
    void markSoldOutThrowsNotFoundForMissingBooth() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();

        when(boothRepository.findById(boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.markSoldOut(manager, boothId, menuId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void markSoldOutThrowsNotFoundForMissingMenu() {
        UUID boothId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        Booth booth = nightBooth(boothId);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(menuItemRepository.findByIdAndBoothId(menuId, boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.markSoldOut(manager, boothId, menuId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Menu not found");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Booth nightBooth(UUID id) {
        Booth booth = new Booth("night booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private Booth dayBooth(UUID id) {
        Booth booth = new Booth("day booth", BoothCategory.ACTIVITY, BoothType.DAY);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private MenuItem menu(Booth booth, String name, short sortOrder) {
        return new MenuItem(booth, name, 5000, "desc", "image", sortOrder);
    }
}
