package com.parkingSystem.service.impl;

import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.repository.ParkingSpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotAllocationServiceImplTest {

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @InjectMocks
    private SlotAllocationServiceImpl slotAllocationService;

    @Test
    void slotAvailabilityCheck() {
        when(parkingSpaceRepository.findByParkingSpaceId(1L))
                .thenReturn(Optional.of(space(1L, "A-1", 0, "Available")));
        when(parkingSpaceRepository.findByParkingSpaceId(2L))
                .thenReturn(Optional.of(space(2L, "A-2", 0, "occupied")));
        when(parkingSpaceRepository.findByParkingSpaceId(3L)).thenReturn(Optional.empty());
        ParkingSpace removed = space(4L, "A-4", 0, "available");
        removed.setIsActive(false);
        when(parkingSpaceRepository.findByParkingSpaceId(4L)).thenReturn(Optional.of(removed));
        when(parkingSpaceRepository.findByParkingSpaceId(5L))
                .thenReturn(Optional.of(space(5L, "A-5", 0, "reserved")));

        assertThat(slotAllocationService.isSlotAvailable(1L)).isTrue();
        assertThat(slotAllocationService.isSlotAvailable(2L)).isFalse();
        assertThat(slotAllocationService.isSlotAvailable(3L)).isFalse();
        assertThat(slotAllocationService.isSlotAvailable(4L)).as("removed space").isFalse();
        assertThat(slotAllocationService.isSlotAvailable(5L)).as("reserved space").isFalse();
    }

    @Test
    void slotsByPriorityAreSortedByFloorThenSpaceNumber() {
        when(parkingSpaceRepository.findNearestAvailableSlots("North", SlotType.NORMAL)).thenReturn(List.of(
                space(1L, "B-2", 1, "available"),
                space(2L, "A-9", 0, "available"),
                space(3L, "B-1", 1, "available"),
                space(4L, "A-1", null, "available")));

        List<ParkingSpace> result = slotAllocationService.getSlotsByPriority(SlotType.NORMAL, "North");

        assertThat(result).extracting(ParkingSpace::getSpaceNumber).containsExactly("A-1", "A-9", "B-1", "B-2");
    }

    @Test
    void availabilitySummaryCountsByType() {
        when(parkingSpaceRepository.countAllAvailable()).thenReturn(6L);
        when(parkingSpaceRepository.countAvailableBySlotType(SlotType.NORMAL)).thenReturn(3L);
        when(parkingSpaceRepository.countAvailableBySlotType(SlotType.VIP)).thenReturn(2L);
        when(parkingSpaceRepository.countAvailableBySlotType(SlotType.HANDICAPPED)).thenReturn(1L);

        SlotAllocationServiceImpl.AvailabilitySummary summary = slotAllocationService.getAvailabilitySummary();

        assertThat(summary.getTotalAvailableSlots()).isEqualTo(6L);
        assertThat(summary.getNormalSlotsAvailable()).isEqualTo(3L);
        assertThat(summary.getVipSlotsAvailable()).isEqualTo(2L);
        assertThat(summary.getHandicappedSlotsAvailable()).isEqualTo(1L);
    }

    private static ParkingSpace space(Long id, String number, Integer floor, String status) {
        return ParkingSpace.builder()
                .parkingSpaceId(id)
                .spaceNumber(number)
                .floorNumber(floor)
                .location("North")
                .slotType(SlotType.NORMAL)
                .ratePerHour(BigDecimal.TEN)
                .availabilityStatus(SpaceStatus.fromValue(status))
                .isActive(true)
                .build();
    }
}
