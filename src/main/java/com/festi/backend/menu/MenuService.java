package com.festi.backend.menu;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.security.BoothAuthorizationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuService {

    private final BoothRepository boothRepository;
    private final MenuItemRepository menuItemRepository;
    private final BoothAuthorizationService boothAuthorizationService;

    public List<MenuDTO.Response> getMenus(UUID boothId) {
        boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        return menuItemRepository.findByBoothIdOrderBySortOrder(boothId).stream()
                .map(MenuDTO.Response::from)
                .toList();
    }

    @Transactional
    public MenuDTO.Response createMenu(AuthenticatedUser currentUser, UUID boothId,
                                       MenuDTO.Request request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        assertNightBooth(booth);
        MenuItem menuItem = menuItemRepository.save(
                new MenuItem(booth, request.name(), request.price(),
                        request.description(), request.imageUrl(), request.sortOrder()));
        return MenuDTO.Response.from(menuItem);
    }

    @Transactional
    public MenuDTO.Response updateMenu(AuthenticatedUser currentUser, UUID boothId, UUID menuId,
                                       MenuDTO.Request request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        assertNightBooth(booth);
        MenuItem menuItem = menuItemRepository.findByIdAndBoothId(menuId, boothId)
                .orElseThrow(() -> new NotFoundException("Menu not found."));
        menuItem.update(request.name(), request.price(), request.description(),
                request.imageUrl(), request.sortOrder());
        return MenuDTO.Response.from(menuItem);
    }

    @Transactional
    public void deleteMenu(AuthenticatedUser currentUser, UUID boothId, UUID menuId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        MenuItem menuItem = menuItemRepository.findByIdAndBoothId(menuId, boothId)
                .orElseThrow(() -> new NotFoundException("Menu not found."));
        menuItemRepository.delete(menuItem);
    }

    @Transactional
    public MenuDTO.Response markSoldOut(AuthenticatedUser currentUser, UUID boothId, UUID menuId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        MenuItem menuItem = menuItemRepository.findByIdAndBoothId(menuId, boothId)
                .orElseThrow(() -> new NotFoundException("Menu not found."));
        menuItem.markSoldOut();
        return MenuDTO.Response.from(menuItem);
    }

    private void assertNightBooth(Booth booth) {
        if (booth.getType() != BoothType.NIGHT) {
            throw new BadRequestException("Menu management is only allowed for NIGHT booths.");
        }
    }
}
