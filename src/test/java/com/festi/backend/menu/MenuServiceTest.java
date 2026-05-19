package com.festi.backend.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(boothRepository, menuItemRepository);
    }

    @Test
    void returnsMenusInRepositoryOrder() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId);
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

    private Booth booth(UUID id) {
        Booth booth = new Booth("booth", BoothCategory.ALCOHOL, BoothType.NIGHT, "creator");
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private MenuItem menu(Booth booth, String name, short sortOrder) {
        return new MenuItem(booth, name, 5000, "desc", "image", sortOrder);
    }
}
