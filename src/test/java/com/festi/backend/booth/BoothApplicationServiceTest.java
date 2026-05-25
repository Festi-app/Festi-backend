package com.festi.backend.booth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalRepository;
import com.festi.backend.user.User;
import com.festi.backend.user.UserPK;
import com.festi.backend.user.UserRepository;
import com.festi.backend.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoothApplicationServiceTest {

    @Mock
    private BoothApplicationRepository boothApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private BoothApplicationService boothApplicationService;

    private Festival festival;

    @BeforeEach
    void setUp() {
        boothApplicationService = new BoothApplicationService(
                boothApplicationRepository,
                userRepository,
                boothRepository,
                festivalRepository,
                passwordEncoder
        );
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
    }

    @Test
    void createsBoothManagerAccountAndPendingApplication() {
        BoothApplicationDTO.CreateRequest request = createRequest(null);
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(userRepository.existsByIdAndFestivalId("manager1", festival.getId())).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(boothApplicationRepository.save(any(BoothApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoothApplicationDTO.Response response = boothApplicationService.createApplication(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<BoothApplication> applicationCaptor = ArgumentCaptor.forClass(BoothApplication.class);
        verify(userRepository).save(userCaptor.capture());
        verify(boothApplicationRepository).save(applicationCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.BOOTH_MANAGER);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(applicationCaptor.getValue().getStatus()).isEqualTo(BoothApplicationStatus.PENDING);
        assertThat(applicationCaptor.getValue().getBoothCategory()).isEqualTo(BoothCategory.ACTIVITY);
        assertThat(response.status()).isEqualTo(BoothApplicationStatus.PENDING);
        assertThat(response.boothId()).isNull();
    }

    @Test
    void rejectsDuplicateApplicantId() {
        BoothApplicationDTO.CreateRequest request = createRequest(BoothCategory.ALCOHOL);
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(userRepository.existsByIdAndFestivalId("manager1", festival.getId())).thenReturn(true);

        assertThatThrownBy(() -> boothApplicationService.createApplication(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void readsApplicationsWithinFestivalScope() {
        UUID applicationId = UUID.randomUUID();
        BoothApplication application = application(applicationId, "manager1");
        when(boothApplicationRepository.findFirstByFestivalIdAndApplicantIdOrderByCreatedAtDesc(
                festival.getId(), "manager1"))
                .thenReturn(Optional.of(application));
        when(boothApplicationRepository.findByFestivalIdOrderByCreatedAtDesc(festival.getId()))
                .thenReturn(List.of(application));
        when(boothApplicationRepository.findByIdAndFestivalId(applicationId, festival.getId()))
                .thenReturn(Optional.of(application));

        assertThat(boothApplicationService.getMyApplication("manager1", festival.getId()).id()).isEqualTo(applicationId);
        assertThat(boothApplicationService.getApplications(festival.getId())).hasSize(1);
        assertThat(boothApplicationService.getApplication(festival.getId(), applicationId).id()).isEqualTo(applicationId);
    }

    @Test
    void rejectsMissingApplicationReads() {
        UUID applicationId = UUID.randomUUID();
        when(boothApplicationRepository.findFirstByFestivalIdAndApplicantIdOrderByCreatedAtDesc(
                festival.getId(), "manager1"))
                .thenReturn(Optional.empty());
        when(boothApplicationRepository.findByIdAndFestivalId(applicationId, festival.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> boothApplicationService.getMyApplication("manager1", festival.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> boothApplicationService.getApplication(festival.getId(), applicationId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void approvesPendingApplicationAndCreatesManagedBooth() {
        UUID applicationId = UUID.randomUUID();
        UUID boothId = UUID.randomUUID();
        BoothApplication application = application(applicationId, "manager1");
        User manager = user("manager1");
        when(boothApplicationRepository.findByIdAndFestivalId(applicationId, festival.getId()))
                .thenReturn(Optional.of(application));
        when(userRepository.findByIdAndFestivalId("manager1", festival.getId()))
                .thenReturn(Optional.of(manager));
        when(boothRepository.save(any(Booth.class))).thenAnswer(invocation -> {
            Booth booth = invocation.getArgument(0);
            ReflectionTestUtils.setField(booth, "id", boothId);
            return booth;
        });

        BoothApplicationDTO.Response response = boothApplicationService.approveApplication(festival.getId(), applicationId);

        ArgumentCaptor<Booth> boothCaptor = ArgumentCaptor.forClass(Booth.class);
        verify(boothRepository).save(boothCaptor.capture());
        assertThat(response.status()).isEqualTo(BoothApplicationStatus.APPROVED);
        assertThat(boothCaptor.getValue().getManager()).isEqualTo(manager);
        assertThat(boothCaptor.getValue().getName()).isEqualTo("Night Booth");
        assertThat(boothCaptor.getValue().getType()).isEqualTo(BoothType.NIGHT);
        assertThat(response.boothId()).isEqualTo(boothId);
        assertThat(application.getBooth()).isSameAs(boothCaptor.getValue());
    }

    @Test
    void rejectsRepeatedReviewTransitions() {
        UUID applicationId = UUID.randomUUID();
        BoothApplication approved = application(applicationId, "manager1");
        approved.approve(new Booth("Night Booth", BoothCategory.ALCOHOL, BoothType.NIGHT));
        when(boothApplicationRepository.findByIdAndFestivalId(applicationId, festival.getId()))
                .thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> boothApplicationService.approveApplication(festival.getId(), applicationId))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> boothApplicationService.rejectApplication(festival.getId(), applicationId, "memo"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsAndNormalizesBlankMemoToNull() {
        UUID applicationId = UUID.randomUUID();
        BoothApplication application = application(applicationId, "manager1");
        when(boothApplicationRepository.findByIdAndFestivalId(applicationId, festival.getId()))
                .thenReturn(Optional.of(application));

        BoothApplicationDTO.Response response = boothApplicationService.rejectApplication(festival.getId(), applicationId, " ");

        assertThat(response.status()).isEqualTo(BoothApplicationStatus.REJECTED);
        assertThat(response.reviewMemo()).isNull();
    }

    @Test
    void deletesPendingOrRejectedApplicationsAndCreatedManagerAccount() {
        UUID applicationId = UUID.randomUUID();
        BoothApplication application = application(applicationId, "manager1");
        User manager = user("manager1");
        when(boothApplicationRepository.findByIdAndFestivalId(applicationId, festival.getId()))
                .thenReturn(Optional.of(application));
        when(userRepository.findByIdAndFestivalId("manager1", festival.getId()))
                .thenReturn(Optional.of(manager));

        boothApplicationService.deleteApplication(festival.getId(), applicationId);

        verify(boothApplicationRepository).delete(application);
        verify(userRepository).delete(manager);
    }

    @Test
    void rejectsDeletingApprovedApplications() {
        UUID applicationId = UUID.randomUUID();
        BoothApplication application = application(applicationId, "manager1");
        application.approve(new Booth("Night Booth", BoothCategory.ALCOHOL, BoothType.NIGHT));
        when(boothApplicationRepository.findByIdAndFestivalId(applicationId, festival.getId()))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> boothApplicationService.deleteApplication(festival.getId(), applicationId))
                .isInstanceOf(ConflictException.class);
    }

    private BoothApplicationDTO.CreateRequest createRequest(BoothCategory boothCategory) {
        return new BoothApplicationDTO.CreateRequest(
                "manager1",
                "Password1!",
                "Manager",
                "01012345678",
                "Night Booth",
                BoothType.NIGHT,
                boothCategory,
                "https://example.com/booth.png",
                "description"
        );
    }

    private BoothApplication application(UUID id, String applicantId) {
        BoothApplication application = new BoothApplication(
                festival,
                applicantId,
                "Night Booth",
                BoothType.NIGHT,
                BoothCategory.ALCOHOL,
                "https://example.com/booth.png",
                "description"
        );
        ReflectionTestUtils.setField(application, "id", id);
        return application;
    }

    private User user(String id) {
        User user = new User(festival, id, "hashed-password", "Manager", "01012345678");
        user.changeRole(UserRole.BOOTH_MANAGER);
        ReflectionTestUtils.setField(user, "pk", new UserPK(festival.getId(), id));
        return user;
    }
}
