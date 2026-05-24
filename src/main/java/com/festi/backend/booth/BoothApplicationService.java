package com.festi.backend.booth;

import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalRepository;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import com.festi.backend.user.UserRole;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoothApplicationService {

    private final BoothApplicationRepository boothApplicationRepository;
    private final UserRepository userRepository;
    private final BoothRepository boothRepository;
    private final FestivalRepository festivalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public BoothApplicationDTO.Response createApplication(BoothApplicationDTO.CreateRequest request) {
        if (request.boothType() == BoothType.FOOD_TRUCK) {
            throw new BadRequestException("Food truck booths must be registered by a festival admin.");
        }

        Festival festival = detectFestival();

        if (userRepository.existsByIdAndFestivalId(request.id(), festival.getId())) {
            throw new ConflictException("ID is already in use.");
        }

        User manager = new User(
                festival,
                request.id(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone()
        );
        manager.changeRole(UserRole.BOOTH_MANAGER);
        userRepository.save(manager);

        BoothCategory category = request.boothCategory() == null
                ? BoothCategory.ACTIVITY
                : request.boothCategory();
        BoothApplication application = new BoothApplication(
                festival,
                request.id(),
                request.boothName(),
                request.boothType(),
                category,
                request.imageUrl(),
                request.description()
        );
        return BoothApplicationDTO.Response.from(boothApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public BoothApplicationDTO.Response getMyApplication(String userId, UUID festivalId) {
        return BoothApplicationDTO.Response.from(
                boothApplicationRepository.findFirstByFestivalIdAndApplicantIdOrderByCreatedAtDesc(festivalId, userId)
                        .orElseThrow(() -> new NotFoundException("Booth application not found."))
        );
    }

    @Transactional(readOnly = true)
    public List<BoothApplicationDTO.Response> getApplications(UUID festivalId) {
        return boothApplicationRepository.findByFestivalIdOrderByCreatedAtDesc(festivalId).stream()
                .map(BoothApplicationDTO.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoothApplicationDTO.Response getApplication(UUID festivalId, UUID applicationId) {
        return BoothApplicationDTO.Response.from(findApplication(festivalId, applicationId));
    }

    @Transactional
    public BoothApplicationDTO.Response approveApplication(UUID festivalId, UUID applicationId) {
        BoothApplication application = findApplication(festivalId, applicationId);
        assertPending(application);

        User manager = userRepository.findByIdAndFestivalId(application.getApplicantId(), festivalId)
                .orElseThrow(() -> new NotFoundException("Booth manager not found."));
        Booth booth = new Booth(application.getBoothName(), application.getBoothCategory(), application.getBoothType());
        booth.update(
                application.getBoothName(),
                application.getBoothCategory(),
                application.getDescription(),
                null,
                application.getImageUrl()
        );
        booth.assignManager(manager);
        Booth savedBooth = boothRepository.save(booth);

        application.approve(savedBooth);
        return BoothApplicationDTO.Response.from(application);
    }

    @Transactional
    public BoothApplicationDTO.Response rejectApplication(UUID festivalId, UUID applicationId, String reviewMemo) {
        BoothApplication application = findApplication(festivalId, applicationId);
        assertPending(application);

        application.reject(normalize(reviewMemo));
        return BoothApplicationDTO.Response.from(application);
    }

    @Transactional
    public void deleteApplication(UUID festivalId, UUID applicationId) {
        BoothApplication application = findApplication(festivalId, applicationId);
        if (application.getStatus() == BoothApplicationStatus.APPROVED) {
            throw new ConflictException("Approved booth applications cannot be deleted.");
        }

        boothApplicationRepository.delete(application);
        userRepository.findByIdAndFestivalId(application.getApplicantId(), festivalId)
                .ifPresent(userRepository::delete);
    }

    private BoothApplication findApplication(UUID festivalId, UUID applicationId) {
        return boothApplicationRepository.findByIdAndFestivalId(applicationId, festivalId)
                .orElseThrow(() -> new NotFoundException("Booth application not found."));
    }

    private void assertPending(BoothApplication application) {
        if (application.getStatus() != BoothApplicationStatus.PENDING) {
            throw new ConflictException("Booth application has already been reviewed.");
        }
    }

    private Festival detectFestival() {
        List<Festival> festivals = festivalRepository.findAll();
        if (festivals.isEmpty()) {
            throw new NotFoundException("Festival not found.");
        }
        return festivals.getFirst();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
