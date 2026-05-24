package com.festi.backend.menu;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByBoothIdOrderBySortOrder(UUID boothId);

    Optional<MenuItem> findByIdAndBoothId(UUID id, UUID boothId);
}
