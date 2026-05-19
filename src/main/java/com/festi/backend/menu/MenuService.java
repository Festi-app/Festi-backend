package com.festi.backend.menu;

import com.festi.backend.booth.BoothRepository;
import com.festi.backend.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuService {

    private final BoothRepository boothRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuService(BoothRepository boothRepository, MenuItemRepository menuItemRepository) {
        this.boothRepository = boothRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public List<MenuDTO.Response> getMenus(UUID boothId) {
        boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        return menuItemRepository.findByBoothIdOrderBySortOrder(boothId).stream()
                .map(MenuDTO.Response::from)
                .toList();
    }
}
