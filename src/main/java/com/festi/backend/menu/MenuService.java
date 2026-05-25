package com.festi.backend.menu;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.image.ImageFileTransactionManager;
import com.festi.backend.image.ImageStorage;
import com.festi.backend.image.ImageStorage.ImageDirectory;
import com.festi.backend.image.ImageStorage.StoredImage;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.security.BoothAuthorizationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuService {

    private final BoothRepository boothRepository;
    private final MenuItemRepository menuItemRepository;
    private final BoothAuthorizationService boothAuthorizationService;
    private final ImageStorage imageStorage;
    private final ImageFileTransactionManager imageFileTransactionManager;

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
        assertMenuManageableBooth(booth);
        MenuItem menuItem = menuItemRepository.save(
                new MenuItem(booth, request.name(), request.price(),
                        request.description(), request.sortOrder()));
        return MenuDTO.Response.from(menuItem);
    }

    @Transactional
    public MenuDTO.Response updateMenu(AuthenticatedUser currentUser, UUID boothId, UUID menuId,
                                       MenuDTO.Request request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        assertMenuManageableBooth(booth);
        MenuItem menuItem = menuItemRepository.findByIdAndBoothId(menuId, boothId)
                .orElseThrow(() -> new NotFoundException("Menu not found."));
        menuItem.update(request.name(), request.price(), request.description(), request.sortOrder());
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
        imageFileTransactionManager.deleteAfterCommit(menuItem.getImageUrl());
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

    @Transactional
    public MenuDTO.Response updateImage(AuthenticatedUser currentUser, UUID boothId, UUID menuId,
                                        MultipartFile image) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        assertMenuManageableBooth(booth);
        MenuItem menuItem = menuItemRepository.findByIdAndBoothId(menuId, boothId)
                .orElseThrow(() -> new NotFoundException("Menu not found."));
        String previousUrl = menuItem.getImageUrl();
        StoredImage storedImage = imageStorage.store(image, ImageDirectory.MENUS);
        imageFileTransactionManager.replaceAfterTransaction(previousUrl, storedImage.publicUrl());
        menuItem.updateImage(storedImage.publicUrl());
        return MenuDTO.Response.from(menuItem);
    }

    @Transactional
    public void removeImage(AuthenticatedUser currentUser, UUID boothId, UUID menuId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        assertMenuManageableBooth(booth);
        MenuItem menuItem = menuItemRepository.findByIdAndBoothId(menuId, boothId)
                .orElseThrow(() -> new NotFoundException("Menu not found."));
        String previousUrl = menuItem.getImageUrl();
        menuItem.removeImage();
        imageFileTransactionManager.deleteAfterCommit(previousUrl);
    }

    private void assertMenuManageableBooth(Booth booth) {
        if (booth.getType() != BoothType.NIGHT && booth.getType() != BoothType.FOOD_TRUCK) {
            throw new BadRequestException("Menu management is only allowed for NIGHT and FOOD_TRUCK booths.");
        }
    }
}
