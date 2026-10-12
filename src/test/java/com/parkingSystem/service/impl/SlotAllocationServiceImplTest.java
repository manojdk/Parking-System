package com.parkingSystem.service.impl;

import com.parkingSystem.dto.AvailabilitySummaryDTO;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.enums.VehicleType;
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
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void availabilitySummaryListsEverySlotTypeWithZeroWhereNoneAreFree() {
        when(parkingSpaceRepository.countAvailableGroupedBySlotType()).thenReturn(List.of(
                new Object[] { SlotType.NORMAL, 3L },
                new Object[] { SlotType.EV_CHARGING, 2L }));

        AvailabilitySummaryDTO summary = slotAllocationService.getAvailabilitySummary();

        assertThat(summary.totalAvailable()).isEqualTo(5L);
        assertThat(summary.availableBySlotType()).containsExactly(
                entry(SlotType.NORMAL, 3L), entry(SlotType.VIP, 0L), entry(SlotType.HANDICAPPED, 0L),
                entry(SlotType.EV_CHARGING, 2L), entry(SlotType.BIKE, 0L));
    }

    @Test
    void evIsOfferedAChargingBayFirstThenANormalOne() {
        ParkingSpace charger = space(7L, "E-1", 0, "available");
        charger.setSlotType(SlotType.EV_CHARGING);
        ParkingSpace normal = space(8L, "N-1", 0, "available");
        when(parkingSpaceRepository.findNearestAvailableSlot("North", SlotType.EV_CHARGING))
                .thenReturn(Optional.of(charger));

        assertThat(slotAllocationService.recommendSlot("North", VehicleType.EV)).contains(charger);

        when(parkingSpaceRepository.findNearestAvailableSlot("North", SlotType.EV_CHARGING)).thenReturn(Optional.empty());
        when(parkingSpaceRepository.findNearestAvailableSlot("North", SlotType.NORMAL)).thenReturn(Optional.of(normal));

        assertThat(slotAllocationService.recommendSlot("North", VehicleType.EV)).contains(normal);
    }

    @Test
    void bikeIsOnlyOfferedABikeBay() {
        when(parkingSpaceRepository.findNearestAvailableSlot("North", SlotType.BIKE)).thenReturn(Optional.empty());

        assertThat(slotAllocationService.recommendSlot("North", VehicleType.BIKE)).isEmpty();
        verify(parkingSpaceRepository, never()).findNearestAvailableSlot("North", SlotType.NORMAL);
    }

    @Test
    void slotTypesAcceptOnlyVehiclesThatFit() {
        assertThat(SlotType.NORMAL.accepts(VehicleType.CAR)).isTrue();
        assertThat(SlotType.NORMAL.accepts(VehicleType.EV)).isTrue();
        assertThat(SlotType.NORMAL.accepts(VehicleType.BIKE)).isFalse();
        assertThat(SlotType.EV_CHARGING.accepts(VehicleType.EV)).isTrue();
        assertThat(SlotType.EV_CHARGING.accepts(VehicleType.CAR)).isFalse();
        assertThat(SlotType.BIKE.accepts(VehicleType.BIKE)).isTrue();
        assertThat(SlotType.BIKE.accepts(VehicleType.CAR)).isFalse();
        assertThat(SlotType.VIP.accepts(VehicleType.CAR)).isTrue();
        assertThat(SlotType.HANDICAPPED.accepts(null)).isFalse();
        for (VehicleType vehicle : VehicleType.values()) {
            assertThat(vehicle.recommendedSlotTypes()).allMatch(slot -> slot.accepts(vehicle));
        }
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
